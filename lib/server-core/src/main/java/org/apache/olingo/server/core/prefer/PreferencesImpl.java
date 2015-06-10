/*
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
 */
package org.apache.olingo.server.core.prefer;

import java.net.URI;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import org.apache.olingo.server.api.prefer.Preferences;

/**
 * Provides access methods to the preferences set in the Prefer HTTP request
 * header as described in <a href="https://www.ietf.org/rfc/rfc7240.txt">RFC 7240</a>.
 * Preferences defined in the OData standard can be accessed with named methods.
 */
public class PreferencesImpl implements Preferences {

  private static final String ALLOW_ENTITY_REFERENCES = "odata.allow-entityreferences";
  private static final String CALLBACK = "odata.callback";
  private static final String CONTINUE_ON_ERROR = "odata.continue-on-error";
  // private static final String INCLUDE_ANNOTATIONS = "odata.include-annotations";
  private static final String MAX_PAGE_SIZE = "odata.maxpagesize";
  private static final String TRACK_CHANGES = "odata.track-changes";
  private static final String RETURN = "return";
  private static final String RESPOND_ASYNC = "respond-async";
  private static final String WAIT = "wait";

  private static final String URL = "url"; // parameter name for odata.callback

  private final Map<String, Preference> preferences;

  public PreferencesImpl(final Collection<String> preferHeaders) {
    preferences = PreferParser.parse(preferHeaders);
  }

  public Preference getPreference(final String name) {
    return preferences.get(name.toLowerCase(Locale.ROOT));
  }

  public boolean hasAllowEntityReferences() {
    return preferences.containsKey(ALLOW_ENTITY_REFERENCES);
  }

  public URI getCallback() {
    if (preferences.containsKey(CALLBACK)
        && preferences.get(CALLBACK).getParameters() != null
        && preferences.get(CALLBACK).getParameters().get(URL) != null) {
      try {
        return URI.create(preferences.get(CALLBACK).getParameters().get(URL));
      } catch (final IllegalArgumentException e) {
        return null;
      }
    }
    return null;
  }

  public boolean hasContinueOnError() {
    return preferences.containsKey(CONTINUE_ON_ERROR);
  }

  public Integer getMaxPageSize() {
    return getNonNegativeIntegerPreference(MAX_PAGE_SIZE);
  }

  public boolean hasTrackChanges() {
    return preferences.containsKey(TRACK_CHANGES);
  }

  public Return getReturn() {
    if (preferences.containsKey(RETURN)) {
      final String value = preferences.get(RETURN).getValue();
      if (Return.REPRESENTATION.toString().toLowerCase(Locale.ROOT).equals(value)) {
        return Return.REPRESENTATION;
      } else if (Return.MINIMAL.toString().toLowerCase(Locale.ROOT).equals(value)) {
        return Return.MINIMAL;
      }
    }
    return null;
  }

  public boolean hasRespondAsync() {
    return preferences.containsKey(RESPOND_ASYNC);
  }

  public Integer getWait() {
    return getNonNegativeIntegerPreference(WAIT);
  }

  private Integer getNonNegativeIntegerPreference(final String name) {
    if (preferences.containsKey(name) && preferences.get(name).getValue() != null) {
      try {
        final Integer result = Integer.valueOf(preferences.get(name).getValue());
        return result < 0 ? null : result;
      } catch (final NumberFormatException e) {
        return null;
      }
    }
    return null;
  }
}
