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
package com.ibm.ws.repository.transport.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.ibm.ws.repository.common.enums.AttachmentLinkType;
import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.utils.internal.RepositoryCommonUtils;
import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Attachment;
import com.ibm.ws.repository.transport.model.WlpInformation;

/**
 *
 */
public abstract class AbstractFileClient extends AbstractRepositoryClient implements RepositoryReadableClient {

    /*
     * This header is used with in products (JARs) to record the location of
     * License Agreement files within the archive.
     */
    public final static String LA_HEADER_PRODUCT = "License-Agreement";

    /*
     * This header is used in products (JARs) to record the location of License
     * Information files within the archive.
     */
    public final static String LI_HEADER_PRODUCT = "License-Information";

    /* Header used in features (ESAs) to record License Agreement location */
    public final static String LA_HEADER_FEATURE = "IBM-License-Agreement";

    /* Header used in features (ESAs) to record License Information location */
    public final static String LI_HEADER_FEATURE = "IBM-License-Information";

    /*
     * ------------------------------------------------------------------------------------------------------------------
     * PUBLIC METHODS OVERRIDEN FROM INTERFACE
     * ------------------------------------------------------------------------------------------------------------------
     */

    /**
     * {@inheritDoc}
     *
     * @throws RequestFailureException
     * @throws IOException
     */
    @Override
    public List<Asset> getAllAssets() throws IOException, RequestFailureException {
        return readAssetsRelative("");
    }

    /** {@inheritDoc} */
    @Override
    public Asset getAsset(final String assetId) throws IOException, BadVersionException, RequestFailureException {
        return getAsset(assetId, true);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<Asset> getFilteredAssets(final Map<FilterableAttribute, Collection<String>> filters) throws IOException, RequestFailureException {
        // Were any filters defined?
        if (filters == null || allFiltersAreEmpty(filters)) {
            return getAllAssets();
        }

        Collection<Asset> allAssets = getAllAssets();
        Collection<Asset> filtered = new ArrayList<Asset>();

        assetLoop: for (Asset asset : allAssets) {
            filterAttribLoop: for (Entry<FilterableAttribute, Collection<String>> entry : filters.entrySet()) {
                FilterableAttribute attrib = entry.getKey();
                // List of values we are looking for
                Collection<String> values = entry.getValue();
                // List of values in the asset
                Collection<String> assetValues = getValues(attrib, asset);

                if (values != null && values.size() != 0) {
                    // Check each required value and see if the asset has it
                    for (String filterValue : values) {
                        // if we find a match stop checking this attribute and move to next attribute
                        if (assetValues.contains(filterValue)) {
                            continue filterAttribLoop;
                        }
                    }
                    // We never found a match for this attribute so move to next asset
                    continue assetLoop;
                }
            }
            filtered.add(asset);
        }

        return filtered;
    }

    @Override
    public List<Asset> findAssets(final String searchString, final Collection<ResourceType> types) throws IOException, RequestFailureException {
        Collection<Asset> assets = getAssets(types, null, null, null);
        List<Asset> foundAssets = new ArrayList<Asset>();
        for (Asset ass : assets) {
            if ((ass.getName() != null && ass.getName().contains(searchString))
                || (ass.getDescription() != null && ass.getDescription().contains(searchString))
                || (ass.getShortDescription() != null && ass.getShortDescription().contains(searchString))) {
                foundAssets.add(ass);
            }
        }
        return foundAssets;
    }

    /*
     * ------------------------------------------------------------------------------------------------------------------
     * PROTECTED AND OVERRIDEABLE IMPLEMENTATION METHODS
     * ------------------------------------------------------------------------------------------------------------------
     */

    protected Asset processJSON(final InputStream jsonInputStream) throws FileNotFoundException, IOException, BadVersionException {
        // id is the file path from the root.
        Asset ass = JSONAssetConverter.readValue(jsonInputStream);

        // TODO: Should we confirm the asset is in the right location. For example a sample might be in the "blah" directory?
        return ass;
    }

    /**
     * Gets the specified asset
     *
     * @param assetId The asset id to get
     * @param includeAttachments Flag to specify if the attachments should be read as well.
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws BadVersionException
     */
    protected Asset getAsset(final String assetId, final boolean includeAttachments) throws FileNotFoundException, IOException, BadVersionException {
        Asset ass = readJson(assetId);
        ass.set_id(assetId);

        // We always get a wlp info when read back from Massive so create one if there isnt already one
        WlpInformation wlpInfo = ass.getWlpInformation();
        if (wlpInfo == null) {
            wlpInfo = new WlpInformation();
            ass.setWlpInformation(wlpInfo);
        }
        if (wlpInfo.getAppliesToFilterInfo() == null) {
            wlpInfo.setAppliesToFilterInfo(Collections.<AppliesToFilterInfo> emptyList());
        }

        if (includeAttachments) {
            if (exists(assetId)) {

                Attachment at = new Attachment();
                at.set_id(assetId);
                at.setLinkType(AttachmentLinkType.DIRECT);
                at.setType(AttachmentType.CONTENT);
                at.setName(getName(assetId));
                at.setSize(getSize(assetId));
                at.setUrl(at.get_id());
                ass.addAttachement(at);
                if (assetId.toLowerCase().endsWith(".jar") || assetId.toLowerCase().endsWith(".esa")) {
                    // Create attachment objects for each license
                    if (hasLicenses(assetId)) {
                        Map<String, Long> licensesMap = getLicenses(assetId);
                        for (Map.Entry<String, Long> e : licensesMap.entrySet()) {
                            String lic = e.getKey();
                            String name = getName(lic);
                            String licId = assetId.concat(String.format("#licenses" + File.separator + "%s", name));
                            Attachment licAt = new Attachment();
                            licAt.set_id(licId);
                            licAt.setLinkType(AttachmentLinkType.DIRECT);
                            licAt.setName(name);
                            licAt.setSize(e.getValue());
                            String locString = name.substring(3);
                            if (name.startsWith("LI")) {
                                licAt.setType(AttachmentType.LICENSE_INFORMATION);
                            } else if (name.startsWith("LA")) {
                                licAt.setType(AttachmentType.LICENSE_AGREEMENT);
                            } else if (name.endsWith(".html")) {
                                licAt.setType(AttachmentType.LICENSE);
                                locString = name.substring(0, name.lastIndexOf("."));
                            }
                            // Make sure we've found one
                            if (licAt.getType() != null) {
                                Locale locale = RepositoryCommonUtils.localeForString(locString);
                                licAt.setLocale(locale);
                                ass.addAttachement(licAt);
                                licAt.setUrl(licAt.get_id());
                            }
                        }
                    }
                }
            }
        }

        return ass;
    }

    /**
     * Reads all assets under the specified location (so it can read a sub set of the repo).
     *
     * @param relative
     * @return
     * @throws IOException
     * @throws RequestFailureException
     */
    protected List<Asset> readAssetsRelative(final String relative) throws IOException, RequestFailureException {
        Collection<String> relativePaths = getChildren(relative);
        List<Asset> results = new ArrayList<Asset>();

        for (String s : relativePaths) {
            if (isJson(s)) {
                try {
                    Asset ass = getAsset(getAssetIdFromJson(s), false);
                    results.add(ass);
                } catch (BadVersionException e) {
                    // Ignore assets with unknown versions
                    continue;
                }
            }
        }

        return results;
    }

    protected String getAssetIdFromJson(final String json) {
        return json.substring(0, json.length() - 5);
    }

    protected boolean hasLicenses(final String assetId) throws IOException {
        return ((getHeader(assetId, LA_HEADER_FEATURE) != null) ||
                (getHeader(assetId, LA_HEADER_PRODUCT) != null) ||
                (getHeader(assetId, LI_HEADER_FEATURE) != null) || (getHeader(assetId, LI_HEADER_PRODUCT) != null));
    }

    /**
     * Gets the name from the relative path of the asset
     *
     * @param relative
     * @return
     */
    protected String getName(final String relative) {
        return relative.substring(relative.lastIndexOf(File.separator) + 1);
    }

    protected boolean isJson(final String relative) {
        return relative.endsWith(".json");
    }

    /**
     * Given a ZipInputStream to an asset within the repo get an input stream to the license attachment within the
     * asset.
     *
     * @param zis ZipInputStream to the container for the asset (i.e. ZipInputStream to an ESA file inside a repo).
     * @param assetId The id of the asset
     * @param attachmentId The id of the license to get an input stream to.
     * @return
     * @throws IOException
     */
    protected InputStream getInputStreamToLicenseInsideZip(final ZipInputStream zis, final String assetId, final String attachmentId) throws IOException {
        InputStream is = null;
        try {
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                if (ze.isDirectory()) {
                    //do nothing
                } else {
                    String name = getName(ze.getName().replace("/", File.separator));
                    String licId = assetId.concat(String.format("#licenses" + File.separator + "%s", name));
                    if (licId.equals(attachmentId)) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int read;
                        int total = 0;
                        while ((read = zis.read(buffer)) != -1) {
                            baos.write(buffer, 0, read);
                            total += read;
                        }
                        //ZipEntry sometimes returns -1 when unable to get original size, in this case we want to proceed
                        if (ze.getSize() != -1 && total != ze.getSize()) {
                            throw new IOException("The size of the retrieved license was wrong. Expected : " + ze.getSize()
                                                  + " bytes, but actually was " + total + " bytes.");
                        }

                        byte[] content = baos.toByteArray();
                        is = new ByteArrayInputStream(content);
                    }
                }
                ze = zis.getNextEntry();
            }
        } finally {
            zis.closeEntry();
            zis.close();
        }

        return is;

    }

    /**
     * Get the specified header from the manifest for the specified asset
     *
     * @param assetId The asset id to look at
     * @param type The header field to look for
     * @return
     * @throws IOException
     */
    protected abstract String getHeader(String assetId, String type) throws IOException;

    /**
     * Gets a map of licenses for the specified asset along with the license file size
     *
     * @param assetId Asset id to get the licenses for
     * @return
     * @throws IOException
     */
    protected abstract Map<String, Long> getLicenses(final String assetId) throws IOException;

    /**
     * Read the json for the specified asset and create an asset from it
     *
     * @param assetId The asset id to read
     * @return
     * @throws IOException
     * @throws BadVersionException
     */
    protected abstract Asset readJson(final String assetId) throws IOException, BadVersionException;

    /**
     * Checks if the specified path exists in the repo
     *
     * @param relative The path to look for
     * @return
     */
    protected abstract boolean exists(final String relative);

    /**
     * Checks if there are assets under this relative path in the repo
     *
     * @param relative The path to check
     * @return
     * @throws IOException
     */
    protected abstract boolean hasChildren(final String relative) throws IOException;

    /**
     * Gets the child assets under the specified path. This gets all children in sub directories too
     *
     * @param relative
     * @return
     * @throws IOException
     */
    protected abstract Collection<String> getChildren(final String relative) throws IOException;

    /**
     * Gets the size of the file specified, this could be any attachment or even the size of the json itself.
     *
     * @param relative Gets the size of the file specified
     * @return
     */
    protected abstract long getSize(final String relative);
}
