/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.ibm.ws.lars.rest.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.ws.lars.rest.InvalidJsonAssetException;
import com.ibm.ws.lars.rest.RepositoryException;

/**
 * This class represents any asset that can be stored in the asset store as JSON. It can be
 * serialised/deserialised to/from JSON.
 *
 * The most important member is properties, which is a Map which corresponds to the JSON contents.
 * Keys should be Strings and values can be any immutable object that can itself be represented as
 * JSON. Note that it's really important that these value objects be immutable: we don't want anyone
 * reaching into our JSON objects and changing them.
 */
public abstract class RepositoryObject {

    public static final String _ID = "_id";

    protected final Map<String, Object> properties;

    private static ObjectMapper reader = new ObjectMapper();
    private static ObjectMapper writer = new ObjectMapper();

    protected RepositoryObject() {
        this(new HashMap<String, Object>());
    }

    /**
     * Construct a new repository object that is a clone of the specified one. We define this as a
     * constructor rather than implement Cloneable because Josh Bloch told us that Cloneable is
     * broken and we'd rather have control of the process.
     */
    protected RepositoryObject(RepositoryObject clone) {
        this(new HashMap<>(clone.getProperties()));
    }

    /**
     * Wrap an existing map of properties in a repository object. Changes to the object will be
     * reflected in the original map.
     *
     * @param properties the map of properties
     */
    protected RepositoryObject(Map<String, Object> properties) {
        this.properties = properties;
    }

    protected static Map<String, Object> readJsonState(String json) throws InvalidJsonAssetException {
        try {
            return reader.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonParseException e) {
            throw new InvalidJsonAssetException(e);
        } catch (JsonMappingException e) {
            throw new InvalidJsonAssetException(e);
        } catch (IOException e) {
            // No idea what would cause this.
            throw new InvalidJsonAssetException(e);
        }
    }

    protected static Map<String, Object> readJsonState(byte[] json) throws InvalidJsonAssetException {
        try {
            return reader.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonParseException e) {
            throw new InvalidJsonAssetException(e);
        } catch (JsonMappingException e) {
            throw new InvalidJsonAssetException(e);
        } catch (IOException e) {
            // No idea what would cause this.
            throw new InvalidJsonAssetException(e);
        }
    }

    protected static Map<String, Object> readJsonState(InputStream json) throws InvalidJsonAssetException {
        try {
            return reader.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonParseException e) {
            throw new InvalidJsonAssetException(e);
        } catch (JsonMappingException e) {
            throw new InvalidJsonAssetException(e);
        } catch (IOException e) {
            // No idea what would cause this.
            throw new InvalidJsonAssetException(e);
        }
    }

    public String toJson() {
        try {
            return writer.writeValueAsString(this.properties);
        } catch (JsonProcessingException e) {
            throw new RepositoryException("Couldn't serialize JSON object from repository", e);
        }
    }

    public void writeJSONToStream(OutputStream output) throws IOException {
        writer.writeValue(output, this.properties);
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Object o) {
        if (o == null) {
            return null;
        } else {
            return (T) o;
        }
    }

    /**
     *
     */
    public <T> T get(String field) {
        return cast(properties.get(field));
    }

    public <T> void put(String field, T value) {
        properties.put(field, value);
    }

    /**
     * This basically only exists for testing purposes. Application code should not call this
     * method.
     */
    public void setProperty(String field, String value) {
        put(field, value);
    }

    /**
     * This basically only exists for testing purposes. Application code should not call this
     * method.
     */
    public Object getProperty(String field) {
        return get(field);
    }

    /**
     * Returns the state of this object *without* copying it - so clients should not update returned
     * state map.
     */
    public Map<String, Object> getProperties() {
        return this.properties;
    }

    public void set_id(String _id) {
        put(_ID, _id);
    }

    public String get_id() {
        return get(_ID);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RepositoryObject other = (RepositoryObject) obj;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return toJson();
    }

}
