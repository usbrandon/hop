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
 *
 */
package org.apache.hop.pipeline.transforms.cassandrasstableoutput;

import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.pipeline.transform.BaseTransformData;
import org.apache.hop.pipeline.transform.ITransformData;

/** Data class for SSTablesOutput transform. */
public class SSTableOutputData extends BaseTransformData implements ITransformData {

  /** The output data format */
  protected IRowMeta outputRowMeta;

  /**
   * Get the output row format
   *
   * @return the output row format
   */
  public IRowMeta getOutputRowMeta() {
    return outputRowMeta;
  }

  /**
   * Set the output row format
   *
   * @param rmi the output row format
   */
  public void setOutputRowMeta(IRowMeta rmi) {
    outputRowMeta = rmi;
  }
}
