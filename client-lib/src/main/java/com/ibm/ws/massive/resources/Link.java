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

package com.ibm.ws.massive.resources;

import java.util.Collection;

public class Link {

    private final com.ibm.ws.massive.sa.client.model.Link _link;

    public Link() {
        _link = new com.ibm.ws.massive.sa.client.model.Link();
    }

    public Link(com.ibm.ws.massive.sa.client.model.Link link) {
        _link = link;
    }

    public Collection<String> getQuery() {
        return _link.getQuery();
    }

    public void setQuery(Collection<String> query) {
        _link.setQuery(query);
    }

    public String getLabel() {
        return _link.getLabel();
    }

    public void setLabel(String label) {
        _link.setLabel(label);
    }

    public String getLinkLabelProperty() {
        return _link.getLinkLabelProperty();
    }

    public void setLinkLabelProperty(String linklabel) {
        _link.setLinkLabelProperty(linklabel);
    }

    public void setLinkLabelPrefix(String linkLabelPrefix) {
        _link.setLinkLabelPrefix(linkLabelPrefix);
    }

    public String getLinkLabelPrefix() {
        return _link.getLinkLabelPrefix();
    }

    public void setLinkLabelSuffix(String linkLabelSuffix) {
        _link.setLinkLabelSuffix(linkLabelSuffix);
    }

    public String getLinkLabelSuffix() {
        return _link.getLinkLabelSuffix();
    }

    com.ibm.ws.massive.sa.client.model.Link getLink() {
        return _link;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                 + ((_link == null) ? 0 : _link.hashCode());
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
        Link other = (Link) obj;
        if (_link == null) {
            if (other._link != null)
                return false;
        } else if (!_link.equals(other._link))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return _link.toString();
    }

}
