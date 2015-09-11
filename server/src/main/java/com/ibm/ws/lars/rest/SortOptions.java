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

/**
 * Parameters required to sort a set of results.
 * <p>
 * Results will be sorted according to the values in the field returned by {@link #getField()} in
 * the order indicated by {@link #getSortOrder()}.
 */
public class SortOptions {

    private final String field;
    private final SortOrder sortOrder;

    /**
     * Create a SortOptions with the given field and sort order.
     *
     * @param field the name of the field that should be used to sort the results
     * @param sortOrder the order that the results should be sorted (either ASCENDING or DESCENDING)
     */
    public SortOptions(String field, SortOrder sortOrder) {
        super();
        this.field = field;
        this.sortOrder = sortOrder;
    }

    /**
     * @return the name of the field that should be used to sort the results
     */
    public String getField() {
        return field;
    }

    /**
     * @return the order that the results should be sorted (either ASCENDING or DESCENDING)
     */
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((field == null) ? 0 : field.hashCode());
        result = prime * result + ((sortOrder == null) ? 0 : sortOrder.hashCode());
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
        SortOptions other = (SortOptions) obj;
        if (field == null) {
            if (other.field != null)
                return false;
        } else if (!field.equals(other.field))
            return false;
        if (sortOrder != other.sortOrder)
            return false;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Field: " + field + ", sortOrder: " + sortOrder;
    }

    /**
     * The order in which a result set should be sorted.
     */
    public static enum SortOrder {
        ASCENDING, DESCENDING
    }

}
