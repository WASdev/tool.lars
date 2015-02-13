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

import java.io.OutputStream;

public class Provider {

    private com.ibm.ws.massive.sa.client.model.Provider _provider;

    public Provider() {
        _provider = new com.ibm.ws.massive.sa.client.model.Provider();
    }

    public Provider(com.ibm.ws.massive.sa.client.model.Provider prov) {
        if (prov == null) {
            _provider = new com.ibm.ws.massive.sa.client.model.Provider();
        } else {
            _provider = prov;
        }
    }

    public String getName() {

        return _provider.getName();
    }

    public void setName(String name) {
        _provider.setName(name);
    }

    public String getUrl() {
        return _provider.getUrl();
    }

    public void setUrl(String url) {
        _provider.setUrl(url);
    }

    com.ibm.ws.massive.sa.client.model.Provider getProvider() {
        return _provider;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                 + ((_provider == null) ? 0 : _provider.hashCode());
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
        Provider other = (Provider) obj;
        if (_provider == null) {
            if (other._provider != null)
                return false;
        } else if (!_provider.equals(other._provider))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return _provider.toString();
    }

    public void dump(OutputStream os) {
        _provider.dump(os);
    }

}
