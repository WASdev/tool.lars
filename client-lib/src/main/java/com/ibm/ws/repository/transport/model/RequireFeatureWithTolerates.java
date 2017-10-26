/*******************************************************************************
* Copyright (c) 2017 IBM Corp.
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
package com.ibm.ws.repository.transport.model;

import java.util.Collection;
import java.util.HashSet;

public class RequireFeatureWithTolerates {

    private String feature;
    private Collection<String> tolerates;

    public String getFeature() {
        return this.feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public Collection<String> getTolerates() {
        return this.tolerates;
    }

    public void setTolerates(Collection<String> tolerates) {
        this.tolerates = tolerates;
    }

    public void addTolerates(String tolerate) {
        if (this.tolerates == null) {
            this.tolerates = new HashSet<String>();
        }
        tolerates.add(tolerate);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((feature == null) ? 0 : feature.hashCode());
        result = prime * result + ((tolerates == null) ? 0 : tolerates.hashCode());
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
        RequireFeatureWithTolerates other = (RequireFeatureWithTolerates) obj;
        if (feature == null) {
            if (other.feature != null)
                return false;
        } else if (!feature.equals(other.feature))
            return false;

        if (tolerates == null) {
            if (other.tolerates != null)
                return false;
        } else if (!tolerates.equals(other.tolerates))
            return false;
        return true;
    }

}
