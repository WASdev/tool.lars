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
package com.ibm.ws.repository.transport.model;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

public class FilterVersion {
    String value;
    boolean inclusive;
    String label;
    String compatibilityLabel;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean getInclusive() {
        return inclusive;
    }

    public void setInclusive(boolean inclusive) {
        this.inclusive = inclusive;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (inclusive ? 1231 : 1237);
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        FilterVersion other = (FilterVersion) obj;
        if (inclusive != other.inclusive)
            return false;
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        if (compatibilityLabel == null) {
            if (other.compatibilityLabel != null)
                return false;
        } else if (!compatibilityLabel.equals(other.compatibilityLabel))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the compatibilityLabel
     */
    public String getCompatibilityLabel() {
        return compatibilityLabel;
    }

    /**
     * @param compatibilityLabel the compatibilityLabel to set
     */
    public void setCompatibilityLabel(String compatibilityLabel) {
        this.compatibilityLabel = compatibilityLabel;
    }

    /**
     * This method creates a version range from the supplied min and max FilterVersion
     * 
     * @param minVersion The min version in the range. Can be null, if so treated as {@link Version#emptyVersion}
     * @param maxVersion The max version. Can be null, null in a version range is treated as infinite
     * @return A version range object representing the min and max values supplied
     */
    public static VersionRange getFilterRange(FilterVersion minVersion, FilterVersion maxVersion) {
        VersionRange vr = null;
        Version vmin = minVersion == null ? Version.emptyVersion : new Version(minVersion.getValue());
        Version vmax = maxVersion == null ? null : new Version(maxVersion.getValue());
        char leftType = (minVersion == null || minVersion.getInclusive()) ? VersionRange.LEFT_CLOSED : VersionRange.LEFT_OPEN;
        char rightType = (maxVersion == null || maxVersion.getInclusive()) ? VersionRange.RIGHT_CLOSED : VersionRange.RIGHT_OPEN;
        vr = new VersionRange(leftType, vmin, vmax, rightType);
        return vr;
    }
}
