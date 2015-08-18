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
 * An immutable object which represents something to match a field against in a filter
 * <p>
 * E.g. a filter may represent the concept of "equal to 3"
 * <p>
 * A field will be paired with a condition to make a filter like "foo=3" or "bar!=sandwich"
 */
public class Condition {

    public enum Operation {
        EQUALS,
        NOT_EQUALS
    }

    private final Operation operation;
    private final String value;

    /**
     * Create a condition with the given operation and value
     *
     * @param operation
     * @param value
     */
    public Condition(Operation operation, String value) {
        this.operation = operation;
        this.value = value;
    }

    /**
     * @return the operation
     */
    public Operation getOperation() {
        return operation;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((operation == null) ? 0 : operation.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        Condition other = (Condition) obj;
        if (operation != other.operation)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        String sign = null;

        switch (operation) {
            case EQUALS:
                sign = "=";
                break;
            case NOT_EQUALS:
                sign = "!=";
                break;
        }

        return sign + " " + value;
    }

}
