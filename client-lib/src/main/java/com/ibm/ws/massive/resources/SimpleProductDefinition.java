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

/**
 * An immutable POJO implementation of {@link ProductDefinition}.
 */
public class SimpleProductDefinition implements ProductDefinition {

    private final String installType;
    private final String id;
    private final String version;
    private final String licenseType;
    private final String edition;

    /**
     * @param id the ID of the product
     * @param version the version of the product
     * @param installType the install type of the product
     * @param licenseType the licenseType of the product
     * @param edition the edition of the product
     */
    public SimpleProductDefinition(String id, String version, String installType, String licenseType, String edition) {
        super();
        this.installType = installType;
        this.id = id;
        this.version = version;
        this.licenseType = licenseType;
        this.edition = edition;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.massive.resolver.ProductDefinition#getId()
     */
    @Override
    public String getId() {
        return this.id;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.massive.resolver.ProductDefinition#getVersion()
     */
    @Override
    public String getVersion() {
        return this.version;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.massive.resolver.ProductDefinition#getInstallType()
     */
    @Override
    public String getInstallType() {
        return this.installType;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.massive.resolver.ProductDefinition#getLicenseType()
     */
    @Override
    public String getLicenseType() {
        return this.licenseType;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.massive.resolver.ProductDefinition#getEdition()
     */
    @Override
    public String getEdition() {
        return this.edition;
    }

}
