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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.olingo.client.core.uri.URIUtils;
import org.apache.olingo.commons.api.domain.CommonODataEntity;
import org.apache.olingo.commons.api.domain.ODataInlineEntity;
import org.apache.olingo.commons.api.domain.ODataInlineEntitySet;
import org.apache.olingo.commons.api.domain.ODataLink;
import org.apache.olingo.commons.api.domain.ODataLinked;
import org.apache.olingo.ext.proxy.Service;
import org.apache.olingo.ext.proxy.api.EntityCollection;
import org.apache.olingo.ext.proxy.api.AbstractEntitySet;
import org.apache.olingo.ext.proxy.api.annotations.NavigationProperty;
import org.apache.olingo.ext.proxy.api.annotations.Property;
import org.apache.olingo.ext.proxy.context.AttachedEntityStatus;
import org.apache.olingo.ext.proxy.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.olingo.client.api.uri.CommonURIBuilder;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.ext.proxy.api.annotations.Namespace;
import org.apache.olingo.ext.proxy.context.EntityUUID;
import org.apache.olingo.ext.proxy.utils.CoreUtils;

public abstract class AbstractStructuredInvocationHandler extends AbstractInvocationHandler {

  private static final long serialVersionUID = 2629912294765040037L;

  /**
   * Logger.
   */
  protected static final Logger LOG = LoggerFactory.getLogger(AbstractStructuredInvocationHandler.class);

  protected CommonURIBuilder<?> uri;

  protected final Class<?> typeRef;

  protected EntityInvocationHandler entityHandler;

  protected Object internal;

  private final Map<String, AnnotatableInvocationHandler> propAnnotatableHandlers =
          new HashMap<String, AnnotatableInvocationHandler>();

  private final Map<String, AnnotatableInvocationHandler> navPropAnnotatableHandlers =
          new HashMap<String, AnnotatableInvocationHandler>();

  protected AbstractStructuredInvocationHandler(
          final Class<?> typeRef,
          final Service<?> service) {

    super(service);
    this.internal = null;
    this.typeRef = typeRef;
    this.entityHandler = null;
  }

  protected AbstractStructuredInvocationHandler(
          final Class<?> typeRef,
          final Object internal,
          final Service<?> service) {

    super(service);
    this.internal = internal;
    this.typeRef = typeRef;
    this.entityHandler = null;
  }

  protected AbstractStructuredInvocationHandler(
          final Class<?> typeRef,
          final Object internal,
          final EntityInvocationHandler entityHandler) {

    super(entityHandler == null ? null : entityHandler.service);
    this.internal = internal;
    this.typeRef = typeRef;
    // prevent memory leak
    this.entityHandler = entityHandler == this ? null : entityHandler;
  }

  public Object getInternal() {
    return internal;
  }

  public EntityInvocationHandler getEntityHandler() {
    return entityHandler == null
            ? this instanceof EntityInvocationHandler
            ? EntityInvocationHandler.class.cast(this)
            : null
            : entityHandler;
  }

  public void setEntityHandler(final EntityInvocationHandler entityHandler) {
    // prevents memory leak
    this.entityHandler = entityHandler == this ? null : entityHandler;
  }

  public Class<?> getTypeRef() {
    return typeRef;
  }

  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
    if ("expand".equals(method.getName()) || "select".equals(method.getName())) {
      invokeSelfMethod(method, args);
      return proxy;
    } else if (isSelfMethod(method, args)) {
      return invokeSelfMethod(method, args);
    } else if ("load".equals(method.getName()) && ArrayUtils.isEmpty(args)) {
      load();
      return proxy;
    } else if ("operations".equals(method.getName()) && ArrayUtils.isEmpty(args)) {
      final Class<?> returnType = method.getReturnType();

      return Proxy.newProxyInstance(
              Thread.currentThread().getContextClassLoader(),
              new Class<?>[] {returnType},
              OperationInvocationHandler.getInstance(getEntityHandler()));
    } else if ("annotations".equals(method.getName()) && ArrayUtils.isEmpty(args)) {
      final Class<?> returnType = method.getReturnType();

      return Proxy.newProxyInstance(
              Thread.currentThread().getContextClassLoader(),
              new Class<?>[] {returnType},
              AnnotatationsInvocationHandler.getInstance(getEntityHandler(), this));
    } else if (method.getName().startsWith("get")) {
      // Assumption: for each getter will always exist a setter and viceversa.
      // get method annotation and check if it exists as expected
      final Object res;
      final Method getter = typeRef.getMethod(method.getName());

      final Property property = ClassUtils.getAnnotation(Property.class, getter);
      if (property == null) {
        final NavigationProperty navProp = ClassUtils.getAnnotation(NavigationProperty.class, getter);
        if (navProp == null) {
          throw new UnsupportedOperationException("Unsupported method " + method.getName());
        } else {
          // if the getter refers to a navigation property ... navigate and follow link if necessary
          res = getNavigationPropertyValue(navProp, getter);
        }
      } else {
        // if the getter refers to a property .... get property from wrapped entity
        res = getPropertyValue(property.name(), getter.getGenericReturnType());
      }

      return res;
    } else if (method.getName().startsWith("set")) {
      // get the corresponding getter method (see assumption above)
      final String getterName = method.getName().replaceFirst("set", "get");
      final Method getter = typeRef.getMethod(getterName);

      final Property property = ClassUtils.getAnnotation(Property.class, getter);
      if (property == null) {
        final NavigationProperty navProp = ClassUtils.getAnnotation(NavigationProperty.class, getter);
        if (navProp == null) {
          throw new UnsupportedOperationException("Unsupported method " + method.getName());
        } else {
          // if the getter refers to a navigation property ... 
          if (ArrayUtils.isEmpty(args) || args.length != 1) {
            throw new IllegalArgumentException("Invalid argument");
          }

          setNavigationPropertyValue(navProp, args[0]);
        }
      } else {
        setPropertyValue(property, args[0]);
      }

      return ClassUtils.returnVoid();
    } else {
      throw new NoSuchMethodException(method.getName());
    }
  }

  protected void attach() {
    attach(AttachedEntityStatus.ATTACHED, false);
  }

  protected void attach(final AttachedEntityStatus status) {
    attach(status, true);
  }

  protected void attach(final AttachedEntityStatus status, final boolean override) {
    if (getContext().entityContext().isAttached(getEntityHandler())) {
      if (override) {
        getContext().entityContext().setStatus(getEntityHandler(), status);
      }
    } else {
      getContext().entityContext().attach(getEntityHandler(), status);
    }
  }

  protected abstract Object getNavigationPropertyValue(final NavigationProperty property, final Method getter);

  protected Object retrieveNavigationProperty(final NavigationProperty property, final Method getter) {
    final Class<?> type = getter.getReturnType();
    final Class<?> collItemType;
    if (EntityCollection.class.isAssignableFrom(type)) {
      final Type[] eCollParams = ((ParameterizedType) type.getGenericInterfaces()[0]).getActualTypeArguments();
      collItemType = (Class<?>) eCollParams[0];
    } else {
      collItemType = type;
    }

    final Object navPropValue;

    URI targetEntitySetURI = CoreUtils.getTargetEntitySetURI(getClient(), property);
    final ODataLink link = ((ODataLinked) internal).getNavigationLink(property.name());

    if (link instanceof ODataInlineEntity) {
      // return entity
      navPropValue = getEntityProxy(
              ((ODataInlineEntity) link).getEntity(),
              targetEntitySetURI,
              type,
              null,
              false);
    } else if (link instanceof ODataInlineEntitySet) {
      // return entity set
      navPropValue = getEntityCollectionProxy(
              collItemType,
              type,
              targetEntitySetURI,
              ((ODataInlineEntitySet) link).getEntitySet(),
              targetEntitySetURI,
              false);
    } else {
      // navigate
      final URI targetURI = URIUtils.getURI(getEntityHandler().getEntityURI(), property.name());

      if (EntityCollection.class.isAssignableFrom(type)) {
        navPropValue = getEntityCollectionProxy(
                collItemType,
                type,
                targetEntitySetURI,
                null,
                targetURI,
                true);
      } else if (AbstractEntitySet.class.isAssignableFrom(type)) {
        navPropValue = getEntitySetProxy(type, targetURI); // cannot be used standard target entity set URI
      } else {
        final EntityUUID uuid = new EntityUUID(targetEntitySetURI, collItemType, null);
        LOG.debug("Ask for '{}({})'", collItemType.getSimpleName(), null);

        EntityInvocationHandler handler = getContext().entityContext().getEntity(uuid);

        if (handler == null) {
          final CommonODataEntity entity = getClient().getObjectFactory().newEntity(new FullQualifiedName(
                  collItemType.getAnnotation(Namespace.class).value(), ClassUtils.getEntityTypeName(collItemType)));

          handler = EntityInvocationHandler.getInstance(
                  entity,
                  URIUtils.getURI(this.uri.build(), property.name()),
                  targetEntitySetURI,
                  collItemType,
                  service);

        } else if (getContext().entityContext().getStatus(handler) == AttachedEntityStatus.DELETED) {
          // object deleted
          LOG.debug("Object '{}({})' has been deleted", collItemType.getSimpleName(), uuid);
          handler = null;
        }

        navPropValue = handler == null ? null : Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[] {collItemType},
                handler);
      }
    }

    return navPropValue;
  }

  public Object getAdditionalProperty(final String name) {
    return getPropertyValue(name, null);
  }

  public abstract Collection<String> getAdditionalPropertyNames();

  private void setNavigationPropertyValue(final NavigationProperty property, final Object value) {
    // 1) attach source entity
    if (!getContext().entityContext().isAttached(getEntityHandler())) {
      getContext().entityContext().attach(getEntityHandler(), AttachedEntityStatus.CHANGED);
    }

    // 2) add links
    addLinkChanges(property, value);
  }

  public Map<String, AnnotatableInvocationHandler> getPropAnnotatableHandlers() {
    return propAnnotatableHandlers;
  }

  public void putPropAnnotatableHandler(final String propName, final AnnotatableInvocationHandler handler) {
    propAnnotatableHandlers.put(propName, handler);
  }

  public Map<String, AnnotatableInvocationHandler> getNavPropAnnotatableHandlers() {
    return navPropAnnotatableHandlers;
  }

  public void putNavPropAnnotatableHandler(final String navPropName, final AnnotatableInvocationHandler handler) {
    navPropAnnotatableHandlers.put(navPropName, handler);
  }

  protected abstract void load();

  protected abstract void setPropertyValue(final Property property, final Object value);

  protected abstract void addLinkChanges(final NavigationProperty navProp, final Object value);

  protected abstract Object getPropertyValue(final String name, final Type type);

  public abstract void addAdditionalProperty(final String name, final Object value);

  public abstract void removeAdditionalProperty(final String name);

  public abstract boolean isChanged();
}
