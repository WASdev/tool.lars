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

import java.util.Collection;

import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;

/**
 * This is an extension to the {@link RepositoryResourceMatchingData} to hold a provide feature, version and applies to filter info fields.
 */
public class ExtendedMatchingData extends RepositoryResourceMatchingData {
    String provideFeature;
    String version;
    Collection<AppliesToFilterInfo> atfi;

    /**
     * @return the provideFeature
     */
    public String getProvideFeature() {
        return provideFeature;
    }

    /**
     * @param provideFeature the provideFeature to set
     */
    public void setProvideFeature(String provideFeature) {
        this.provideFeature = provideFeature;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the atfi
     */
    public Collection<AppliesToFilterInfo> getAtfi() {
        return atfi;
    }

    /**
     * @param atfi the atfi to set
     */
    public void setAtfi(Collection<AppliesToFilterInfo> atfi) {
        this.atfi = atfi;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((provideFeature == null) ? 0 : provideFeature.hashCode());
        result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ExtendedMatchingData other = (ExtendedMatchingData) obj;
        if (provideFeature == null) {
            if (other.provideFeature != null)
                return false;
        } else if (!provideFeature.equals(other.provideFeature))
            return false;
        if (getType() != other.getType())
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;

        // atfi is a list, equals will say they are different if they are in a different order but same contents. We 
        // want to treat them as being the same so do logic here instead of delegating to list.
        // Check appliesToFilterInfo
        Collection<AppliesToFilterInfo> appliesTo = other.getAtfi();
        if (appliesTo == null) {
            if (atfi != null)
                return false;
        } else {
            if (atfi == null)
                return false;
        }
        if (atfi != null) {
            if (appliesTo.size() != atfi.size())
                return false;
            for (AppliesToFilterInfo a : appliesTo) {
                if (!atfi.contains(a))
                    return false;
            }
        }

        return true;
    }
}
