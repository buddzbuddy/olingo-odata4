/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.olingo.commons.core.edm.provider;

import java.io.InputStream;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmServiceMetadata;
import org.apache.olingo.commons.api.edm.helper.EdmEntitySetInfo;
import org.apache.olingo.commons.api.edm.helper.EdmFunctionImportInfo;
import org.apache.olingo.commons.api.edm.helper.EdmSingletonInfo;
import org.apache.olingo.commons.api.edm.provider.EdmProvider;

public class EdmServiceMetadataImpl implements EdmServiceMetadata {

  public EdmServiceMetadataImpl(final EdmProvider provider) {}

  @Override
  public InputStream getMetadata() {
    return null;
  }

  @Override
  public String getDataServiceVersion() {
    // TODO: make constant
    return "4.0";
  }

  @Override
  public List<EdmEntitySetInfo> getEntitySetInfos() {
    return null;
  }

  @Override
  public List<EdmSingletonInfo> getSingletonInfos() {
    return null;
  }

  @Override
  public List<EdmFunctionImportInfo> getFunctionImportInfos() {
    return null;
  }

}