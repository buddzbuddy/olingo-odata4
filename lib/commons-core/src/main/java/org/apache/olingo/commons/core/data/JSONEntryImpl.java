/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.commons.core.data;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.net.URI;
import org.apache.olingo.commons.api.Constants;

/**
 * A single entry, represented via JSON.
 */
@JsonSerialize(using = JSONEntrySerializer.class)
@JsonDeserialize(using = JSONEntryDeserializer.class)
public class JSONEntryImpl extends AbstractEntry {

  private static final long serialVersionUID = -5275365545400797758L;

  private String mediaETag;

  @Override
  public URI getBaseURI() {
    URI baseURI = null;
    if (getContextURL() != null) {
      final String metadataURI = getContextURL().toASCIIString();
      baseURI = URI.create(metadataURI.substring(0, metadataURI.indexOf(Constants.METADATA)));
    }

    return baseURI;
  }

  /**
   * The odata.mediaEtag annotation MAY be included; its value MUST be the ETag of the binary stream represented by this
   * media entity or named stream property.
   *
   * @return odata.mediaEtag annotation value.
   */
  public String getMediaETag() {
    return mediaETag;
  }

  /**
   * The odata.mediaEtag annotation MAY be included; its value MUST be the ETag of the binary stream represented by this
   * media entity or named stream property.
   *
   * @param eTag odata.mediaEtag annotation value.
   */
  public void setMediaETag(final String eTag) {
    this.mediaETag = eTag;
  }
}
