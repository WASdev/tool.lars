/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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

import java.util.Collections;
import java.util.List;

/**
 * A filter for assets returned from the persistence layer. The key of the filter should be a field
 * specification as per the mongo query syntax. So a key of 'foo.bar' would refer to a document
 * containing a field named 'foo' where 'foo' is an object containing a field name 'bar'
 * <p>
 * A filter will match an asset where the field indicated by a key is a match for <b>any</b> of the
 * conditions in the corresponding list.
 * <p>
 *
 */

public class AssetFilter {

    public AssetFilter(String key, List<Condition> conditions) {
        this.key = key;
        this.conditions = Collections.unmodifiableList(conditions);
    }

    private final String key;

    public String getKey() {
        return key;
    }

    private final List<Condition> conditions;

    public List<Condition> getConditions() {
        return conditions;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((conditions == null) ? 0 : conditions.hashCode());
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AssetFilter other = (AssetFilter) obj;
        if (conditions == null) {
            if (other.conditions != null)
                return false;
        } else if (!conditions.equals(other.conditions))
            return false;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        return true;
    }

}
