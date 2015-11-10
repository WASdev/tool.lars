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

import java.util.Collection;

/**
 *
 */
public class Link {

    Collection<String> query;
    private String label;
    private String linkLabelProperty;
    private String linkLabelPrefix;
    private String linkLabelSuffix;

    /**
     * @return the query
     */
    public Collection<String> getQuery() {
        return query;
    }

    /**
     * @param query the query to set
     */
    public void setQuery(Collection<String> query) {
        this.query = query;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the linkLabelProperty
     */
    public String getLinkLabelProperty() {
        return linkLabelProperty;
    }

    /**
     * @param linkLabelProperty the linkLabelProperty to set
     */
    public void setLinkLabelProperty(String linkLabelProperty) {
        this.linkLabelProperty = linkLabelProperty;
    }

    public void setLinkLabelPrefix(String linkLabelPrefix) {
        this.linkLabelPrefix = linkLabelPrefix;
    }

    public String getLinkLabelPrefix() {
        return this.linkLabelPrefix;
    }

    public void setLinkLabelSuffix(String linkLabelSuffix) {
        this.linkLabelSuffix = linkLabelSuffix;
    }

    public String getLinkLabelSuffix() {
        return this.linkLabelSuffix;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((linkLabelProperty == null) ? 0 : linkLabelProperty.hashCode());
        result = prime * result + ((query == null) ? 0 : query.hashCode());
        result = prime * result + ((linkLabelPrefix == null) ? 0 : linkLabelPrefix.hashCode());
        result = prime * result + ((linkLabelSuffix == null) ? 0 : linkLabelSuffix.hashCode());
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
        Link other = (Link) obj;
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        if (linkLabelProperty == null) {
            if (other.linkLabelProperty != null)
                return false;
        } else if (!linkLabelProperty.equals(other.linkLabelProperty))
            return false;
        if (query == null) {
            if (other.query != null)
                return false;
        } else if (!query.equals(other.query)) {
            return false;
        } else if (linkLabelPrefix == null) {
            if (other.linkLabelPrefix != null) {
                return false;
            }
        } else if (!linkLabelPrefix.equals(other.linkLabelPrefix)) {
            return false;
        } else if (linkLabelSuffix == null) {
            if (other.linkLabelSuffix != null) {
                return false;
            }
        } else if (!linkLabelSuffix.equals(other.linkLabelSuffix)) {
            return false;
        }
        return true;
    }
}
