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

package com.ibm.ws.massive.sa.client;

import java.net.URL;

/**
 *
 */
public class LoginInfoClientProxy {

    URL proxyURL;
    String proxyUserid;
    String plainProxyPassword;

    /**
     *
     * @param proxyURL - the proxy url and port
     * @param proxyuserid
     * @param encryptedProxyPassword - encrypted using xor or aes
     */
    public LoginInfoClientProxy(URL proxyURL, String proxyUserid, String plainProxyPassword) {
        this.proxyURL = proxyURL;
        this.proxyUserid = proxyUserid;
        this.plainProxyPassword = plainProxyPassword;
    }

    /**
     * @return the proxyURL
     */
    public URL getProxyURL() {
        return proxyURL;
    }

    /**
     * @return the proxyUserid
     */
    public String getProxyUserid() {
        return proxyUserid;
    }

    /**
     * @return the encryptedProxyPassword
     */
    public String getProxyPassword() {
        return plainProxyPassword;
    }

    /**
     * Is this an http/https proxy ?
     *
     * @return boolean - whether the proxyURL is http/https
     */
    public boolean isHTTP() {
        if (proxyURL.getProtocol().equalsIgnoreCase("HTTP") ||
            proxyURL.getProtocol().equalsIgnoreCase("HTTPS")) {
            return true;
        } else {
            return false;
        }
    }
}
