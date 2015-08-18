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
 * Stores the options for pagination parsed from the request URI.
 * <p>
 * Generally, a method which returns multiple results in a predictable order should accept a
 * PaginationsOptions as a parameter.
 */
public class PaginationOptions {

    private final int offset;
    private final int limit;

    /**
     * Create a new PaginationOptions with the given offset and limit parameters.
     *
     * @param offset
     * @param limit
     */
    public PaginationOptions(int offset, int limit) {
        super();
        this.offset = offset;
        this.limit = limit;
    }

    /**
     * @return the offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "limit = " + limit + ", offset = " + offset;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + limit;
        result = prime * result + offset;
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
        PaginationOptions other = (PaginationOptions) obj;
        if (limit != other.limit)
            return false;
        if (offset != other.offset)
            return false;
        return true;
    }

}
