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
package org.apache.olingo.commons.api.data;

import java.net.URI;
import java.util.List;

public interface Feed {

  /**
   * Gets base URI.
   *
   * @return base URI.
   */
  URI getBaseURI();

  /**
   * The context URL describes the content of the payload. It consists of the canonical metadata document URL and a
   * fragment identifying the relevant portion of the metadata document.
   *
   * @return context URL.
   */
  URI getContextURL();

  /**
   * Set context URL.
   *
   * @param contextURL context URL.
   */
  void setContextURL(final URI contextURL);

  /**
   * Sets number of entries.
   *
   * @param count number of entries
   */
  void setCount(Integer count);

  /**
   * Gets number of entries - if it was required.
   *
   * @return number of entries into the feed.
   */
  Integer getCount();

  /**
   * Gest feed ID.
   *
   * @return feed ID.
   */
  String getId();

  /**
   * Gets entries.
   *
   * @return entries.
   */
  List<Entry> getEntries();

  /**
   * Gets next link if exists.
   *
   * @return next link if exists; null otherwise.
   */
  URI getNext();

  /**
   * Sets next link.
   *
   * @param next next link.
   */
  void setNext(URI next);
}
