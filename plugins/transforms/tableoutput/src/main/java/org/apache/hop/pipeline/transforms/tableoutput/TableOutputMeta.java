/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.pipeline.transforms.tableoutput;

import com.google.common.base.Preconditions;
import org.apache.hop.core.CheckResult;
import org.apache.hop.core.Const;
import org.apache.hop.core.ICheckResult;
import org.apache.hop.core.IProvidesModelerMeta;
import org.apache.hop.core.SqlStatement;
import org.apache.hop.core.annotations.Transform;
import org.apache.hop.core.database.Database;
import org.apache.hop.core.database.DatabaseMeta;
import org.apache.hop.core.exception.HopDatabaseException;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.exception.HopXmlException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.value.ValueMetaInteger;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.xml.XmlHandler;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.pipeline.DatabaseImpact;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.ITransform;
import org.apache.hop.pipeline.transform.ITransformData;
import org.apache.hop.pipeline.transform.ITransformMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Transform(
    id = "TableOutput",
    image = "tableoutput.svg",
    name = "i18n::BaseTransform.TypeLongDesc.TableOutput",
    description = "i18n::BaseTransform.TypeTooltipDesc.TableOutput",
    categoryDescription = "i18n:org.apache.hop.pipeline.transform:BaseTransform.Category.Output",
    documentationUrl = "https://hop.apache.org/manual/latest/pipeline/transforms/tableoutput.html")
public class TableOutputMeta extends BaseTransformMeta
    implements ITransformMeta<TableOutput, TableOutputData>, IProvidesModelerMeta {
  private static final Class<?> PKG = TableOutputMeta.class; // For Translator

  private DatabaseMeta databaseMeta;
  private String schemaName;
  private String tableName;
  private String commitSize;
  private boolean truncateTable;
  private boolean ignoreErrors;
  private boolean useBatchUpdate;

  private boolean partitioningEnabled;
  private String partitioningField;
  private boolean partitioningDaily;
  private boolean partitioningMonthly;

  private boolean tableNameInField;
  private String tableNameField;
  private boolean tableNameInTable;

  private boolean returningGeneratedKeys;
  private String generatedKeyField;

  /** Do we explicitly select the fields to update in the database */
  private boolean specifyFields;

  /** Fields containing the values in the input stream to insert */
  private String[] fieldStream;

  /** Fields in the table to insert */
  private String[] fieldDatabase;

  /** @return Returns the generatedKeyField. */
  public String getGeneratedKeyField() {
    return generatedKeyField;
  }

  /** @param generatedKeyField The generatedKeyField to set. */
  public void setGeneratedKeyField(String generatedKeyField) {
    this.generatedKeyField = generatedKeyField;
  }

  /** @return Returns the returningGeneratedKeys. */
  public boolean isReturningGeneratedKeys() {
    if (getDatabaseMeta() != null) {
      return getDatabaseMeta().supportsAutoGeneratedKeys() && returningGeneratedKeys;
    }
    return false;
  }

  /** @param returningGeneratedKeys The returningGeneratedKeys to set. */
  public void setReturningGeneratedKeys(boolean returningGeneratedKeys) {
    this.returningGeneratedKeys = returningGeneratedKeys;
  }

  /** @return Returns the tableNameInTable. */
  public boolean isTableNameInTable() {
    return tableNameInTable;
  }

  /** @param tableNameInTable The tableNameInTable to set. */
  public void setTableNameInTable(boolean tableNameInTable) {
    this.tableNameInTable = tableNameInTable;
  }

  /** @return Returns the tableNameField. */
  public String getTableNameField() {
    return tableNameField;
  }

  /** @param tableNameField The tableNameField to set. */
  public void setTableNameField(String tableNameField) {
    this.tableNameField = tableNameField;
  }

  /** @return Returns the tableNameInField. */
  public boolean isTableNameInField() {
    return tableNameInField;
  }

  /** @param tableNameInField The tableNameInField to set. */
  public void setTableNameInField(boolean tableNameInField) {
    this.tableNameInField = tableNameInField;
  }

  /** @return Returns the partitioningDaily. */
  public boolean isPartitioningDaily() {
    return partitioningDaily;
  }

  /** @param partitioningDaily The partitioningDaily to set. */
  public void setPartitioningDaily(boolean partitioningDaily) {
    this.partitioningDaily = partitioningDaily;
  }

  /** @return Returns the partitioningMontly. */
  public boolean isPartitioningMonthly() {
    return partitioningMonthly;
  }

  /** @param partitioningMontly The partitioningMontly to set. */
  public void setPartitioningMonthly(boolean partitioningMontly) {
    this.partitioningMonthly = partitioningMontly;
  }

  /** @return Returns the partitioningEnabled. */
  public boolean isPartitioningEnabled() {
    return partitioningEnabled;
  }

  /** @param partitioningEnabled The partitioningEnabled to set. */
  public void setPartitioningEnabled(boolean partitioningEnabled) {
    this.partitioningEnabled = partitioningEnabled;
  }

  /** @return Returns the partitioningField. */
  public String getPartitioningField() {
    return partitioningField;
  }

  /** @param partitioningField The partitioningField to set. */
  public void setPartitioningField(String partitioningField) {
    this.partitioningField = partitioningField;
  }

  public TableOutputMeta() {
    super(); // allocate BaseTransformMeta
    useBatchUpdate = true;
    commitSize = "1000";

    fieldStream = new String[0];
    fieldDatabase = new String[0];
  }

  public void allocate(int nrRows) {
    fieldStream = new String[nrRows];
    fieldDatabase = new String[nrRows];
  }

  public void loadXml(Node transformNode, IHopMetadataProvider metadataProvider)
      throws HopXmlException {
    readData(transformNode, metadataProvider);
  }

  public Object clone() {
    Preconditions.checkState(
        fieldStream.length == fieldDatabase.length,
        "Table fields and stream fields are not of equal length.");
    TableOutputMeta retval = (TableOutputMeta) super.clone();
    int nrRows = fieldStream.length;
    retval.allocate(nrRows);
    System.arraycopy(fieldStream, 0, retval.fieldStream, 0, nrRows);
    System.arraycopy(fieldDatabase, 0, retval.fieldDatabase, 0, nrRows);

    return retval;
  }

  /** @return Returns the database. */
  public DatabaseMeta getDatabaseMeta() {
    return databaseMeta;
  }

  /** @param database The database to set. */
  public void setDatabaseMeta(DatabaseMeta database) {
    this.databaseMeta = database;
  }

  /** @return Returns the commitSize. */
  public String getCommitSize() {
    return commitSize;
  }

  /** @param commitSizeInt The commitSize to set. */
  public void setCommitSize(int commitSizeInt) {
    this.commitSize = Integer.toString(commitSizeInt);
  }

  /** @param commitSize The commitSize to set. */
  public void setCommitSize(String commitSize) {
    this.commitSize = commitSize;
  }

  /** @return the table name */
  public String getTableName() {
    return tableName;
  }

  /**
   * Assign the table name to write to.
   *
   * @param tableName The table name to set
   */
  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  /**
   * @return Returns the tablename.
   * @deprecated Use {@link #getTableName()}
   */
  @Deprecated
  public String getTablename() {
    return getTableName();
  }

  /**
   * @param tableName The tablename to set.
   * @deprecated Use {@link #setTableName(String)}
   */
  @Deprecated
  public void setTablename(String tableName) {
    setTableName(tableName);
  }

  /** @return Returns the truncate table flag. */
  public boolean truncateTable() {
    return truncateTable;
  }

  /** @param truncateTable The truncate table flag to set. */
  public void setTruncateTable(boolean truncateTable) {
    this.truncateTable = truncateTable;
  }

  /** @param ignoreErrors The ignore errors flag to set. */
  public void setIgnoreErrors(boolean ignoreErrors) {
    this.ignoreErrors = ignoreErrors;
  }

  /** @return Returns the ignore errors flag. */
  public boolean ignoreErrors() {
    return ignoreErrors;
  }

  /** @param specifyFields The specify fields flag to set. */
  public void setSpecifyFields(boolean specifyFields) {
    this.specifyFields = specifyFields;
  }

  /** @return Returns the specify fields flag. */
  public boolean specifyFields() {
    return specifyFields;
  }

  /** @param useBatchUpdate The useBatchUpdate flag to set. */
  public void setUseBatchUpdate(boolean useBatchUpdate) {
    this.useBatchUpdate = useBatchUpdate;
  }

  /** @return Returns the useBatchUpdate flag. */
  public boolean useBatchUpdate() {
    return useBatchUpdate;
  }

  private void readData(Node transformNode, IHopMetadataProvider metadataProvider)
      throws HopXmlException {
    try {
      String con = XmlHandler.getTagValue(transformNode, "connection");
      databaseMeta = DatabaseMeta.loadDatabase(metadataProvider, con);
      schemaName = XmlHandler.getTagValue(transformNode, "schema");
      tableName = XmlHandler.getTagValue(transformNode, "table");
      commitSize = XmlHandler.getTagValue(transformNode, "commit");
      truncateTable = "Y".equalsIgnoreCase(XmlHandler.getTagValue(transformNode, "truncate"));
      ignoreErrors = "Y".equalsIgnoreCase(XmlHandler.getTagValue(transformNode, "ignore_errors"));
      useBatchUpdate = "Y".equalsIgnoreCase(XmlHandler.getTagValue(transformNode, "use_batch"));

      // If not present it will be false to be compatible with pre-v3.2
      specifyFields = "Y".equalsIgnoreCase(XmlHandler.getTagValue(transformNode, "specify_fields"));

      partitioningEnabled =
          "Y".equalsIgnoreCase(XmlHandler.getTagValue(transformNode, "partitioning_enabled"));
      partitioningField = XmlHandler.getTagValue(transformNode, "partitioning_field");
      partitioningDaily =
          "Y".equalsIgnoreCase(XmlHandler.getTagValue(transformNode, "partitioning_daily"));
      partitioningMonthly =
          "Y".equalsIgnoreCase(XmlHandler.getTagValue(transformNode, "partitioning_monthly"));

      tableNameInField =
          "Y".equalsIgnoreCase(XmlHandler.getTagValue(transformNode, "tablename_in_field"));
      tableNameField = XmlHandler.getTagValue(transformNode, "tablename_field");
      tableNameInTable =
          "Y".equalsIgnoreCase(XmlHandler.getTagValue(transformNode, "tablename_in_table"));

      returningGeneratedKeys =
          "Y".equalsIgnoreCase(XmlHandler.getTagValue(transformNode, "return_keys"));
      generatedKeyField = XmlHandler.getTagValue(transformNode, "return_field");

      Node fields = XmlHandler.getSubNode(transformNode, "fields");
      int nrRows = XmlHandler.countNodes(fields, "field");

      allocate(nrRows);

      for (int i = 0; i < nrRows; i++) {
        Node knode = XmlHandler.getSubNodeByNr(fields, "field", i);

        fieldDatabase[i] = XmlHandler.getTagValue(knode, "column_name");
        fieldStream[i] = XmlHandler.getTagValue(knode, "stream_name");
      }
    } catch (Exception e) {
      throw new HopXmlException("Unable to load transform info from XML", e);
    }
  }

  public void setDefault() {
    databaseMeta = null;
    tableName = "";
    commitSize = "1000";

    partitioningEnabled = false;
    partitioningMonthly = true;
    partitioningField = "";
    tableNameInTable = true;
    tableNameField = "";

    // To be compatible with pre-v3.2 (SB)
    specifyFields = false;
  }

  public String getXml() {
    StringBuilder retval = new StringBuilder();

    retval.append(
        "    "
            + XmlHandler.addTagValue(
                "connection", databaseMeta == null ? "" : databaseMeta.getName()));
    retval.append("    " + XmlHandler.addTagValue("schema", schemaName));
    retval.append("    " + XmlHandler.addTagValue("table", tableName));
    retval.append("    " + XmlHandler.addTagValue("commit", commitSize));
    retval.append("    " + XmlHandler.addTagValue("truncate", truncateTable));
    retval.append("    " + XmlHandler.addTagValue("ignore_errors", ignoreErrors));
    retval.append("    " + XmlHandler.addTagValue("use_batch", useBatchUpdate));
    retval.append("    " + XmlHandler.addTagValue("specify_fields", specifyFields));

    retval.append("    " + XmlHandler.addTagValue("partitioning_enabled", partitioningEnabled));
    retval.append("    " + XmlHandler.addTagValue("partitioning_field", partitioningField));
    retval.append("    " + XmlHandler.addTagValue("partitioning_daily", partitioningDaily));
    retval.append("    " + XmlHandler.addTagValue("partitioning_monthly", partitioningMonthly));

    retval.append("    " + XmlHandler.addTagValue("tablename_in_field", tableNameInField));
    retval.append("    " + XmlHandler.addTagValue("tablename_field", tableNameField));
    retval.append("    " + XmlHandler.addTagValue("tablename_in_table", tableNameInTable));

    retval.append("    " + XmlHandler.addTagValue("return_keys", returningGeneratedKeys));
    retval.append("    " + XmlHandler.addTagValue("return_field", generatedKeyField));

    retval.append("    <fields>").append(Const.CR);

    for (int i = 0; i < fieldDatabase.length; i++) {
      retval.append("        <field>").append(Const.CR);
      retval.append("          ").append(XmlHandler.addTagValue("column_name", fieldDatabase[i]));
      retval.append("          ").append(XmlHandler.addTagValue("stream_name", fieldStream[i]));
      retval.append("        </field>").append(Const.CR);
    }
    retval.append("    </fields>").append(Const.CR);

    return retval.toString();
  }

  public void getFields(
      IRowMeta row,
      String origin,
      IRowMeta[] info,
      TransformMeta nextTransform,
      IVariables variables,
      IHopMetadataProvider metadataProvider)
      throws HopTransformException {
    // Just add the returning key field...
    if (returningGeneratedKeys && generatedKeyField != null && generatedKeyField.length() > 0) {
      IValueMeta key = new ValueMetaInteger(variables.resolve(generatedKeyField));
      key.setOrigin(origin);
      row.addValueMeta(key);
    }
  }

  public void check(
      List<ICheckResult> remarks,
      PipelineMeta pipelineMeta,
      TransformMeta transformMeta,
      IRowMeta prev,
      String[] input,
      String[] output,
      IRowMeta info,
      IVariables variables,
      IHopMetadataProvider metadataProvider) {
    if (databaseMeta != null) {
      CheckResult cr =
          new CheckResult(
              ICheckResult.TYPE_RESULT_OK,
              BaseMessages.getString(PKG, "TableOutputMeta.CheckResult.ConnectionExists"),
              transformMeta);
      remarks.add(cr);

      Database db = new Database(loggingObject, variables, databaseMeta);
      try {
        db.connect();

        cr =
            new CheckResult(
                ICheckResult.TYPE_RESULT_OK,
                BaseMessages.getString(PKG, "TableOutputMeta.CheckResult.ConnectionOk"),
                transformMeta);
        remarks.add(cr);

        if (!Utils.isEmpty(tableName)) {
          String realSchemaName = db.resolve(schemaName);
          String realTableName = db.resolve(tableName);
          String schemaTable =
              databaseMeta.getQuotedSchemaTableCombination(
                  variables, realSchemaName, realTableName);
          // Check if this table exists...
          if (db.checkTableExists(realSchemaName, realTableName)) {
            cr =
                new CheckResult(
                    ICheckResult.TYPE_RESULT_OK,
                    BaseMessages.getString(
                        PKG, "TableOutputMeta.CheckResult.TableAccessible", schemaTable),
                    transformMeta);
            remarks.add(cr);

            IRowMeta r = db.getTableFieldsMeta(realSchemaName, realTableName);
            if (r != null) {
              cr =
                  new CheckResult(
                      ICheckResult.TYPE_RESULT_OK,
                      BaseMessages.getString(
                          PKG, "TableOutputMeta.CheckResult.TableOk", schemaTable),
                      transformMeta);
              remarks.add(cr);

              String errorMessage = "";
              boolean errorFound = false;
              // OK, we have the table fields.
              // Now see what we can find as previous transform...
              if (prev != null && prev.size() > 0) {
                cr =
                    new CheckResult(
                        ICheckResult.TYPE_RESULT_OK,
                        BaseMessages.getString(
                            PKG, "TableOutputMeta.CheckResult.FieldsReceived", "" + prev.size()),
                        transformMeta);
                remarks.add(cr);

                if (!specifyFields()) {
                  // Starting from prev...
                  for (int i = 0; i < prev.size(); i++) {
                    IValueMeta pv = prev.getValueMeta(i);
                    int idx = r.indexOfValue(pv.getName());
                    if (idx < 0) {
                      errorMessage +=
                          "\t\t" + pv.getName() + " (" + pv.getTypeDesc() + ")" + Const.CR;
                      errorFound = true;
                    }
                  }
                  if (errorFound) {
                    errorMessage =
                        BaseMessages.getString(
                            PKG,
                            "TableOutputMeta.CheckResult.FieldsNotFoundInOutput",
                            errorMessage);

                    cr =
                        new CheckResult(
                            ICheckResult.TYPE_RESULT_ERROR, errorMessage, transformMeta);
                    remarks.add(cr);
                  } else {
                    cr =
                        new CheckResult(
                            ICheckResult.TYPE_RESULT_OK,
                            BaseMessages.getString(
                                PKG, "TableOutputMeta.CheckResult.AllFieldsFoundInOutput"),
                            transformMeta);
                    remarks.add(cr);
                  }
                } else {
                  // Specifying the column names explicitly
                  for (int i = 0; i < getFieldDatabase().length; i++) {
                    int idx = r.indexOfValue(getFieldDatabase()[i]);
                    if (idx < 0) {
                      errorMessage += "\t\t" + getFieldDatabase()[i] + Const.CR;
                      errorFound = true;
                    }
                  }
                  if (errorFound) {
                    errorMessage =
                        BaseMessages.getString(
                            PKG,
                            "TableOutputMeta.CheckResult.FieldsSpecifiedNotInTable",
                            errorMessage);

                    cr =
                        new CheckResult(
                            ICheckResult.TYPE_RESULT_ERROR, errorMessage, transformMeta);
                    remarks.add(cr);
                  } else {
                    cr =
                        new CheckResult(
                            ICheckResult.TYPE_RESULT_OK,
                            BaseMessages.getString(
                                PKG, "TableOutputMeta.CheckResult.AllFieldsFoundInOutput"),
                            transformMeta);
                    remarks.add(cr);
                  }
                }

                errorMessage = "";
                if (!specifyFields()) {
                  // Starting from table fields in r...
                  for (int i = 0; i < getFieldDatabase().length; i++) {
                    IValueMeta rv = r.getValueMeta(i);
                    int idx = prev.indexOfValue(rv.getName());
                    if (idx < 0) {
                      errorMessage +=
                          "\t\t" + rv.getName() + " (" + rv.getTypeDesc() + ")" + Const.CR;
                      errorFound = true;
                    }
                  }
                  if (errorFound) {
                    errorMessage =
                        BaseMessages.getString(
                            PKG, "TableOutputMeta.CheckResult.FieldsNotFound", errorMessage);

                    cr =
                        new CheckResult(
                            ICheckResult.TYPE_RESULT_WARNING, errorMessage, transformMeta);
                    remarks.add(cr);
                  } else {
                    cr =
                        new CheckResult(
                            ICheckResult.TYPE_RESULT_OK,
                            BaseMessages.getString(
                                PKG, "TableOutputMeta.CheckResult.AllFieldsFound"),
                            transformMeta);
                    remarks.add(cr);
                  }
                } else {
                  // Specifying the column names explicitly
                  for (int i = 0; i < getFieldStream().length; i++) {
                    int idx = prev.indexOfValue(getFieldStream()[i]);
                    if (idx < 0) {
                      errorMessage += "\t\t" + getFieldStream()[i] + Const.CR;
                      errorFound = true;
                    }
                  }
                  if (errorFound) {
                    errorMessage =
                        BaseMessages.getString(
                            PKG,
                            "TableOutputMeta.CheckResult.FieldsSpecifiedNotFound",
                            errorMessage);

                    cr =
                        new CheckResult(
                            ICheckResult.TYPE_RESULT_ERROR, errorMessage, transformMeta);
                    remarks.add(cr);
                  } else {
                    cr =
                        new CheckResult(
                            ICheckResult.TYPE_RESULT_OK,
                            BaseMessages.getString(
                                PKG, "TableOutputMeta.CheckResult.AllFieldsFound"),
                            transformMeta);
                    remarks.add(cr);
                  }
                }
              } else {
                cr =
                    new CheckResult(
                        ICheckResult.TYPE_RESULT_ERROR,
                        BaseMessages.getString(PKG, "TableOutputMeta.CheckResult.NoFields"),
                        transformMeta);
                remarks.add(cr);
              }
            } else {
              cr =
                  new CheckResult(
                      ICheckResult.TYPE_RESULT_ERROR,
                      BaseMessages.getString(PKG, "TableOutputMeta.CheckResult.TableNotAccessible"),
                      transformMeta);
              remarks.add(cr);
            }
          } else {
            cr =
                new CheckResult(
                    ICheckResult.TYPE_RESULT_ERROR,
                    BaseMessages.getString(
                        PKG, "TableOutputMeta.CheckResult.TableError", schemaTable),
                    transformMeta);
            remarks.add(cr);
          }
        } else {
          cr =
              new CheckResult(
                  ICheckResult.TYPE_RESULT_ERROR,
                  BaseMessages.getString(PKG, "TableOutputMeta.CheckResult.NoTableName"),
                  transformMeta);
          remarks.add(cr);
        }
      } catch (HopException e) {
        cr =
            new CheckResult(
                ICheckResult.TYPE_RESULT_ERROR,
                BaseMessages.getString(
                    PKG, "TableOutputMeta.CheckResult.UndefinedError", e.getMessage()),
                transformMeta);
        remarks.add(cr);
      } finally {
        db.disconnect();
      }
    } else {
      CheckResult cr =
          new CheckResult(
              ICheckResult.TYPE_RESULT_ERROR,
              BaseMessages.getString(PKG, "TableOutputMeta.CheckResult.NoConnection"),
              transformMeta);
      remarks.add(cr);
    }

    // See if we have input streams leading to this transform!
    if (input.length > 0) {
      CheckResult cr =
          new CheckResult(
              ICheckResult.TYPE_RESULT_OK,
              BaseMessages.getString(PKG, "TableOutputMeta.CheckResult.ExpectedInputOk"),
              transformMeta);
      remarks.add(cr);
    } else {
      CheckResult cr =
          new CheckResult(
              ICheckResult.TYPE_RESULT_ERROR,
              BaseMessages.getString(PKG, "TableOutputMeta.CheckResult.ExpectedInputError"),
              transformMeta);
      remarks.add(cr);
    }
  }

  public ITransform createTransform(
      TransformMeta transformMeta,
      TableOutputData data,
      int cnr,
      PipelineMeta pipelineMeta,
      Pipeline pipeline) {
    return new TableOutput(transformMeta, this, data, cnr, pipelineMeta, pipeline);
  }

  public TableOutputData getTransformData() {
    return new TableOutputData();
  }

  public void analyseImpact(
      IVariables variables,
      List<DatabaseImpact> impact,
      PipelineMeta pipelineMeta,
      TransformMeta transformMeta,
      IRowMeta prev,
      String[] input,
      String[] output,
      IRowMeta info,
      IHopMetadataProvider metadataProvider) {
    if (truncateTable) {
      DatabaseImpact ii =
          new DatabaseImpact(
              DatabaseImpact.TYPE_IMPACT_TRUNCATE,
              pipelineMeta.getName(),
              transformMeta.getName(),
              databaseMeta.getDatabaseName(),
              tableName,
              "",
              "",
              "",
              "",
              "Truncate of table");
      impact.add(ii);
    }
    // The values that are entering this transform are in "prev":
    if (prev != null) {
      for (int i = 0; i < prev.size(); i++) {
        IValueMeta v = prev.getValueMeta(i);
        DatabaseImpact ii =
            new DatabaseImpact(
                DatabaseImpact.TYPE_IMPACT_WRITE,
                pipelineMeta.getName(),
                transformMeta.getName(),
                databaseMeta.getDatabaseName(),
                tableName,
                v.getName(),
                v.getName(),
                v != null ? v.getOrigin() : "?",
                "",
                "Type = " + v.toStringMeta());
        impact.add(ii);
      }
    }
  }

  public SqlStatement getSqlStatements(
      IVariables variables,
      PipelineMeta pipelineMeta,
      TransformMeta transformMeta,
      IRowMeta prev,
      IHopMetadataProvider metadataProvider) {
    return getSqlStatements(variables, pipelineMeta, transformMeta, prev, null, false, null);
  }

  public SqlStatement getSqlStatements(
      IVariables variables,
      PipelineMeta pipelineMeta,
      TransformMeta transformMeta,
      IRowMeta prev,
      String tk,
      boolean useAutoIncrement,
      String pk) {
    SqlStatement retval =
        new SqlStatement(transformMeta.getName(), databaseMeta, null); // default: nothing to do!

    if (databaseMeta != null) {
      if (prev != null && prev.size() > 0) {
        if (!Utils.isEmpty(tableName)) {
          Database db = new Database(loggingObject, variables, databaseMeta);
          try {
            db.connect();

            String schemaTable =
                databaseMeta.getQuotedSchemaTableCombination(variables, schemaName, tableName);
            String crTable = db.getDDL(schemaTable, prev, tk, useAutoIncrement, pk);

            // Empty string means: nothing to do: set it to null...
            if (crTable == null || crTable.length() == 0) {
              crTable = null;
            }

            retval.setSql(crTable);
          } catch (HopDatabaseException dbe) {
            retval.setError(
                BaseMessages.getString(
                    PKG, "TableOutputMeta.Error.ErrorConnecting", dbe.getMessage()));
          } finally {
            db.disconnect();
          }
        } else {
          retval.setError(BaseMessages.getString(PKG, "TableOutputMeta.Error.NoTable"));
        }
      } else {
        retval.setError(BaseMessages.getString(PKG, "TableOutputMeta.Error.NoInput"));
      }
    } else {
      retval.setError(BaseMessages.getString(PKG, "TableOutputMeta.Error.NoConnection"));
    }

    return retval;
  }

  public IRowMeta getRequiredFields(IVariables variables) throws HopException {
    String realTableName = variables.resolve(tableName);
    String realSchemaName = variables.resolve(schemaName);

    if (databaseMeta != null) {
      Database db = new Database(loggingObject, variables, databaseMeta);
      try {
        db.connect();

        if (!Utils.isEmpty(realTableName)) {
          // Check if this table exists...
          if (db.checkTableExists(realSchemaName, realTableName)) {
            return db.getTableFieldsMeta(realSchemaName, realTableName);
          } else {
            throw new HopException(
                BaseMessages.getString(PKG, "TableOutputMeta.Exception.TableNotFound"));
          }
        } else {
          throw new HopException(
              BaseMessages.getString(PKG, "TableOutputMeta.Exception.TableNotSpecified"));
        }
      } catch (Exception e) {
        throw new HopException(
            BaseMessages.getString(PKG, "TableOutputMeta.Exception.ErrorGettingFields"), e);
      } finally {
        db.disconnect();
      }
    } else {
      throw new HopException(
          BaseMessages.getString(PKG, "TableOutputMeta.Exception.ConnectionNotDefined"));
    }
  }

  public DatabaseMeta[] getUsedDatabaseConnections() {
    if (databaseMeta != null) {
      return new DatabaseMeta[] {databaseMeta};
    } else {
      return super.getUsedDatabaseConnections();
    }
  }

  /** @return Fields containing the values in the input stream to insert. */
  public String[] getFieldStream() {
    return fieldStream;
  }

  /**
   * @param fieldStream The fields containing the values in the input stream to insert in the table.
   */
  public void setFieldStream(String[] fieldStream) {
    this.fieldStream = fieldStream;
  }

  @Override
  public RowMeta getRowMeta(IVariables variables, ITransformData transformData) {
    return (RowMeta) ((TableOutputData) transformData).insertRowMeta;
  }

  @Override
  public List<String> getDatabaseFields() {
    if (specifyFields()) {
      return Arrays.asList(getFieldDatabase());
    }
    return Collections.emptyList();
  }

  @Override
  public List<String> getStreamFields() {
    if (specifyFields()) {
      return Arrays.asList(getFieldStream());
    }
    return Collections.emptyList();
  }

  /** @return Fields containing the fieldnames in the database insert. */
  public String[] getFieldDatabase() {
    return fieldDatabase;
  }

  /** @param fieldDatabase The fields containing the names of the fields to insert. */
  public void setFieldDatabase(String[] fieldDatabase) {
    this.fieldDatabase = fieldDatabase;
  }

  /** @return the schemaName */
  public String getSchemaName() {
    return schemaName;
  }

  /** @param schemaName the schemaName to set */
  public void setSchemaName(String schemaName) {
    this.schemaName = schemaName;
  }

  public boolean supportsErrorHandling() {
    if (databaseMeta != null) {
      return databaseMeta.getIDatabase().supportsErrorHandling();
    } else {
      return true;
    }
  }

  @Override
  public String getMissingDatabaseConnectionInformationMessage() {
    // Use default connection missing message
    return null;
  }
}
