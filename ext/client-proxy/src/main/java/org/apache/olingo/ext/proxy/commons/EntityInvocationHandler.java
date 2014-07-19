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
package org.apache.olingo.ext.proxy.commons;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntityRequest;
import org.apache.olingo.client.api.communication.request.retrieve.ODataMediaRequest;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.uri.CommonURIBuilder;
import org.apache.olingo.client.core.uri.URIUtils;
import org.apache.olingo.commons.api.domain.CommonODataEntity;
import org.apache.olingo.commons.api.domain.CommonODataProperty;
import org.apache.olingo.commons.api.domain.v4.ODataAnnotation;
import org.apache.olingo.commons.api.domain.v4.ODataEntity;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.constants.ODataServiceVersion;
import org.apache.olingo.commons.api.format.ODataFormat;
import org.apache.olingo.ext.proxy.Service;
import org.apache.olingo.ext.proxy.api.AbstractTerm;
import org.apache.olingo.ext.proxy.api.Annotatable;
import org.apache.olingo.ext.proxy.api.annotations.CompoundKey;
import org.apache.olingo.ext.proxy.api.annotations.CompoundKeyElement;
import org.apache.olingo.ext.proxy.api.annotations.EntityType;
import org.apache.olingo.ext.proxy.api.annotations.Namespace;
import org.apache.olingo.ext.proxy.api.annotations.NavigationProperty;
import org.apache.olingo.ext.proxy.api.annotations.Property;
import org.apache.olingo.ext.proxy.api.annotations.Term;
import org.apache.olingo.ext.proxy.context.AttachedEntityStatus;
import org.apache.olingo.ext.proxy.context.EntityUUID;
import org.apache.olingo.ext.proxy.utils.CoreUtils;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class EntityInvocationHandler extends AbstractStructuredInvocationHandler implements Annotatable {

  private static final long serialVersionUID = 2629912294765040037L;

  private URI baseURI;

  protected final Map<String, Object> propertyChanges = new HashMap<String, Object>();

  protected final Map<NavigationProperty, Object> linkChanges = new HashMap<NavigationProperty, Object>();

  protected final Map<NavigationProperty, Object> linkChache = new HashMap<NavigationProperty, Object>();

  protected int propertiesTag = 0;

  protected int linksTag = 0;

  private final Map<String, InputStream> streamedPropertyChanges = new HashMap<String, InputStream>();

  private final Map<Class<? extends AbstractTerm>, Object> annotations =
          new HashMap<Class<? extends AbstractTerm>, Object>();

  private InputStream stream;

  private EntityUUID uuid;

  static EntityInvocationHandler getInstance(
          final CommonODataEntity entity,
          final EntitySetInvocationHandler<?, ?, ?> entitySet,
          final Class<?> typeRef) {

    return new EntityInvocationHandler(
            null,
            entity,
            entitySet.getURI(),
            typeRef,
            entitySet.service);
  }

  static EntityInvocationHandler getInstance(
          final Object key,
          final CommonODataEntity entity,
          final URI entitySetURI,
          final Class<?> typeRef,
          final Service<?> service) {

    return new EntityInvocationHandler(key, entity, entitySetURI, typeRef, service);
  }

  public static EntityInvocationHandler getInstance(
          final CommonODataEntity entity,
          final URI entitySetURI,
          final Class<?> typeRef,
          final Service<?> service) {

    return new EntityInvocationHandler(null, entity, entitySetURI, typeRef, service);
  }

  public static EntityInvocationHandler getInstance(
          final CommonODataEntity entity,
          final URI entitySetURI,
          final URI entityURI,
          final Class<?> typeRef,
          final Service<?> service) {

    return new EntityInvocationHandler(entity, entityURI, entitySetURI, typeRef, service);
  }

  public static EntityInvocationHandler getInstance(
          final Class<?> typeRef,
          final Service<?> service) {

    return new EntityInvocationHandler(typeRef, service);
  }

  private EntityInvocationHandler(
          final Class<?> typeRef,
          final Service<?> service) {

    super(typeRef, service);

    final String name = typeRef.getAnnotation(org.apache.olingo.ext.proxy.api.annotations.EntityType.class).name();
    final String namespace = typeRef.getAnnotation(Namespace.class).value();

    this.internal = service.getClient().getObjectFactory().newEntity(new FullQualifiedName(namespace, name));
    CommonODataEntity.class.cast(this.internal).setMediaEntity(typeRef.getAnnotation(EntityType.class).hasStream());

    this.uuid = new EntityUUID(
            null,
            typeRef,
            null);
  }

  private EntityInvocationHandler(
          final CommonODataEntity entity,
          final URI entitySetURI,
          final URI entityURI,
          final Class<?> typeRef,
          final Service<?> service) {
    super(typeRef, entity, service);

    if (entityURI != null) {
      this.baseURI = entityURI;
      this.uri = getClient().newURIBuilder(baseURI.toASCIIString());
    } else {
      this.baseURI = null;
      this.uri = null;
    }

    this.internal = entity;
    getEntity().setMediaEntity(typeRef.getAnnotation(EntityType.class).hasStream());

    this.uuid = new EntityUUID(entitySetURI, typeRef, null);
  }

  private EntityInvocationHandler(
          final Object entityKey,
          final CommonODataEntity entity,
          final URI entitySetURI,
          final Class<?> typeRef,
          final Service<?> service) {

    super(typeRef, entity, service);

    final Object key = entityKey == null ? CoreUtils.getKey(getClient(), this, typeRef, entity) : entityKey;

    if (entity.getEditLink() != null) {
      this.baseURI = entity.getEditLink();
      this.uri = getClient().newURIBuilder(baseURI.toASCIIString());
    } else if (key != null) {
      final CommonURIBuilder<?> uriBuilder = getClient().newURIBuilder(entitySetURI.toASCIIString());

      if (key.getClass().getAnnotation(CompoundKey.class) == null) {
        LOG.debug("Append key segment '{}'", key);
        uriBuilder.appendKeySegment(key);
      } else {
        LOG.debug("Append compound key segment '{}'", key);
        uriBuilder.appendKeySegment(getCompoundKey(key));
      }

      this.uri = uriBuilder;
      this.baseURI = this.uri.build();
      entity.setEditLink(this.baseURI);
    } else {
      this.baseURI = null;
      this.uri = null;
    }

    this.internal = entity;
    getEntity().setMediaEntity(typeRef.getAnnotation(EntityType.class).hasStream());

    this.uuid = new EntityUUID(entitySetURI, typeRef, key);
  }

  public void setEntity(final CommonODataEntity entity) {
    this.internal = entity;
    getEntity().setMediaEntity(typeRef.getAnnotation(EntityType.class).hasStream());

    this.uuid = new EntityUUID(
            getUUID().getEntitySetURI(),
            getUUID().getType(),
            CoreUtils.getKey(getClient(), this, typeRef, entity));

    // fix for OLINGO-353
    if (this.uri == null) {
      this.baseURI = entity.getEditLink();
      this.uri = this.baseURI == null ? null : getClient().newURIBuilder(this.baseURI.toASCIIString());
    }

    this.streamedPropertyChanges.clear();
    this.propertyChanges.clear();
    this.linkChanges.clear();
    this.linkChache.clear();
    this.propertiesTag = 0;
    this.linksTag = 0;
    this.annotations.clear();
  }

  public EntityUUID getUUID() {
    return uuid;
  }

  public EntityUUID updateUUID(final URI entitySetURI, final Class<?> type, final Object key) {
    this.uuid = new EntityUUID(entitySetURI, type, key);
    return this.uuid;
  }

  public URI getEntitySetURI() {
    return uuid.getEntitySetURI();
  }

  public final CommonODataEntity getEntity() {
    return (CommonODataEntity) internal;
  }

  public URI getEntityURI() {
    return this.baseURI;
  }

  /**
   * Gets the current ETag defined into the wrapped entity.
   *
   * @return
   */
  public String getETag() {
    return getEntity().getETag();
  }

  /**
   * Overrides ETag value defined into the wrapped entity.
   *
   * @param eTag ETag.
   */
  public void setETag(final String eTag) {
    getEntity().setETag(eTag);
  }

  public Map<String, Object> getPropertyChanges() {
    return propertyChanges;
  }

  @Override
  public void addAdditionalProperty(final String name, final Object value) {
    propertyChanges.put(name, value);
    attach(AttachedEntityStatus.CHANGED);
  }

  public Map<NavigationProperty, Object> getLinkChanges() {
    return linkChanges;
  }

  public Map<Class<? extends AbstractTerm>, Object> getAnnotations() {
    return annotations;
  }

  private void updatePropertiesTag(final int checkpoint) {
    if (checkpoint == propertiesTag) {
      propertiesTag = propertyChanges.hashCode();
    }
  }

  private void updateLinksTag(final int checkpoint) {
    if (checkpoint == linksTag) {
      linksTag = linkChanges.hashCode();
    }
  }

  @Override
  protected Object getPropertyValue(final String name, final Type type) {
    try {
      if (!(type instanceof ParameterizedType) && (Class<?>) type == InputStream.class) {
        return getStreamedProperty(name);
      } else {
        final CommonODataProperty property = getEntity().getProperty(name);

        Object res;
        if (propertyChanges.containsKey(name)) {
          res = propertyChanges.get(name);
        } else {
          res = property == null || property.hasNullValue()
                  ? null
                  : CoreUtils.getObjectFromODataValue(getClient(), property.getValue(), type, this);

          if (res != null) {
            cacheProperty(name, res);
          }
        }

        return res;
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("Error getting value for property '" + name + "'", e);
    }
  }

  @Override
  public Collection<String> getAdditionalPropertyNames() {
    final Set<String> res = new HashSet<String>(propertyChanges.keySet());
    final Set<String> propertyNames = new HashSet<String>();
    for (Method method : typeRef.getMethods()) {
      final Annotation ann = method.getAnnotation(Property.class);
      if (ann != null) {
        final String property = ((Property) ann).name();
        propertyNames.add(property);

        // maybe someone could add a normal attribute to the additional set
        res.remove(property);
      }
    }

    for (CommonODataProperty property : getEntity().getProperties()) {
      if (!propertyNames.contains(property.getName())) {
        res.add(property.getName());
      }
    }

    return res;
  }

  @Override
  protected void setPropertyValue(final Property property, final Object value) {
    if (EdmPrimitiveTypeKind.Stream.getFullQualifiedName().toString().equalsIgnoreCase(property.type())) {
      setStreamedProperty(property, (InputStream) value);
    } else {
      propertyChanges.put(property.name(), value);

      if (value != null) {
        Collection<?> coll;
        if (Collection.class.isAssignableFrom(value.getClass())) {
          coll = Collection.class.cast(value);
        } else {
          coll = Collections.singleton(value);
        }

        for (Object item : coll) {
          if (item instanceof Proxy) {
            final InvocationHandler handler = Proxy.getInvocationHandler(item);
            if ((handler instanceof ComplexInvocationHandler)
                    && ((ComplexInvocationHandler) handler).getEntityHandler() == null) {
              ((ComplexInvocationHandler) handler).setEntityHandler(this);
            }
          }
        }
      }
    }

    attach(AttachedEntityStatus.CHANGED);
  }

  @Override
  public boolean isChanged() {
    return this.linkChanges.hashCode() != this.linksTag
            || this.propertyChanges.hashCode() != this.propertiesTag
            || this.stream != null
            || !this.streamedPropertyChanges.isEmpty();
  }

  public void setStream(final InputStream stream) {
    if (typeRef.getAnnotation(EntityType.class).hasStream()) {
      IOUtils.closeQuietly(this.stream);
      this.stream = stream;
      attach(AttachedEntityStatus.CHANGED);
    }
  }

  public InputStream getStreamChanges() {
    return this.stream;
  }

  public Map<String, InputStream> getStreamedPropertyChanges() {
    return streamedPropertyChanges;
  }

  public InputStream getStream() {
    final URI contentSource = getEntity().getMediaContentSource();

    if (this.stream == null
            && typeRef.getAnnotation(EntityType.class).hasStream()
            && contentSource != null) {

      final ODataMediaRequest retrieveReq = getClient().getRetrieveRequestFactory()
              .getMediaEntityRequest(contentSource);
      if (StringUtils.isNotBlank(getEntity().getMediaContentType())) {
        retrieveReq.setFormat(ODataFormat.fromString(getEntity().getMediaContentType()));
      }
      this.stream = retrieveReq.execute().getBody();
    }

    return this.stream;
  }

  public Object getStreamedProperty(final String name) {
    InputStream res = streamedPropertyChanges.get(name);

    try {
      if (res == null) {
        final URI link = URIUtils.getURI(
                getClient().getServiceRoot(),
                CoreUtils.getMediaEditLink(name, getEntity()).toASCIIString());

        final ODataMediaRequest req = getClient().getRetrieveRequestFactory().getMediaRequest(link);
        res = req.execute().getBody();
      }
    } catch (Exception e) {
      res = null;
    }

    return res;
  }

  private void setStreamedProperty(final Property property, final InputStream input) {
    final Object obj = streamedPropertyChanges.get(property.name());
    if (obj instanceof InputStream) {
      IOUtils.closeQuietly((InputStream) obj);
    }

    streamedPropertyChanges.put(property.name(), input);
  }

  @Override
  protected Object getNavigationPropertyValue(final NavigationProperty property, final Method getter) {
    final Object navPropValue;

    if (linkChanges.containsKey(property)) {
      navPropValue = linkChanges.get(property);
    } else if (linkChache.containsKey(property)) {
      navPropValue = linkChache.get(property);
    } else {
      navPropValue = retrieveNavigationProperty(property, getter);
    }

    if (navPropValue != null) {
      cacheLink(property, navPropValue);
    }

    return navPropValue;
  }

  @Override
  public void removeAdditionalProperty(final String name) {
    propertyChanges.remove(name);
    attach(AttachedEntityStatus.CHANGED);
  }

  protected void cacheProperty(final String name, final Object value) {
    final int checkpoint = propertyChanges.hashCode();
    propertyChanges.put(name, value);
    updatePropertiesTag(checkpoint);
  }

  @Override
  protected void addLinkChanges(final NavigationProperty navProp, final Object value) {
    final int checkpoint = linkChanges.hashCode();
    linkChanges.put(navProp, value);
    updateLinksTag(checkpoint);

    if (linkChache.containsKey(navProp)) {
      linkChache.remove(navProp);
    }
  }

  protected void cacheLink(final NavigationProperty navProp, final Object value) {
    linkChache.put(navProp, value);
  }

  @Override
  public void addAnnotation(final Class<? extends AbstractTerm> term, final Object value) {
    this.annotations.put(term, value);

    if (value != null) {
      Collection<?> coll;
      if (Collection.class.isAssignableFrom(value.getClass())) {
        coll = Collection.class.cast(value);
      } else {
        coll = Collections.singleton(value);
      }

      for (Object item : coll) {
        if (item instanceof Proxy) {
          final InvocationHandler handler = Proxy.getInvocationHandler(item);
          if ((handler instanceof ComplexInvocationHandler)
                  && ((ComplexInvocationHandler) handler).getEntityHandler() == null) {
            ((ComplexInvocationHandler) handler).setEntityHandler(this);
          }
        }
      }
    }

    attach(AttachedEntityStatus.CHANGED);
  }

  @Override
  public void removeAnnotation(final Class<? extends AbstractTerm> term) {
    this.annotations.remove(term);
    attach(AttachedEntityStatus.CHANGED);
  }

  @Override
  public Object getAnnotation(final Class<? extends AbstractTerm> term) {
    Object res = null;

    if (annotations.containsKey(term)) {
      res = annotations.get(term);
    } else if (getEntity() instanceof ODataEntity) {
      try {
        final Term termAnn = term.getAnnotation(Term.class);
        final Namespace namespaceAnn = term.getAnnotation(Namespace.class);
        ODataAnnotation annotation = null;
        for (ODataAnnotation _annotation : ((ODataEntity) getEntity()).getAnnotations()) {
          if ((namespaceAnn.value() + "." + termAnn.name()).equals(_annotation.getTerm())) {
            annotation = _annotation;
          }
        }
        res = annotation == null || annotation.hasNullValue()
                ? null
                : CoreUtils.getObjectFromODataValue(getClient(), annotation.getValue(), null, this);
        if (res != null) {
          annotations.put(term, res);
        }
      } catch (Exception e) {
        throw new IllegalArgumentException("Error getting annotation for term '" + term.getName() + "'", e);
      }
    }

    return res;
  }

  @Override
  public Collection<Class<? extends AbstractTerm>> getAnnotationTerms() {
    return getEntity() instanceof ODataEntity
            ? CoreUtils.getAnnotationTerms(((ODataEntity) getEntity()).getAnnotations())
            : Collections.<Class<? extends AbstractTerm>>emptyList();
  }

  @Override
  protected void load() {
    // Search against the service
    final Object key = uuid.getKey();

    try {
      final ODataEntityRequest<CommonODataEntity> req =
              getClient().getRetrieveRequestFactory().getEntityRequest(uri.build());

      if (getClient().getServiceVersion().compareTo(ODataServiceVersion.V30) > 0) {
        req.setPrefer(getClient().newPreferences().includeAnnotations("*"));
      }

      final ODataRetrieveResponse<CommonODataEntity> res = req.execute();

      final String etag = res.getETag();
      final CommonODataEntity entity = res.getBody();
      if (entity == null) {
        throw new IllegalArgumentException("Invalid " + typeRef.getSimpleName() + "(" + key + ")");
      }

      setEntity(entity);
      setETag(etag);

      if (key != null && !key.equals(CoreUtils.getKey(getClient(), this, typeRef, entity))) {
        throw new IllegalArgumentException("Invalid " + typeRef.getSimpleName() + "(" + key + ")");
      }

      IOUtils.closeQuietly(this.stream);
      this.stream = null;
    } catch (IllegalArgumentException e) {
      LOG.warn("Entity '" + uuid + "' not found", e);
      throw e;
    } catch (Exception e) {
      LOG.warn("Error retrieving entity '" + uuid + "'", e);
      throw new IllegalArgumentException("Error retrieving " + typeRef.getSimpleName() + "(" + key + ")", e);
    }
  }

  private Map<String, Object> getCompoundKey(final Object key) {
    final Set<CompoundKeyElementWrapper> elements = new TreeSet<CompoundKeyElementWrapper>();

    for (Method method : key.getClass().getMethods()) {
      final Annotation annotation = method.getAnnotation(CompoundKeyElement.class);
      if (annotation instanceof CompoundKeyElement) {
        elements.add(new CompoundKeyElementWrapper(
                ((CompoundKeyElement) annotation).name(), method, ((CompoundKeyElement) annotation).position()));
      }
    }

    final LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();

    for (CompoundKeyElementWrapper element : elements) {
      try {
        map.put(element.getName(), element.getMethod().invoke(key));
      } catch (Exception e) {
        LOG.warn("Error retrieving compound key element '{}' value", element.getName(), e);
      }
    }

    return map;
  }

  public void expand(final String... expand) {
    this.uri.expand(expand);
  }

  public void select(final String... select) {
    this.uri.select(select);
  }

  public void clearQueryOptions() {
    this.uri = getClient().newURIBuilder(baseURI.toASCIIString());
  }

  @Override
  public String toString() {
    return uuid.toString();
  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    return obj instanceof EntityInvocationHandler
            && ((EntityInvocationHandler) obj).getUUID().equals(uuid);
  }
}
