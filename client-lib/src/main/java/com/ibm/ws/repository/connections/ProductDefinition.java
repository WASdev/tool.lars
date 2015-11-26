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

package com.ibm.ws.repository.connections;

/**
 * These objects define information about an installed product runtime and specify what resolved resources will install into.
 */
public interface ProductDefinition {

    /**
     * @return the ID of the product
     */
    public String getId();

    /**
     * @return the version of the product
     */
    public String getVersion();

    /**
     * @return the install type of the product
     */
    public String getInstallType();

    /**
     * @return the license type of the product
     */
    public String getLicenseType();

    /**
     * @return the edition of the product
     */
    public String getEdition();

}
