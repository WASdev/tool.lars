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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.massive.LoginInfo;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.RepositoryBackendException;
import com.ibm.ws.massive.RepositoryBackendIOException;
import com.ibm.ws.massive.RepositoryBackendRequestFailureException;
import com.ibm.ws.massive.RepositoryException;
import com.ibm.ws.massive.sa.client.MassiveClient;
import com.ibm.ws.massive.sa.client.RequestFailureException;
import com.ibm.ws.massive.sa.client.model.AppliesToFilterInfo;
import com.ibm.ws.massive.sa.client.model.Asset;
import com.ibm.ws.massive.sa.client.model.JavaSEVersionRequirements;
import com.ibm.ws.massive.sa.client.model.WlpInformation;

public class EsaResource extends MassiveResource implements WebDisplayable {

    /*
     * ------------------------------------------------------------------------------------------------
     * InstallPolicy Enum
     * ------------------------------------------------------------------------------------------------
     */
    /**
     * The install policy for an asset. Set to WHEN_SATISFIED if the ESA should be installed if all
     * its dependencies are met and MANUAL if this should not auto install.
     */
    public enum InstallPolicy {
        /**
         * The ESA should automatically install if it's provisioning capabilities are satisfied.
         */
        WHEN_SATISFIED(WlpInformation.InstallPolicy.WHEN_SATISFIED),
        /**
         * The ESA should not automatically install
         */
        MANUAL(WlpInformation.InstallPolicy.MANUAL);

        // Object for the enums to lock on while updating their lookup tables
        private static Object _enumLock = new Object();

        private final WlpInformation.InstallPolicy _policy;
        private static Map<WlpInformation.InstallPolicy, InstallPolicy> _lookup;

        /**
         * This method is used to obtain the MassiveResource.InstallPolicy given the WlpInformation.InstallPolicy value
         *
         * @param v The WlpInformation.InstallPolicy type which is contained within the asset
         * @return The MassiveResource.InstallPolicy enum value which maps to the supplied WlpInformation.InstallPolicy
         *         enum value
         */
        static InstallPolicy lookup(WlpInformation.InstallPolicy p) {
            synchronized (_enumLock) {
                // recheck in case someone else created before we got the lock
                if (null == _lookup) {
                    _lookup = new HashMap<WlpInformation.InstallPolicy, InstallPolicy>();
                    for (InstallPolicy policy : InstallPolicy.values()) {
                        _lookup.put(policy.getWlpInstallPolicy(), policy);
                    }
                }
            }
            return _lookup.get(p);
        }

        private InstallPolicy(WlpInformation.InstallPolicy p) {
            _policy = p;
        }

        WlpInformation.InstallPolicy getWlpInstallPolicy() {
            return _policy;
        }
    }

    /**
     * The attributes that you can filter an ESA resource by.
     */
    public enum FilterableAttribute {
        SYMBOLIC_NAME(MassiveClient.FilterableAttribute.SYMBOLIC_NAME),
        SHORT_NAME(MassiveClient.FilterableAttribute.SHORT_NAME),
        LOWER_CASE_SHORT_NAME(MassiveClient.FilterableAttribute.LOWER_CASE_SHORT_NAME);

        private final MassiveClient.FilterableAttribute clientFilterableAttribute;

        private FilterableAttribute(MassiveClient.FilterableAttribute clientFilterableAttribute) {
            this.clientFilterableAttribute = clientFilterableAttribute;
        }

        MassiveClient.FilterableAttribute getClientFilterableAttribute() {
            return clientFilterableAttribute;
        }
    }

    /**
     * ----------------------------------------------------------------------------------------------------
     * STATIC HELPER METHODS
     * ----------------------------------------------------------------------------------------------------
     */

    /**
     * This methods returns a list of all features in the supplied repository
     *
     * @param userId The userid to connect to massive with
     * @param password The password to connect to massive with
     * @param apiKey The apikey for the repository
     * @return A list of resources representing the features in massive
     * @throws RepositoryBackendException
     */
    public static Collection<EsaResource> getAllFeatures(LoginInfo loginInfo) throws RepositoryBackendException {
        return MassiveResource.getAllResources(Type.FEATURE, loginInfo);
    }

    /**
     * This method returns a Collection of all features in the repository
     * with the specified license type.
     *
     * @Param licenseType the type of license in question - IPLA, ILAN, etc
     * @param userId The userid to connect to massive with
     * @param password The password to connect to massive with
     * @param apiKey The apikey for the repository
     * @return A list of resources representing the features in massive
     *         with the requested license type
     * @throws RepositoryBackendException
     */
    public static Collection<EsaResource> getAllFeatures(LicenseType licenseType, LoginInfo loginInfo) throws RepositoryBackendException {
        return MassiveResource.getAllResources(licenseType, Type.FEATURE, loginInfo);
    }

    /**
     * Returns a specific feature from massive given the id of the asset
     *
     * @param userId The userid to connect to massive with
     * @param password The password to connect to massive with
     * @param apiKey The apikey for the repository
     * @param id The id of the feature to retrieve
     * @return A resource representing the feature in massive. Will return null if no asset is found
     * @throws RepositoryBackendException
     */
    public static EsaResource getEsa(LoginInfoEntry loginInfoResource, String id) throws RepositoryBackendException, RepositoryBadDataException {
        return getResource(loginInfoResource, id);
    }

    public static Collection<EsaResource> getMatchingEsas(LoginInfo loginInfo, ProductDefinition definition) throws RepositoryBackendException {
        @SuppressWarnings("unchecked")
        Collection<EsaResource> features = (Collection<EsaResource>) getResources(Collections.singleton(definition), Collections.singleton(Type.FEATURE), null, loginInfo).get(Type.FEATURE);
        if (features == null) {
            features = Collections.emptySet();
        }
        return features;
    }

    /**
     * Get all features in the repository that match the provided ProductDefinition (normally of the machine you are on)
     * and the supplied visibility setting.
     *
     * @param loginInfo
     * @param definition
     * @param visible
     * @return A collection of matching features
     * @throws RepositoryBackendException
     */
    public static Collection<EsaResource> getMatchingEsas(LoginInfo loginInfo, ProductDefinition definition, Visibility visible) throws RepositoryBackendException {
        @SuppressWarnings("unchecked")
        Collection<EsaResource> features = (Collection<EsaResource>) getResources(Collections.singleton(definition), Collections.singleton(Type.FEATURE), visible, loginInfo).get(Type.FEATURE);
        if (features == null) {
            features = Collections.emptySet();
        }
        return features;
    }

    /**
     * Find the features that match the supplied search string, ProductDefinition and Visibility
     *
     * @param searchString
     * @param loginInfo
     * @param definition
     * @param visible
     * @return a collection of matching feature resources
     * @throws RepositoryException
     */
    public static Collection<EsaResource> findMatchingEsas(String searchString, LoginInfo loginInfo, ProductDefinition definition, Visibility visible) throws RepositoryException {
        Collection<EsaResource> returnedEsas = new ArrayList<EsaResource>();

        Collection<EsaResource> esas = new ArrayList<EsaResource>();
        esas.addAll(MassiveResource.<EsaResource> findResources(searchString, loginInfo, TypeLabel.FEATURE, visible));
        for (EsaResource esa : esas) {

            // currently visibility is passed all the way through but the field isn't indexed so we need to deal
            // with it here.  ProductDefinition is not yet passed through so also has to be handled here.
            Visibility visibilityMatches = esa.getVisibility() == null ? Visibility.PUBLIC : esa.getVisibility();
            if ((visibilityMatches.equals(visible)) && (esa.matches(definition) == MatchResult.MATCHED)) {
                returnedEsas.add(esa);
            }
        }
        return returnedEsas;
    }

    /**
     * This will return any ESAs that match the supplied <code>identifier</code>. The matching is done on the same attributes as can be used in the name on a string passed to the
     * {@link MassiveResolver#resolve(String)} method, namely it is either the symbolic name, short name or lower case short name of the resource.
     *
     * @param attribute The attribute to match against
     * @param identifier The identifier to look for
     * @param loginInfo The repository login information to load the resources from
     * @return The EsaResources that match the identifier
     * @throws RepositoryBackendException
     */
    public static Collection<EsaResource> getMatchingEsas(FilterableAttribute attribute, String identifier, LoginInfo loginInfo) throws RepositoryBackendException {
        Collection<EsaResource> resources = new ResourceList();
        Map<MassiveClient.FilterableAttribute, Collection<String>> filters = new HashMap<MassiveClient.FilterableAttribute, Collection<String>>();
        filters.put(MassiveClient.FilterableAttribute.TYPE, Collections.singleton(Type.FEATURE.getAssetType().getValue()));
        for (LoginInfoEntry loginInfoResource : loginInfo) {
            MassiveClient client = new MassiveClient(loginInfoResource.getClientLoginInfo());
            try {
                filters.put(attribute.getClientFilterableAttribute(), Collections.singleton(identifier));
                Collection<Asset> assets = client.getFilteredAssets(filters);
                for (Asset ass : assets) {
                    EsaResource res = createResourceFromAsset(ass, loginInfoResource);
                    resources.add(res);
                }
            } catch (IOException ioe) {
                throw new RepositoryBackendIOException("Failed to obtain the assets from massive", ioe);
            } catch (RequestFailureException e) {
                throw new RepositoryBackendRequestFailureException(e);
            }
        }
        return resources;
    }

    /**
     * ----------------------------------------------------------------------------------------------------
     * INSTANCE METHODS
     * ----------------------------------------------------------------------------------------------------
     */

    /**
     * Constructor - requires connection info for massive
     *
     * @param userId The userid to connect to massive with
     * @param password The password to connect to massive with
     * @param apiKey The apikey for the repository
     */
    public EsaResource(LoginInfoEntry loginInfoResource) {
        super(loginInfoResource);
        setType(Type.FEATURE);
        setDownloadPolicy(DownloadPolicy.INSTALLER);
        setInstallPolicy(InstallPolicy.MANUAL);
    }

    /**
     * Set the provide feature field for this feature
     *
     * @param feature The name this feature should use.
     */
    public void setProvideFeature(String feature) {
        if (_asset.getWlpInformation().getProvideFeature() != null) {
            _asset.getWlpInformation().setProvideFeature(null);
        }
        _asset.getWlpInformation().addProvideFeature(feature);
    }

    /**
     * Gets the name of the feature
     *
     * @return The name of the feature
     */
    public String getProvideFeature() {
        Collection<String> provideFeatures = _asset.getWlpInformation()
                        .getProvideFeature();
        if (provideFeatures == null || provideFeatures.isEmpty()) {
            return null;
        } else {
            return provideFeatures.iterator().next();
        }
    }

    /*
     * Uses the required features information to calculate a list of queries
     * stored as Strings that can be searched upon later
     * Input the list of required features information to convert into the query
     * Returns the list of queries (Strings)
     */
    private Collection<String> createEnablesQuery() {
        Collection<String> query = null;
        Collection<String> requiredFeatures = getRequireFeature();
        if (requiredFeatures != null) {
            query = new ArrayList<String>();
            for (String required : requiredFeatures) {
                String temp = "wlpInformation.provideFeature=" + required;

                String version = findVersion();
                if (version != null) {
                    temp += "&wlpInformation.appliesToFilterInfo.minVersion.value=";
                    temp += version;
                }

                temp += "&type=";
                temp += getType().getValue(); // get the long name of the Type
                query.add(temp);
            }
        }
        return query;
    }

    /**
     * Uses the filter information to return the first version number
     *
     * @return the first version number
     */
    private String findVersion() {
        WlpInformation wlp = _asset.getWlpInformation();
        if (wlp == null) {
            return null;
        }

        Collection<AppliesToFilterInfo> filterInfo = wlp.getAppliesToFilterInfo();
        if (filterInfo == null) {
            return null;
        }

        for (AppliesToFilterInfo filter : filterInfo) {
            if (filter.getMinVersion() != null) {
                return filter.getMinVersion().getValue();
            }
        }
        return null;
    }

    /*
     * Calculates the Enabled By query (required by features)
     */
    private Collection<String> createEnabledByQuery() {
        Collection<String> query = new ArrayList<String>();
        String temp = "";

        //generate queries
        temp = "wlpInformation.requireFeature=";
        temp += getProvideFeature();
        String version = findVersion();
        if (version != null) {
            temp += "&wlpInformation.appliesToFilterInfo.minVersion.value=";
            temp += version;
        }

        temp += "&type=";
        temp += getType().getValue();

        query.add(temp);
        return query;
    }

    /**
     * @return the query for Superseded By or null if this feature
     *         does not declare itself to be superseded by anything.
     */
    private Collection<String> createSupersededByQuery() {
        Collection<String> supersededBy = _asset.getWlpInformation().getSupersededBy();

        Collection<String> query = null;
        if (supersededBy != null) { //if there are no queries to add
            query = new ArrayList<String>();
            for (String feature : supersededBy) {
                StringBuilder b = new StringBuilder();
                b.append("wlpInformation.shortName=");
                b.append(feature);
                b.append("&wlpInformation.appliesToFilterInfo.minVersion.value=");
                String version = findVersion();
                if (version != null) {
                    b.append(version);
                    b.append("&type=com.ibm.websphere.Feature");
                }
                query.add(b.toString());
            }
        }

        return query;
    }

    /**
     * @return the Supersedes query. Note that this query will always be
     *         set to something because the website can't tell if this features
     *         supersedes anything without running the query. So this method
     *         won't ever return null.
     */
    private Collection<String> createSupersedesQuery() {
        String shortName = _asset.getWlpInformation().getShortName();
        if (shortName != null) {
            StringBuilder b = new StringBuilder();
            b.append("wlpInformation.supersededBy=");
            b.append(shortName);
            String version = findVersion();
            if (version != null) {
                b.append("&wlpInformation.appliesToFilterInfo.minVersion.value=");
                b.append(version);
            }
            return Arrays.asList(new String[] { b.toString() });
        } else {
            // if we get here then our shortname is null so we can't create a
            // query that refers to it.
            return null;
        }
    }

    /**
     * This generates the string that should be displayed on the website to indicate
     * the supported Java versions. The requirements come from the bundle manifests.
     * The mapping between the two is non-obvious, as it is the intersection between
     * the Java EE requirement and the versions of Java that Liberty supports.
     */
    private void addVersionDisplayString() {
        WlpInformation wlp = _asset.getWlpInformation();
        JavaSEVersionRequirements reqs = wlp.getJavaSEVersionRequirements();
        if (reqs == null) {
            return;
        }

        String minVersion = reqs.getMinVersion();

        // Null means no requirements specified which is fine
        if (minVersion == null) {
            return;
        }

        String requiresJava7 = "Java SE 7";
        String requiresJava6or7 = "Java SE 6, Java SE 7";

        // The min version should have been validated when the ESA was constructed
        // so checking for the version string should be safe
        if (minVersion.equals("1.6.0")) {
            reqs.setVersionDisplayString(requiresJava6or7);
            return;
        }
        if (minVersion.equals("1.7.0")) {
            reqs.setVersionDisplayString(requiresJava7);
            return;
        }

        // The min version string has been generated/validated incorrectly
        // Can't recover from this, it is a bug in MassiveEsa
        throw new AssertionError();

    }

    /**
     * @return the query for Superseded By (optional) or null if this feature
     *         does not declare itself to be optionally superseded by anything.
     */
    private Collection<String> createSupersededByOptionalQuery() {
        Collection<String> supersededByOptional = _asset.getWlpInformation().getSupersededByOptional();

        Collection<String> query = null;
        if (supersededByOptional != null) { //if there are no queries to add
            query = new ArrayList<String>();

            for (String feature : supersededByOptional) {
                StringBuilder b = new StringBuilder();
                b.append("wlpInformation.shortName=");
                b.append(feature);
                b.append("&wlpInformation.appliesToFilterInfo.minVersion.value=");
                String version = findVersion();
                if (version != null) {
                    b.append(version);
                    b.append("&type=com.ibm.websphere.Feature");
                }
                query.add(b.toString());
            }
        }

        return query;
    }

    private Link makeLink(String label, String linkLabelProperty, Collection<String> query, String linkLabelPrefix, String linkLabelSuffix) {
        Link link = makeLink(label, linkLabelProperty, query);
        link.setLinkLabelPrefix(linkLabelPrefix);
        link.setLinkLabelSuffix(linkLabelSuffix);
        return link;
    }

    private Link makeLink(String label, String linkLabelProperty, Collection<String> query) {
        Link link = new Link();
        link.setLabel(label);
        link.setLinkLabelProperty(linkLabelProperty);
        link.setQuery(query);
        return link;
    }

    /**
     * Creates the links to enables/enabled-by/supersedes/superseded-by sections. At present,
     * link labels are hardcoded in this function. We may need to move them in the future if
     * we need to translate them or if we just don't like the idea of having hardcoded message
     * strings in here.
     */
    private Collection<Link> createLinks() {
        ArrayList<Link> links = new ArrayList<Link>();

        Collection<String> enablesQuery = createEnablesQuery();
        links.add(makeLink("Features that this feature enables", "name", enablesQuery));

        Collection<String> enabledByQuery = createEnabledByQuery();
        links.add(makeLink("Features that enable this feature", "name", enabledByQuery));

        Collection<String> supersedesQuery = createSupersedesQuery();
        links.add(makeLink("Features that this feature supersedes", "name", supersedesQuery));

        Collection<String> supersededByQuery = createSupersededByQuery();
        links.add(makeLink("Features that supersede this feature", "name", supersededByQuery));

        // Note: by giving this the same link title as superseded-by, the links appear in the same
        // link section on the website (but with the suffix that we add here).
        Collection<String> supersededByOptionalQuery = createSupersededByOptionalQuery();
        links.add(makeLink("Features that supersede this feature", "name", supersededByOptionalQuery, null, " (optional)"));

        return links;
    }

    @Override
    public void updateGeneratedFields() {
        super.updateGeneratedFields();

        setLinks(createLinks());

        // add the string the website will use for displaying java verison compatibility
        addVersionDisplayString();
    }

    protected Collection<AppliesToFilterInfo> getAppliesToFilterInfo() {
        return _asset.getWlpInformation().getAppliesToFilterInfo();
    }

    @Override
    public MassiveResourceMatchingData createMatchingData() {
        EsaResourceMatchingData matchingData = new EsaResourceMatchingData();
        matchingData.setType(getType());
        matchingData.setAtfi(getAppliesToFilterInfo());
        matchingData.setVersion(getVersion());
        matchingData.setProvideFeature(getProvideFeature());
        return matchingData;
    }

    @Override
    protected void copyFieldsFrom(MassiveResource fromResource, boolean includeAttachmentInfo) {
        super.copyFieldsFrom(fromResource, includeAttachmentInfo);
        EsaResource esaRes = (EsaResource) fromResource;
        setAppliesTo(esaRes.getAppliesTo());
        setDisplayPolicy(esaRes.getDisplayPolicy());
        setWebDisplayPolicy(esaRes.getWebDisplayPolicy());
        setInstallPolicy(esaRes.getInstallPolicy());
        setLinks(esaRes.getLinks());
        setProvideFeature(esaRes.getProvideFeature());
        setProvisionCapability(esaRes.getProvisionCapability());
        setRequireFeature(esaRes.getRequireFeature());
        setVisibility(esaRes.getVisibility());
        setShortName(esaRes.getShortName());
    }

    @Override
    protected String getNameForVanityUrl() {
        return getProvideFeature();
    }

    /**
     * Returns the Enables {@link Links} for this feature
     *
     * @return
     */
    public void setLinks(Collection<Link> links) {
        Collection<com.ibm.ws.massive.sa.client.model.Link> attachmentLinks = new ArrayList<com.ibm.ws.massive.sa.client.model.Link>();
        for (Link link : links) {
            attachmentLinks.add(link.getLink());
        }

        _asset.getWlpInformation().setLinks(attachmentLinks);
    }

    /**
     * Set the Enables {@link Links} for this feature
     *
     * @return
     */
    public Collection<Link> getLinks() {
        Collection<com.ibm.ws.massive.sa.client.model.Link> attachmentLinks = _asset.getWlpInformation().getLinks();
        Collection<Link> links = new ArrayList<Link>();
        for (com.ibm.ws.massive.sa.client.model.Link link : attachmentLinks) {
            links.add(new Link(link));
        }
        return links;
    }

    /**
     * Add the supplied feature to the list of required features
     *
     * @param requiredFeatureSymbolicName The name of the required feature that is to be added to the list
     *            of required features.
     */
    public void addRequireFeature(String requiredFeatureSymbolicName) {
        _asset.getWlpInformation().addRequireFeature(requiredFeatureSymbolicName);
    }

    /**
     * Add the supplied fix to the list of required fixes
     *
     * @param fix The ID of the fix
     */
    public void addRequireFix(String fix) {
        _asset.getWlpInformation().addRequireFix(fix);
    }

    /**
     * Returns a collection of iFix IDs that this feature require
     *
     * @return
     */
    public Collection<String> getRequireFix() {
        return _asset.getWlpInformation().getRequireFix();
    }

    /**
     * Sets the list of required features to the supplied list of features
     *
     * @param feats The list of features that should be used as the required features list.
     */
    public void setRequireFeature(Collection<String> feats) {
        _asset.getWlpInformation().setRequireFeature(feats);
    }

    /**
     * Gets the list of required features for this feature
     *
     * @return The list of required features defined on this feature
     */
    public Collection<String> getRequireFeature() {
        return _asset.getWlpInformation().getRequireFeature();
    }

    public void addSupersededBy(String feature) {
        _asset.getWlpInformation().addSupersededBy(feature);
    }

    public Collection<String> getSupersededBy() {
        return _asset.getWlpInformation().getSupersededBy();
    }

    public void addSupersededByOptional(String feature) {
        _asset.getWlpInformation().addSupersededByOptional(feature);
    }

    public Collection<String> getSupersededByOptional() {
        return _asset.getWlpInformation().getSupersededByOptional();
    }

    /**
     * Returns the {@link Visibility} for this feature
     *
     * @return
     */
    public Visibility getVisibility() {
        return Visibility.lookup(_asset.getWlpInformation().getVisibility());
    }

    /**
     * Set the Visibility to the supplied {@link Visibility}
     *
     * @param vis The {@link Visibility} to use for this feature
     */
    public void setVisibility(Visibility vis) {
        _asset.getWlpInformation().setVisibility(vis.getWlpVisibility());
    }

    /**
     * Sets the ibm short name to use for this feature
     *
     * @param shortName The ibm short name to use
     */
    public void setShortName(String shortName) {
        _asset.getWlpInformation().setShortName(shortName);
    }

    /**
     * Gets the ibm short name for this feature
     *
     * @return The ibm short name for this feature
     */
    public String getShortName() {
        return _asset.getWlpInformation().getShortName();
    }

    /**
     * Gets a lower case version of the {@link #getIbmShortName()}.
     *
     * @return
     */
    public String getLowerCaseShortName() {
        return _asset.getWlpInformation().getLowerCaseShortName();
    }

    /**
     * Sets the appliesTo field on the resource
     *
     * @param appliesTo The appliesTo field to be applied to the resource
     */
    public void setAppliesTo(String appliesTo) {
        _asset.getWlpInformation().setAppliesTo(appliesTo);
    }

    /**
     * Gets the appliesTo field associated with the resource
     *
     * @return The appliesTo field associated with the resource
     */
    public String getAppliesTo() {
        return _asset.getWlpInformation().getAppliesTo();
    }

//    @Override
//    protected String getVersionForVanityUrl() {
//        String version = "";
//        WlpInformation wlp = _asset.getWlpInformation();
//        if (wlp != null) {
//            Collection<AppliesToFilterInfo> atfis = wlp.getAppliesToFilterInfo();
//            if (atfis != null && !atfis.isEmpty()) {
//                AppliesToFilterInfo atfi = atfis.iterator().next();
//                if (atfi != null) {
//                    FilterVersion ver = atfi.getMinVersion();
//                    if (ver != null) {
//                        version = ver.getLabel();
//                    }
//                }
//            }
//        }
//        return version;
//    }

    /**
     * The {@link DisplayPolicy} to use
     */
    public void setDisplayPolicy(DisplayPolicy policy) {
        _asset.getWlpInformation().setDisplayPolicy(policy == null ? null : policy.getWlpDisplayPolicy());
    }

    /**
     * Get the {@link DisplayPolicy}
     *
     * @return {@link DisplayPolicy} in use
     */
    @Deprecated
    public DisplayPolicy getDisplayPolicy() {
        if (_asset.getWlpInformation() == null) {
            return null;
        }
        return DisplayPolicy.lookup(_asset.getWlpInformation().getDisplayPolicy());
    }

    /**
     * The {@link DisplayPolicy} to use
     */
    @Override
    public void setWebDisplayPolicy(DisplayPolicy policy) {
        _asset.getWlpInformation().setWebDisplayPolicy(policy == null ? null : policy.getWlpDisplayPolicy());
    }

    /**
     * Get the {@link DisplayPolicy}
     *
     * @return {@link DisplayPolicy} in use
     */
    /* package */DisplayPolicy getWebDisplayPolicy() {
        if (_asset.getWlpInformation() == null) {
            return null;
        }
        return DisplayPolicy.lookup(_asset.getWlpInformation().getWebDisplayPolicy());
    }

    /**
     * Returns the value of the ibmProvisionCapability field (from the
     * "IBM-Provision-Capability" header in the ESA's manifest) which gives
     * information about the capabilities required to be present for the feature
     * to be provisioned.
     *
     * @return the value of the "IBM-Provision-Capability" header
     */
    public String getProvisionCapability() {
        return _asset.getWlpInformation().getProvisionCapability();
    }

    /**
     * Sets the ibmProvisionCapability field.
     *
     * @param ibmProvisionCapability
     *            The new ibmProvisionCapability to be used
     */
    public void setProvisionCapability(String provisionCapability) {
        _asset.getWlpInformation().setProvisionCapability(provisionCapability);
    }

    /**
     * Returns {@link InstallPolicy#WHEN_SATISFIED} if this ESA should be installed automatically
     * if all of its provisioning capabilities are satisfied.
     *
     * @return The {@link InstallPolicy} for this ESAs, set to {@link InstallPolicy#WHEN_SATISFIED} if this ESA should be installed automatically and {@link InstallPolicy#MANUAL}
     */
    public InstallPolicy getInstallPolicy() {
        if (_asset.getWlpInformation() == null) {
            return null;
        }
        return InstallPolicy.lookup(_asset.getWlpInformation().getInstallPolicy());
    }

    /**
     * Specify if this ESA is auto installable, should only be set to
     * <code>WHEN_SATISFIED</code> if {@link #getIbmProvisionCapability()} returns a non-
     * <code>null</code> value.
     *
     * @param policy
     *            the new value for installPolicy
     */
    public void setInstallPolicy(InstallPolicy policy) {
        _asset.getWlpInformation().setInstallPolicy(policy == null ? null : policy.getWlpInstallPolicy());
    }

    /**
     * Specify the minimum/maximum Java version needed by this ESA, and the Require-Capability headers
     * from each contained bundle which have led to the requirement. All fields are allowed to be null.
     *
     * @param minimum an OSGI version string representing the minimum Java version required.
     * @param maximum an OSGI version string representing the minimum Java version required.
     * @param displayMinimum An alternative representation of the minimum version for display purposes
     * @param displayMaximum An alternative representation of the maximum version for display purposes
     * @param rawBundleRequirements The Require-Capability headers from all the bundles contained in this ESA
     */
    public void setJavaSEVersionRequirements(String minimum, String maximum, Collection<String> rawBundleRequirements) {
        JavaSEVersionRequirements reqs = new JavaSEVersionRequirements();
        reqs.setMinVersion(minimum);
        reqs.setMaxVersion(maximum);
        reqs.setRawRequirments(rawBundleRequirements);
        _asset.getWlpInformation().setJavaSEVersionRequirements(reqs);
    }

    /**
     * An ESA may require a minimum or maximum Java version. This is an aggregate min/max,
     * calculated from the individual requirements of the contained bundles, as specified
     * by the bundles' Require-Capability header in the bundle manifest. The
     * <code>JavaSEVersionRequirements</code> contains the set of the Require-Capability
     * headers, i.e. one from each bundle which specifies the header.
     * All fields in the version object may be null, if no requirement was specified in the bundles.
     *
     * @return
     */
    public JavaSEVersionRequirements getJavaSEVersionRequirements() {
        return _asset.getWlpInformation().getJavaSEVersionRequirements();
    }

}
