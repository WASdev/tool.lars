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
package com.ibm.ws.repository.resources.internal;

import com.ibm.ws.repository.common.enums.ResourceType;

/**
 *
 */
public class RepositoryResourceMatchingData {
    private String name;
    private String providerName;
    private ResourceType type;

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((providerName == null) ? 0 : providerName.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        RepositoryResourceMatchingData other = (RepositoryResourceMatchingData) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (providerName == null) {
            if (other.providerName != null)
                return false;
        } else if (!providerName.equals(other.providerName))
            return false;
        if (type != other.type)
            return false;
        return true;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the provider
     */
    public String getProviderName() {
        return providerName;
    }

    /**
     * @param provider the provider to set
     */
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    /**
     * @return the type
     */
    public ResourceType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(ResourceType type) {
        this.type = type;
    }
}
