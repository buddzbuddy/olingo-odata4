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
package org.apache.olingo.commons.api.domain;

import java.io.Serializable;
import java.net.URI;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract representation of OData entities and links.
 */
public abstract class ODataItem implements Serializable {

  private static final long serialVersionUID = -2600707722689304686L;

  /**
   * Logger.
   */
  protected static final Logger LOG = LoggerFactory.getLogger(ODataItem.class);

  /**
   * OData entity name/type.
   */
  private final String name;

  /**
   * OData item self link.
   */
  protected URI link;

  /**
   * Constructor.
   *
   * @param name OData entity name.
   */
  public ODataItem(final String name) {
    this.name = name;
  }

  /**
   * Returns OData entity name.
   *
   * @return entity name.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns self link.
   *
   * @return entity edit link.
   */
  public URI getLink() {
    return link;
  }

  /**
   * Sets self link.
   *
   * @param link link.
   */
  public void setLink(final URI link) {
    this.link = link;
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public boolean equals(final Object obj) {
    return EqualsBuilder.reflectionEquals(this, obj);
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  /**
   * {@inheritDoc }
   */
  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
  }
}
