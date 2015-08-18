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
package com.ibm.ws.lars.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

@SuppressWarnings("serial")
public class MultivaluedMapImpl<K, V> extends HashMap<K, List<V>> implements MultivaluedMap<K, V> {

    /** {@inheritDoc} */
    @Override
    public void add(K key, V value) {
        List<V> values = get(key);
        if (values == null) {
            values = new ArrayList<V>();
            put(key, values);
        }
        values.add(value);
    }

    /** {@inheritDoc} */
    @Override
    public V getFirst(K key) {
        V value = null;
        List<V> values = get(key);
        if (values != null && !values.isEmpty()) {
            value = values.get(0);
        }
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public void putSingle(K key, V value) {
        List<V> values = new ArrayList<V>();
        values.add(value);
        put(key, values);
    }
}