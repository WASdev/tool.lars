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

package com.ibm.ws.massive.esa;

import static com.ibm.ws.lars.testutils.ReflectionTricks.getAsset;
import static com.ibm.ws.lars.testutils.ReflectionTricks.reflectiveCallNoPrimitives;
import static com.ibm.ws.repository.strategies.writeable.UploadStrategy.DEFAULT_STRATEGY;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;
import com.ibm.ws.massive.esa.internal.EsaManifest;
import com.ibm.ws.massive.upload.RepositoryArchiveEntryNotFoundException;
import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.common.enums.InstallPolicy;
import com.ibm.ws.repository.common.enums.LicenseType;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.connections.ProductDefinition;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.SimpleProductDefinition;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.resources.AttachmentResource;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.EsaResourceImpl;
import com.ibm.ws.repository.resources.internal.Link;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl.AttachmentResourceImpl;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl.MatchResult;
import com.ibm.ws.repository.resources.writeable.EsaResourceWritable;
import com.ibm.ws.repository.strategies.writeable.UpdateInPlaceStrategy;
import com.ibm.ws.repository.strategies.writeable.UploadStrategy;
import com.ibm.ws.repository.transport.client.DataModelSerializer;
import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.WlpInformation;

/**
 * Test class for {@link MassiveEsa} it requires having a testLogin.properties folder at the root of
 * the com.ibm.ws.massive.testsuite project
 */
public class MassiveEsaTest {

    private static final String ENABLES = "Features that this feature enables";
    private static final String ENABLED_BY = "Features that enable this feature";
    private static final String SUPERSEDES = "Features that this feature supersedes";
    private static final String SUPERSEDED_BY = "Features that supersede this feature";

    private final static File esaDir = new File("resources");

    private MassiveEsa massiveEsa;

    @Rule
    public RepositoryFixture repo = FatUtils.FAT_REPO;
    private RepositoryConnection repoConnection;

    @Before
    public void createMassiveEsa() throws RepositoryException {
        repoConnection = repo.getAdminConnection();
        massiveEsa = new MassiveEsa(repoConnection);
    }

    private EsaResourceWritable uploadAsset(File blueprintESA) throws RepositoryException {
        return massiveEsa.uploadFile(blueprintESA, new UpdateInPlaceStrategy(State.AWAITING_APPROVAL, State.AWAITING_APPROVAL, false), null);
    }

    private void checkAttachment(RepositoryResource res) throws RepositoryBackendException, RepositoryResourceException {
        assertNotNull("No main attachment", res.getMainAttachment());
        assertEquals("Wrong attachment size", res.getMainAttachment().getSize(), res.getMainAttachmentSize());
    }

    /**
     * Returns a map from file path to size for each file in the given zip file.
     *
     * @param zipFile a zip file
     * @return a map (file path -> file size in bytes)
     * @throws IOException
     */
    private Map<String, Long> getZipFileSizes(File zipFile) throws IOException {
        Map<String, Long> sizes = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                sizes.put(entry.getName(), entry.getSize());
            }
        }
        return sizes;
    }

    @Test
    public void testRealFeature() throws Throwable {
        File blueprintESA = new File(esaDir,
                "com.ibm.websphere.appserver.blueprint-1.0.esa");
        File blueprintMetadata = new File(esaDir,
                "com.ibm.websphere.appserver.blueprint-1.0.esa.metadata.zip");

        Map<String, Long> esaSizes = getZipFileSizes(blueprintESA);
        Map<String, Long> metadataSizes = getZipFileSizes(blueprintMetadata);

        EsaResource blueprintFeature = uploadAsset(blueprintESA);
        AttachmentResource li = blueprintFeature
                .getLicenseInformation(Locale.ENGLISH);
        assertEquals("LI should be null!", null, li);

        AttachmentResourceImpl la = (AttachmentResourceImpl) blueprintFeature.getLicenseAgreement(Locale.ENGLISH);
        File downloadLA = new File(esaDir, "download_LA");
        la.downloadToFile(downloadLA);
        assertEquals("LA wrong size", (long) esaSizes.get("wlp/lafiles/LA_en"), downloadLA.length());

        AttachmentResourceImpl licenseImpl = (AttachmentResourceImpl) blueprintFeature.getLicense(Locale.ENGLISH);
        File downloadLicenseHTML = new File(esaDir, "download_en.html");
        licenseImpl.downloadToFile(downloadLicenseHTML);
        assertEquals("licenseHtml wrong size", (long) metadataSizes.get("lafiles/en.html"),
                     downloadLicenseHTML.length());

        AttachmentResource license = blueprintFeature.getLicense(Locale.TAIWAN);
        assertEquals("Missing license", Locale.TAIWAN, license.getLocale());
        assertEquals("Wrong attachment size", (long) metadataSizes.get("lafiles/zh_TW.html"), license.getSize());

        license = blueprintFeature.getLicenseAgreement(Locale.TAIWAN);
        assertEquals("Missing license agreement", Locale.TAIWAN, license.getLocale());
        assertEquals("Wrong attachment size", (long) esaSizes.get("wlp/lafiles/LA_zh_TW"), license.getSize());

        license = blueprintFeature.getLicenseAgreement(Locale.CHINESE);
        assertEquals("Missing license agreement", Locale.CHINESE, license.getLocale());
        assertEquals("Wrong attachment size", (long) esaSizes.get("wlp/lafiles/LA_zh"), license.getSize());
    }

    @Test
    public void testMainAttachment() throws Throwable {
        File simpleESA = new File(esaDir, "simple.esa");
        EsaResourceWritable mr = uploadAsset(simpleESA);
        checkAttachment(mr);
    }

    /**
     * Tests creating an asset in Massive and overriding the download URL
     *
     * @throws RepositoryException
     */
    @Test
    public void testOverriddenDownloadUrl() throws RepositoryException {
        String expectedUrl = "http://whatever";
        File file = new File(esaDir, "simple.esa");
        EsaResource esa = massiveEsa.uploadFile(file, DEFAULT_STRATEGY, expectedUrl);
        assertEquals("Supplied download URL not used", expectedUrl, esa.getMainAttachment().getURL());
    }

    @Test
    public void testAddLicensedPublicFeature() throws Throwable {
        File simpleESA = new File(esaDir, "simpleLicensedFeature.esa");
        EsaResource featureInMassive = uploadAsset(simpleESA);

        // check the license is of the expected type
        LicenseType lt = featureInMassive.getLicenseType();
        assertEquals("Wrong license type", LicenseType.ILAN, lt);

        AttachmentResource license = featureInMassive
                .getLicense(Locale.ENGLISH);
        assertEquals("Missing license", Locale.ENGLISH, license.getLocale());
        assertEquals("Wrong attachment size", 91, license.getSize());

        license = featureInMassive.getLicense(Locale.GERMAN);
        assertEquals("Missing license", Locale.GERMAN, license.getLocale());
        assertEquals("Wrong attachment size", 90, license.getSize());
    }

    @Test
    public void testAdditionalMetadata() throws Throwable {
        File simpleESA = new File(esaDir, "simpleLicensedFeature.esa");
        EsaResource featureInMassive = uploadAsset(simpleESA);

        assertEquals("Wrong name", "com.ibm.ws.test.simple",
                     featureInMassive.getName());
        assertEquals("Wrong short description",
                     "This is a simple test feature",
                     featureInMassive.getShortDescription());
        assertEquals(
                     "Wrong long description",
                     "<h2 id=\"ibm-wasdev-feature-instructions-title\">Command Line Install</h2><div id=\"ibm-wasdev-feature-instructions-content\"> To install the feature from the command line type: <code>bin/featureManager install simpleFeature-1.0 </code> </div> <h2 id=\"ibm-wasdev-feature-notes-title\"> Config Instructions</h2> <div id=\"ibm-wasdev-feature-notes-content\"> To use the feature at runtime add the following to your server.xml [code]<featureManager> <feature> simpleFeature-1.0</featureManager>[/code] </div>",
                     featureInMassive.getDescription());

    }

    @Test
    public void testMatches() throws Throwable {
        // IBM-AppliesTo: com.ibm.websphere.appserver; productVersion=8.5.5.1;
        File esa = new File(esaDir, "com.ibm.websphere.appserver.servlet-3.0.esa");
        EsaResourceImpl resource = (EsaResourceImpl) uploadAsset(esa);

        // We don't support install type yet so add it via reflection
        Field f = RepositoryResourceImpl.class.getDeclaredField("_asset");
        f.setAccessible(true);
        Asset ass = (Asset) f.get(resource);
        WlpInformation wlpInfo = ass.getWlpInformation();
        wlpInfo.getAppliesToFilterInfo().iterator().next().setInstallType("Archive");

        ProductDefinition def = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.1", "Archive", null, "BASE");
        assertEquals("Matcher should have returned MATCHED", MatchResult.MATCHED, resource.matches(def));

        def = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.2", "Archive", null, "BASE");
        assertEquals("Matcher should have returned INVALID_VERSION", MatchResult.INVALID_VERSION, resource.matches(def));

        def = new SimpleProductDefinition("wibble", "8.5.5.1", "Archive", null, "BASE");
        assertEquals("Matcher should have returned NOT_APPLICABLE", MatchResult.NOT_APPLICABLE, resource.matches(def));

        // Should match as our asset has no edition set
        def = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.1", "Archive", null, "Invalid");
        assertEquals("Matcher should have returned INVALID_EDITION", MatchResult.MATCHED, resource.matches(def));

        def = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.1", "Invalid", null, "BASE");
        assertEquals("Matcher should have returned INVALID_INSTALL_TYPE", MatchResult.INVALID_INSTALL_TYPE, resource.matches(def));

        def = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.1", "Archive", null, "BASE");
        assertEquals("Matcher should have returned MATCHED", MatchResult.MATCHED, resource.matches(def));

        // IBM-AppliesTo: com.ibm.websphere.appserver; productVersion=2014.3.0.0; productInstallType=Archive
        File esa2 = new File(esaDir, "com.ibm.websphere.appserver.blueprint-1.0.esa");
        EsaResourceImpl resource2 = (EsaResourceImpl) uploadAsset(esa2);

        def = new SimpleProductDefinition("com.ibm.websphere.appserver", "2014.3.0.0", "Archive", null, "BASE");
        assertEquals("Matcher should have returned MATCHED", MatchResult.MATCHED, resource2.matches(def));

    }

    @Test
    // This relies in blank fields...
    public void testFindAllMatches() throws Throwable {
        // IBM-AppliesTo: com.ibm.websphere.appserver; productVersion=8.5.5.1;
        File esa8552 = new File(esaDir, "com.ibm.websphere.appserver.servlet-3.0.esa");
        EsaResource resource8552 = uploadAsset(esa8552);

        // IBM-AppliesTo: com.ibm.websphere.appserver; productVersion=2014.3.0.0; productInstallType=Archive
        File betaEsa = new File(esaDir, "com.ibm.websphere.appserver.blueprint-1.0.esa");
        EsaResource betaResource = uploadAsset(betaEsa);

        // IBM-AppliesTo: com.ibm.websphere.appserver;
        File noVersionEsa = new File(esaDir, "NoVersionInAppliesTo.esa");
        EsaResource noVersionResource = uploadAsset(noVersionEsa);

        // IBM-AppliesTo: com.ibm.websphere.appserver;productVersion=8.5.5.1+;
        File versionRangeEsa = new File(esaDir, "VersionRange.esa");
        EsaResource versionRangeResource = uploadAsset(versionRangeEsa);

        ProductDefinition def = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.1", "Archive", null, "BASE");
        Collection<EsaResource> results = new RepositoryConnectionList(repoConnection).getMatchingEsas(def);
        assertEquals("There should be 3 hits", 3, results.size());
        for (EsaResource e : results) {
            EsaResourceImpl res = (EsaResourceImpl) e;
            assertTrue("Unexpected resource found in hits " + res.getName(),
                       res.equivalentWithoutAttachments(resource8552) || res.equivalentWithoutAttachments(noVersionResource)
                               || res.equivalentWithoutAttachments(versionRangeResource));
        }

        def = new SimpleProductDefinition("com.ibm.websphere.appserver", null, "Archive", null, "BASE");
        results = new RepositoryConnectionList(repoConnection).getMatchingEsas(def);
        assertEquals("There should be 4 hits", 4, results.size());
        for (EsaResource e : results) {
            EsaResourceImpl res = (EsaResourceImpl) e;
            assertTrue("Unexpected resource found in hits " + e.getName(),
                       res.equivalentWithoutAttachments(resource8552) || res.equivalentWithoutAttachments(betaResource) ||
                               res.equivalentWithoutAttachments(noVersionResource) || res.equivalentWithoutAttachments(versionRangeResource));
        }

        def = new SimpleProductDefinition("com.ibm.websphere.appserver", "2014.4.0.0", "Archive", null, "BASE");
        results = new RepositoryConnectionList(repoConnection).getMatchingEsas(def);
        assertEquals("There should be 1 hit", 2, results.size());
        for (EsaResource e : results) {
            EsaResourceImpl res = (EsaResourceImpl) e;
            assertTrue("Unexpected resource found in hits " + res.getName(),
                       res.equivalentWithoutAttachments(noVersionResource) || res.equivalentWithoutAttachments(versionRangeResource));
        }
    }

    @Test
    public void testMatchesVisibility() throws Throwable {
        File esa8552 = new File(esaDir, "com.ibm.websphere.appserver.servlet-3.0.esa");
        EsaResource resource8552 = uploadAsset(esa8552);

        File betaEsa = new File(esaDir, "com.ibm.websphere.appserver.blueprint-1.0.esa");
        EsaResource betaResource = uploadAsset(betaEsa);

        File esaPriv = new File(esaDir, "AppliesToPrivateTest.esa");
        EsaResource resourcePriv = uploadAsset(esaPriv);

        ProductDefinition def = new SimpleProductDefinition("com.ibm.websphere.appserver", null, "Archive", null, "BASE");
        Collection<EsaResource> result = new RepositoryConnectionList(repoConnection).getMatchingEsas(def, Visibility.PRIVATE);
        assertEquals("There should be 1 private ESA", 1, result.size());
        assertEquals("The hidden one should be the private esa", result.iterator().next().getName(), resourcePriv.getName());

        result = new RepositoryConnectionList(repoConnection).getMatchingEsas(def, Visibility.PUBLIC);
        assertEquals("There should be 2 public ESAs", 2, result.size());
        for (EsaResource r : result) {
            assertTrue("Unexpected name for public esa " + r.getName(),
                       r.getName().equals(resource8552.getName()) || r.getName().equals(betaResource.getName()));
        }
    }

    /**
     * Same as {@link #testAddLicensedPublicFeature()} but does a get all first
     *
     * @throws Throwable
     */
    @Test
    public void testGetLicensedPublicFeature() throws Throwable {
        File simpleESA = new File(esaDir, "simpleLicensedFeature.esa");
        uploadAsset(simpleESA);

        EsaResource featureInMassive = null;
        Collection<EsaResource> allEsas = new RepositoryConnectionList(repoConnection).getAllFeatures();
        for (EsaResource esa : allEsas) {
            if ("com.ibm.ws.test.simple".equals(esa.getProvideFeature())) {
                featureInMassive = esa;
                break;
            }
        }
        assertNotNull("We should have found the ESA", featureInMassive);

        // check the license is of the expected type
        LicenseType lt = featureInMassive.getLicenseType();
        assertEquals("Wrong license type", LicenseType.ILAN, lt);

        AttachmentResource license = featureInMassive
                .getLicense(Locale.ENGLISH);
        assertNotNull("We should have found the license", license);
        assertEquals("Missing license", Locale.ENGLISH, license.getLocale());
        assertEquals("Wrong attachment size", 91, license.getSize());

        license = featureInMassive.getLicense(Locale.GERMAN);
        assertNotNull("We should have found the license", license);
        assertEquals("Missing license", Locale.GERMAN, license.getLocale());
        assertEquals("Wrong attachment size", 90, license.getSize());

    }

    @Test
    public void testLIAndLAandAppliesTo() throws Throwable {
        File esaFile = new File(esaDir,
                "com.ibm.websphere.appserver.json-1.0.esa");
        uploadAsset(esaFile);

        EsaResource featureInMassive = null;
        Collection<EsaResource> allEsas = new RepositoryConnectionList(repoConnection).getAllFeatures();
        for (EsaResource esa : allEsas) {
            if ("com.ibm.websphere.appserver.json-1.0".equals(esa
                    .getProvideFeature())) {
                featureInMassive = esa;
                break;
            }
        }
        assertNotNull("We should have found the ESA", featureInMassive);

        assertNotNull("No LI file found",
                      featureInMassive.getLicenseInformation(Locale.FRANCE));
        assertNotNull("No LA file found",
                      featureInMassive.getLicenseAgreement(Locale.GERMAN));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        featureInMassive.dump(baos);

        String test = baos.toString();
        assertTrue("Applies do did not encode as JSON",
                   test.contains("appliesToFilterInfo"));
        Asset a = DataModelSerializer.deserializeObject(
                                                        new ByteArrayInputStream(baos.toByteArray()), Asset.class);
        Collection<AppliesToFilterInfo> atfis = a.getWlpInformation()
                .getAppliesToFilterInfo();
        assertTrue("Applies to should have encoded to a single atfi",
                   atfis.size() == 1);
        AppliesToFilterInfo atfi = atfis.iterator().next();
        assertTrue(
                   "Applies to should have encoded product id of com.ibm.websphere.appserver",
                   "com.ibm.websphere.appserver".equals(atfi.getProductId()));
        assertNotNull("Applies to should have encoded max version ",
                      atfi.getMaxVersion());
        assertNotNull("Applies to should have encoded min version ",
                      atfi.getMinVersion());
        assertTrue("Applies to should have encoded max version inclusive", atfi
                .getMaxVersion().getInclusive());
        assertTrue("Applies to should have encoded min version inclusive", atfi
                .getMinVersion().getInclusive());
        assertEquals("Applies to should have encoded min version label",
                     "8.5.5", atfi.getMinVersion().getLabel());
        assertEquals("Applies to should have encoded max version label",
                     "8.5.5", atfi.getMaxVersion().getLabel());
        assertEquals("Applies to should have encoded max version value",
                     "8.5.5.2", atfi.getMinVersion().getValue());
        assertEquals("Applies to should have encoded max version value",
                     "8.5.5.2", atfi.getMaxVersion().getValue());
        assertNotNull("Applies to should have encoded editions list ",
                      atfi.getEditions());
        assertEquals(
                     "Applies to should have encoded editions list with default 7 elements ",
                     7, atfi.getEditions().size());
        assertTrue(
                   "Applies to should have encoded default editions got: "
                           + atfi.getEditions(),
                   atfi.getEditions().containsAll(
                                                  Arrays.asList("Liberty Core", "Base", "Express",
                                                                "Developers", "ND", "z/OS")));

    }

    /**
     * Test to make sure that private features can be added to Massive
     *
     * @throws Throwable
     */
    @Test
    public void testAddPrivateFeature() throws Throwable {
        File simpleEsa = new File(esaDir, "simple.hidden.esa");
        EsaResourceWritable featureInMassive = uploadAsset(simpleEsa);
        assertEquals(
                     "The symbolic name should have been set to the value in the ESA file",
                     "com.ibm.ws.test.simple.hidden",
                     featureInMassive.getProvideFeature());
        assertEquals("The feature display policy should be hidden", DisplayPolicy.HIDDEN,
                     featureInMassive.getDisplayPolicy());
        assertEquals("The feature web display policy should be hidden", DisplayPolicy.HIDDEN,
                     reflectiveCallNoPrimitives(featureInMassive, "getWebDisplayPolicy", (Object[]) null));
        assertNotNull("The ID should have been set by Massive",
                      featureInMassive.getId());
        assertEquals("State should be awaiting approval.",
                     State.AWAITING_APPROVAL, featureInMassive.getState());

        // Make sure there is an attachment
        assertEquals(
                     "One attachment for the ESA should have been added to the feature in Massive",
                     1, featureInMassive.getAttachments().size());
        assertEquals(
                     "The attachment name should be the ESA name and set to the symbolic name of the feature",
                     "com.ibm.ws.test.simple.hidden.esa", featureInMassive
                             .getAttachments().iterator().next().getName());
        assertNull(
                   "The ibm provision capability should not be set if it isn't set in the feature manifests",
                   featureInMassive.getProvisionCapability());
    }

    /**
     * Test to make sure that public features can be added to Massive
     *
     * @throws Throwable
     */
    @Test
    public void testAddPublicFeature() throws Throwable {
        File simpleEsa = new File(esaDir, "simple.esa");
        EsaResourceWritable featureInMassive = uploadAsset(simpleEsa);
        assertEquals(
                     "The symbolic name should have been set to the value in the ESA file",
                     "com.ibm.ws.test.simple", featureInMassive.getProvideFeature());
        assertEquals("The feature display policy should be visible", DisplayPolicy.VISIBLE,
                     featureInMassive.getDisplayPolicy());
        assertEquals("The feature web display policy should be visible", DisplayPolicy.VISIBLE,
                     reflectiveCallNoPrimitives(featureInMassive, "getWebDisplayPolicy", (Object[]) null));
        assertNotNull("The ID should have been set by Massive",
                      featureInMassive.getId());
        assertEquals(
                     "The feature should be created in awaiting_approval state",
                     State.AWAITING_APPROVAL, featureInMassive.getState());
    }

    /**
     * Test to make sure that public features can be added to Massive by using the short name to
     * indicate it should be public
     *
     * @throws Throwable
     */
    @Test
    public void testAddPublicFeatureByShortName() throws Throwable {
        File simpleEsa = new File(esaDir, "simple.with.short.name.esa");
        EsaResourceWritable featureInMassive = uploadAsset(simpleEsa);
        assertEquals(
                     "The symbolic name should have been set to the value in the ESA file",
                     "com.ibm.ws.test.simple.with.short.name",
                     featureInMassive.getProvideFeature());
        assertEquals("The feature display policy should be visible", DisplayPolicy.VISIBLE,
                     featureInMassive.getDisplayPolicy());
        assertEquals("The feature webdisplay policy should be visible", DisplayPolicy.VISIBLE,
                     reflectiveCallNoPrimitives(featureInMassive, "getWebDisplayPolicy", (Object[]) null));
        assertNotNull("The ID should have been set by Massive",
                      featureInMassive.getId());
        assertEquals(
                     "The feature should be created in awaiting_approval state",
                     State.AWAITING_APPROVAL, featureInMassive.getState());
    }

    /**
     * Test adding a feature where the longDescription is stored in the properties file rather than
     * in a separate file in the metadata zip.
     */
    @Test
    public void testDescriptionInProperties() throws Throwable {
        File simpleEsa = new File(esaDir, "descriptionInProperties.esa");
        EsaResource featureInMassive = uploadAsset(simpleEsa);
        assertEquals("Long description should match string in properties file",
                     "Test long description", featureInMassive.getDescription());
    }

    /**
     * Tests that you can update an existing asset
     *
     * @throws Throwable
     */
    @Test
    public void testUpdate() throws Throwable {
        assumeThat(repo.isUpdateSupported(), is(true));

        File simpleEsa = new File(esaDir, "simple.esa");
        EsaResourceWritable featureInMassive = uploadAsset(simpleEsa);
        assertEquals("Check state is in awaiting approval state.",
                     State.AWAITING_APPROVAL, featureInMassive.getState());
        assertEquals(
                     "The asset should have been created with the initial visibility value!",
                     Visibility.PUBLIC, featureInMassive.getVisibility());
        String originalAssetId = featureInMassive.getId();
        File updatedEsa = new File(esaDir, "simple.with.new.visibility.esa");
        featureInMassive = uploadAsset(updatedEsa);
        assertEquals("The asset visibility should have been updated",
                     Visibility.PRIVATE, featureInMassive.getVisibility());
        assertEquals("The asset ID should not have changed", originalAssetId,
                     featureInMassive.getId());
        assertEquals("The asset should only have one attachment", 1,
                     featureInMassive.getAttachments().size());
        assertEquals("Feature should still be in awaiting approval state.",
                     State.AWAITING_APPROVAL, featureInMassive.getState());

    }

    /**
     * Tests enabling information is loaded. One enables, and one enabled by link
     *
     * @throws Throwable
     */
    @Test
    public void testEnablingInfo() throws Throwable {
        File enablingEsa = new File(esaDir, "enabling2.esa");
        EsaResourceImpl autoFeatureInMassive = (EsaResourceImpl) uploadAsset(enablingEsa);
        Collection<Link> links = autoFeatureInMassive.getLinks();
        Collection<String> expectedEnablesQuery = new ArrayList<String>();
        expectedEnablesQuery.add("wlpInformation.provideFeature=com.ibm.websphere.appserver.appLifecycle-1.0&wlpInformation.appliesToFilterInfo.minVersion.value=8.5.5.2&type=com.ibm.websphere.Feature");

        Collection<String> expectedQuery = new ArrayList<String>();
        expectedQuery.add("wlpInformation.requireFeature=com.ibm.websphere.appserver.contextService-1.0&wlpInformation.appliesToFilterInfo.minVersion.value=8.5.5.2&type=com.ibm.websphere.Feature");

        int linkNumber = 0;
        for (Link temp : links)
        {
            linkNumber++;
            System.out.println(temp);

            if (linkNumber == 1)
            {
                assertEquals("Enables label", ENABLES, temp.getLabel());
                assertEquals("Enables link label property matches", "name", temp.getLinkLabelProperty());
                assertEquals("Enables query matches", expectedEnablesQuery, temp.getQuery());

            }
            else if (linkNumber == 2) {
                assertEquals("Enabled By label", ENABLED_BY, temp.getLabel());
                assertEquals("Enabled By link label property matches", "name", temp.getLinkLabelProperty());
                assertEquals("Enabled By query matches", expectedQuery, temp.getQuery());
            }
        }
    }

    /**
     * Tests enabling information is loaded. One enables with no query, and one enabled by link
     *
     * @throws Throwable
     */
    @Test
    public void testEnablingInfo2() throws Throwable {
        File enablingEsa = new File(esaDir, "enabling4.esa");
        EsaResourceImpl autoFeatureInMassive = (EsaResourceImpl) uploadAsset(enablingEsa);
        Collection<Link> links = autoFeatureInMassive.getLinks();
        Collection<String> expectedQuery = new ArrayList<String>();
        expectedQuery.add("wlpInformation.requireFeature=com.ibm.websphere.appserver.contextService-1.0&wlpInformation.appliesToFilterInfo.minVersion.value=8.5.5.2&type=com.ibm.websphere.Feature");

        int linkNumber = 0;
        for (Link temp : links)
        {
            linkNumber++;
            System.out.println("Link " + linkNumber);
            System.out.println(temp);

            if (linkNumber == 1)
            {
                assertEquals("Enables label", ENABLES, temp.getLabel());
                assertEquals("Enables link label property matches", "name", temp.getLinkLabelProperty());
                assertEquals("Enables query matches", null, temp.getQuery());

            }
            else if (linkNumber == 2) {
                assertEquals("Enabled By label", ENABLED_BY, temp.getLabel());
                assertEquals("Enabled By link label property matches", "name", temp.getLinkLabelProperty());
                assertEquals("Enabled By query matches", expectedQuery, temp.getQuery());
            }
        }
    }

    /**
     * Tests enabling information is loaded. One enables with no query, and one enabled by link with
     * no version
     *
     * @throws Throwable
     */
    @Test
    public void testEnablingInfo3() throws Throwable {
        File enablingEsa = new File(esaDir, "enabling5.esa");
        EsaResourceImpl autoFeatureInMassive = (EsaResourceImpl) uploadAsset(enablingEsa);
        Collection<Link> links = autoFeatureInMassive.getLinks();
        Collection<String> expectedQuery = new ArrayList<String>();
        expectedQuery.add("wlpInformation.requireFeature=com.ibm.websphere.appserver.contextService-1.0&type=com.ibm.websphere.Feature");
        int linkNumber = 0;
        for (Link temp : links)
        {
            linkNumber++;

            if (linkNumber == 1)
            {
                assertEquals("Enables label", ENABLES, temp.getLabel());
                assertEquals("Enables link label property matches", "name", temp.getLinkLabelProperty());
                assertEquals("Enables query matches", null, temp.getQuery());

            }
            else if (linkNumber == 2) {
                assertEquals("Enabled By label", ENABLED_BY, temp.getLabel());
                assertEquals("Enabled By link label property matches", "name", temp.getLinkLabelProperty());
                assertEquals("Enabled By query matches", expectedQuery, temp.getQuery());
            }
        }
    }

    /**
     * Tests enabling information is loaded. Two enables queries, and one enabled by query
     *
     * @throws Throwable
     */
    @Test
    public void testEnablingInfo4() throws Throwable {
        File enablingEsa = new File(esaDir, "enabling3.esa");
        EsaResourceImpl autoFeatureInMassive = (EsaResourceImpl) uploadAsset(enablingEsa);
        Collection<Link> links = autoFeatureInMassive.getLinks();
        Collection<String> expectedEnablesQuery = new HashSet<String>();
        expectedEnablesQuery.add("wlpInformation.provideFeature=com.ibm.websphere.appserver.appLifecycle-1.0&wlpInformation.appliesToFilterInfo.minVersion.value=8.5.5.2&type=com.ibm.websphere.Feature");
        expectedEnablesQuery.add("wlpInformation.provideFeature=com.ibm.websphere.appserver.appLifecycle-2.0&wlpInformation.appliesToFilterInfo.minVersion.value=8.5.5.2&type=com.ibm.websphere.Feature");

        Collection<String> expectedQuery = new ArrayList<String>();
        expectedQuery.add("wlpInformation.requireFeature=com.ibm.websphere.appserver.contextService-1.0&wlpInformation.appliesToFilterInfo.minVersion.value=8.5.5.2&type=com.ibm.websphere.Feature");

        int linkNumber = 0;
        for (Link temp : links)
        {
            linkNumber++;
            System.out.println(temp);

            if (linkNumber == 1)
            {
                assertEquals("Enables label", ENABLES, temp.getLabel());
                assertEquals("Enables link label property matches", "name", temp.getLinkLabelProperty());

                // order isn't important so create a HashSet of the results and use the HashSet equals as
                // the order cannot be guaranteed.
                HashSet<String> hs = new HashSet<String>(temp.getQuery());
                assertEquals("Enables query matches", expectedEnablesQuery, hs);
            }
            else if (linkNumber == 2) {
                assertEquals("Enabled By label", ENABLED_BY, temp.getLabel());
                assertEquals("Enabled By link label property matches", "name", temp.getLinkLabelProperty());
                assertEquals("Enabled By query matches", expectedQuery, temp.getQuery());
            }
        }
    }

    /**
     * This tests that the translated name and description get set correctly in Massive
     *
     * @throws Throwable
     */
    @Test
    public void testTranslation() throws Throwable {
        File translatedEsa = new File(esaDir, "translated.esa");
        EsaResource featureInMassive = uploadAsset(translatedEsa);
        assertEquals("The name should be translated", "Translated Name",
                     featureInMassive.getName());
        assertEquals("The description should be translated",
                     "Translated Description",
                     featureInMassive.getShortDescription());
    }

    /**
     * Test that an ESA with a required fix is added correctly
     *
     * @throws Throwable
     */
    @Test
    public void testRequiredFixes() throws Throwable {
        File simpleEsa = new File(esaDir, "with.required.fix.esa");
        EsaResource featureInMassive = uploadAsset(simpleEsa);
        assertEquals(
                     "The symbolic name should have been set to the value in the ESA file",
                     "com.ibm.ws.test.with.required.fix",
                     featureInMassive.getProvideFeature());
        assertEquals("The ESA requires one fix", 1, featureInMassive.getRequireFix().size());
        assertTrue("The required fix should have been set to the value in the ESA",
                   featureInMassive.getRequireFix().contains("PM00000"));
    }

    /**
     * Test that an ESA with multiple required fixes is added correctly
     *
     * @throws Throwable
     */
    @Test
    public void testMultipleRequiredFixes() throws Throwable {
        File simpleEsa = new File(esaDir, "with.multiple.required.fix.esa");
        EsaResource featureInMassive = uploadAsset(simpleEsa);
        assertEquals(
                     "The symbolic name should have been set to the value in the ESA file",
                     "com.ibm.ws.test.with.multiple.required.fix",
                     featureInMassive.getProvideFeature());
        assertEquals("The ESA requires two fixes", 2, featureInMassive.getRequireFix().size());
        assertTrue(
                   "The required fix should contain the first value in the ESA",
                   featureInMassive.getRequireFix().contains("PM00000"));
        assertTrue(
                   "The required fix should contain the second value in the ESA",
                   featureInMassive.getRequireFix().contains("PM00001"));
    }

    /**
     * Tests that you can have two copies of a feature with different versions
     *
     * @throws Throwable
     */
    @Test
    public void testMultipleVersions() throws Throwable {
        File simpleEsa = new File(esaDir, "simple.esa");
        EsaResource featureInMassiveV100 = uploadAsset(simpleEsa);
        assertEquals(
                     "The version of the resource should match the one in the ESA",
                     "1.0.0", featureInMassiveV100.getVersion());

        String originalAssetId = featureInMassiveV100.getId();
        File updatedEsa = new File(esaDir, "simple.with.new.version.esa");
        EsaResource featureInMassiveV101 = uploadAsset(updatedEsa);
        assertFalse("The asset ID should be new",
                    originalAssetId.equals(featureInMassiveV101.getId()));
        assertEquals("The asset should only have one attachment", 1,
                     featureInMassiveV101.getAttachments().size());
        assertEquals(
                     "The version of the resource should match the one in the ESA",
                     "1.0.1", featureInMassiveV101.getVersion());

        // Also make sure nothing changed on the v100 feature
        featureInMassiveV100 = (EsaResource) repoConnection.getResource(originalAssetId);
        assertNotNull("There should still be a v1.0.0 asset in massive",
                      featureInMassiveV100);
        assertEquals("The ID of the v1.0.0 feature should not of changed",
                     originalAssetId, featureInMassiveV100.getId());
        assertEquals("The version of the v1.0.0 feature should not of changed",
                     "1.0.0", featureInMassiveV100.getVersion());
    }

    @Test
    public void testUpdatedEsaWithSameVersionButNewAppliesTo() throws Throwable {
        File simpleEsa = new File(esaDir, "simple.esa");
        EsaResource featureInMassiveV100 = uploadAsset(simpleEsa);
        assertEquals(
                     "The version of the resource should match the one in the ESA",
                     "1.0.0", featureInMassiveV100.getVersion());

        File updatedEsa = new File(esaDir, "simple.updatedAppliesTo8551.esa");
        uploadAsset(updatedEsa);

        assertEquals("There should be 2 features now", 2, repoConnection.getAllResources().size());

        updatedEsa = new File(esaDir, "simple.updatedAppliesTo8552.esa");
        uploadAsset(updatedEsa);

        assertEquals("There should be 3 features now", 3, repoConnection.getAllResources().size());

        updatedEsa = new File(esaDir, "simple.updatedAppliesTo8552.esa");
        uploadAsset(updatedEsa);

        assertEquals("There should still be 3 features", 3, repoConnection.getAllResources().size());
    }

    // Counts the number of icon files within an attachment list, and checks
    // they exist
    private int countIcons(Collection<AttachmentResource> attachmentList)
            throws Throwable {
        int iconCount = 0;
        for (AttachmentResource attachment : attachmentList) {
            if (attachment.getType() == AttachmentType.THUMBNAIL) {
                assertNotNull(attachment.getInputStream());
                iconCount++;
            }
        }
        return iconCount;
    }

    /**
     * Test for a single icon which has a size
     *
     * @throws Throwable
     */
    @Test
    public void testOneIconOneSize() throws Throwable {
        File iconEsa = new File(esaDir, "icontest.1icon.1size.esa");
        EsaResource autoFeatureInMassive = uploadAsset(iconEsa);
        assertEquals(1, countIcons(autoFeatureInMassive.getAttachments()));
    }

    /**
     * Test for 2 icons, neither of which has a size
     *
     * @throws Throwable
     */
    @Test
    public void testTwoIconsNoSize() throws Throwable {

        File iconEsa = new File(esaDir, "icontest.2icons.0sizes.esa");
        EsaResource autoFeatureInMassive = uploadAsset(iconEsa);
        assertEquals(2, countIcons(autoFeatureInMassive.getAttachments()));
    }

    /**
     * Test for 2 icons, one of which has a size
     *
     * @throws Throwable
     */
    @Test
    public void testTwoIconsOneSize() throws Throwable {
        File iconEsa = new File(esaDir, "icontest.2icons.1size.esa");
        EsaResource autoFeatureInMassive = uploadAsset(iconEsa);
        assertEquals(2, countIcons(autoFeatureInMassive.getAttachments()));
    }

    /**
     * Test for 2 icons, both in the manifest referring to the same filename
     *
     * @throws Throwable
     */
    @Test
    public void testTwoIconsOneSizeDuplicatefile() throws Throwable {
        File iconEsa = new File(esaDir, "icontest.2icons.1size.duplicatefile.esa");
        EsaResource autoFeatureInMassive = uploadAsset(iconEsa);
        assertEquals(1, countIcons(autoFeatureInMassive.getAttachments()));
    }

    /**
     * Test for 2 icons, both of which have a size
     *
     * @throws Throwable
     */
    @Test
    public void testTwoIconsTwoSizes() throws Throwable {
        File iconEsa = new File(esaDir, "icontest.2icons.2sizes.esa");
        EsaResource autoFeatureInMassive = uploadAsset(iconEsa);
        assertEquals(2, countIcons(autoFeatureInMassive.getAttachments()));
    }

    /**
     * Test for 1 icon that does not exist
     *
     * @throws Throwable
     */
    @Test
    public void testOneIconMissingFile() throws Throwable {
        File iconEsa = new File(esaDir, "icontest.1icon.1size.missingfile.esa");
        try {
            EsaResource autoFeatureInMassive = uploadAsset(iconEsa);
            fail("Expected to throw exception because of missing icon file");
        } catch (RepositoryArchiveEntryNotFoundException e) {
            // expected error
        }
    }

    /**
     * Test to make sure something with the IBM-Provision-Capability header set will be read in and
     * that it is not auto installable by default
     *
     * @throws Throwable
     */
    @Test
    public void testAutoFeature() throws Throwable {
        File autoFeatureEsa = new File(esaDir, "auto.feature.esa");
        EsaResource autoFeatureInMassive = uploadAsset(autoFeatureEsa);
        assertEquals(
                     "The IBM-Provision-Capability should have been read into the field",
                     "osgi.identity; filter:=\"(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.ws.test.simple))\"",
                     autoFeatureInMassive.getProvisionCapability());
        assertEquals(
                     "auto features without the IBM-Install-Policy header should not be installable by default",
                     InstallPolicy.MANUAL, autoFeatureInMassive.getInstallPolicy());
    }

    /**
     * Test to make sure something with the IBM-Provision-Capability header set that is a manual
     * install will set the display policy to visible even if it is private
     *
     * @throws Throwable
     */
    @Test
    public void testPrivateAutoFeature() throws Throwable {
        File autoFeatureEsa = new File(esaDir, "private.auto.feature.esa");
        EsaResource autoFeatureInMassive = uploadAsset(autoFeatureEsa);
        assertEquals(
                     "The IBM-Provision-Capability should have been read into the field",
                     "osgi.identity; filter:=\"(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.ws.test.simple))\"",
                     autoFeatureInMassive.getProvisionCapability());
        assertEquals(
                     "auto features without the IBM-Install-Policy header should not be installable by default",
                     InstallPolicy.MANUAL, autoFeatureInMassive.getInstallPolicy());
        assertEquals("This is a private auto feature", Visibility.PRIVATE, autoFeatureInMassive.getVisibility());
        assertEquals("This is a manual install auto feature so should be visible", DisplayPolicy.VISIBLE, autoFeatureInMassive.getDisplayPolicy());
    }

    /**
     * Test to make sure something with the IBM-Provision-Capability header set will be read in and
     * the IBM-Install-Policy set to when-satisfied makes it auto installable
     *
     * @throws Throwable
     */
    @Test
    public void testAutoFeatureExplicitInstallable() throws Throwable {
        File autoFeatureEsa = new File(esaDir, "auto.feature.explicit.installable.esa");
        EsaResource autoFeatureInMassive = uploadAsset(autoFeatureEsa);
        assertEquals(
                     "The IBM-Provision-Capability should have been read into the field",
                     "osgi.identity; filter:=\"(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.ws.test.simple))\"",
                     autoFeatureInMassive.getProvisionCapability());
        assertEquals(
                     "auto features with the IBM-Install-Policy header set to when-satisfied should be installable",
                     InstallPolicy.WHEN_SATISFIED,
                     autoFeatureInMassive.getInstallPolicy());
    }

    /**
     * Test to make sure something with the IBM-Provision-Capability header set will be read in and
     * the IBM-Install-Policy set to manual makes it not auto installable
     *
     * @throws Throwable
     */
    @Test
    public void testAutoFeatureNotInstallable() throws Throwable {
        File autoFeatureEsa = new File(esaDir, "auto.feature.not.installable.esa");
        EsaResource autoFeatureInMassive = uploadAsset(autoFeatureEsa);
        assertEquals(
                     "The IBM-Provision-Capability should have been read into the field",
                     "osgi.identity; filter:=\"(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.ws.test.simple))\"",
                     autoFeatureInMassive.getProvisionCapability());
        assertEquals(
                     "auto features with the IBM-Install-Policy header set to manual should not be installable",
                     InstallPolicy.MANUAL, autoFeatureInMassive.getInstallPolicy());
    }

    /**
     * Test uploading an asset with an invalid download URL.
     *
     * @throws Throwable
     */
    @Test
    public void testInvalidDownloadUrl() throws Throwable {
        File dheFeatureEsa = new File(esaDir, "downloadUrlBlank.esa");
        EsaResource downloadUrlBlankEsaResource = uploadAsset(dheFeatureEsa);

        // Make sure we can read the feature from DHE
        File tmp = File.createTempFile(
                                       "testInvalidDownloadUrl_subsytemManifest", ".esa");
        tmp.deleteOnExit();
        ((AttachmentResourceImpl) downloadUrlBlankEsaResource.getMainAttachment()).downloadToFile(tmp);

        EsaManifest feature = EsaManifest.constructInstance(tmp);

        assertNotNull("Unable to load feature from input stream", feature);
        assertEquals("The wrong feature was returned",
                     "com.ibm.ws.test.downloadUrlBlank", feature.getSymbolicName());

        // The download URL was invalid so didn't point to a valid ESA which
        // means if we got this far it must of uploaded it as it should of done

        // It is marked for delete on exit but might as well tidy up anyway...
        tmp.delete();
    }

    /**
     * Some artifacts may have to be uploaded with a 'license' of the form, "This download contains
     * the following files: {x, y, z} and is shipped to you under the terms of the Liberty license
     * into which you are installing this feature."
     *
     * Currently such licenses must be uploaded with LicenseType.UNSPECIFIED.
     */

    @Test
    public void testForFindingAllESAsWithAGivenLicense() throws Throwable {

        // Most of our tests upload resources to the repository through
        // MassiveTest.uploadAsset(). However that only works if createTestObject returns
        // a MassiveResource. This class' createTestObject() returns an EsaResource, which
        // prevents uploadAsset() from working. The next block of code is a slightly longer
        // winded approach.

        // Feature com.ibm.ws.test.simple with an IPLA license
        File iplaZip = new File(esaDir, "iplaLicensedFeature.esa");

        // Feature com.ibm.ws.test.translated with an ILAN license
        File ilanZip = new File(esaDir, "ilanLicensedFeature.esa");

        // Feature com.ibm.ws.test.with.required.fix with an UNSPECIFIED license
        File unspecZip = new File(esaDir, "unspecifiedLicensedFeature.esa");

        Collection<File> files = new ArrayList<File>();
        files.add(iplaZip);
        files.add(ilanZip);
        files.add(unspecZip);

        MassiveEsa client = new MassiveEsa(repoConnection);
        Collection<EsaResource> allResources = client.addEsasToMassive(files, UploadStrategy.DEFAULT_STRATEGY);

        // Test assets uploaded: tests follow.

        RepositoryConnectionList connectionList = new RepositoryConnectionList(repoConnection);

        Collection<EsaResource> ilanESAs = connectionList.getAllFeatures(LicenseType.ILAN);
        assertEquals("Wrong number of ILAN-licensed features", 1, ilanESAs.size());
        assertEquals("Wrong ILAN-licensed asset name", "Translated Name", ilanESAs.iterator().next().getName());

        Collection<EsaResource> iplaESAs = connectionList.getAllFeatures(LicenseType.IPLA);
        assertEquals("Wrong number of IPLA licensed features", 1, iplaESAs.size());
        assertEquals("Wrong IPLA-licensed asset name", "com.ibm.ws.test.simple", iplaESAs.iterator().next().getName());

        Collection<EsaResource> unspecESAs = connectionList.getAllFeatures(LicenseType.UNSPECIFIED);
        assertEquals("Wrong number of IPLA licensed features", 1, unspecESAs.size());
        assertEquals("Wrong UNSPECIFIED licensed asset name", "com.ibm.ws.test.with.required.fix", unspecESAs.iterator().next().getName());

    }

    @Test
    public void testTestLA_LI_andLicenseId() throws Exception {

        MassiveEsa client = new MassiveEsa(repoConnection);

        Collection<File> files = Collections.singletonList(new File(esaDir, "massiveEsaResourceTest1.esa"));
        Collection<EsaResource> esas = client.addEsasToMassive(files, UploadStrategy.DEFAULT_STRATEGY);
        for (EsaResource r : esas) {
            System.out.println("Uploaded esa with id " + r.getId());
        }

        EsaResource feature = (EsaResource) repoConnection.getResource(esas.iterator().next().getId());
        AttachmentResource res = feature.getLicenseAgreement(Locale.UK); // ie en_GB - not an exact match
        String enLAText = slurp(res);
        System.out.println("Slurped " + enLAText);

        System.out.println("Text = " + enLAText);
        assertEquals("English LA text bad", "This isn't a real license agreement", enLAText);

        res = feature.getLicenseInformation(Locale.US); // en_US is not exactly 'en' - this is deliberate
        String enLIText = slurp(res);
        assertEquals("English LI text bad", "This is a plain text file pretending to be some License Information in english.", enLIText);

        String licenseId = feature.getLicenseId().trim();
        assertEquals("Bad LicenseId", "http://www.ibm.com/licenses/L-JTHS-9C2M2H", licenseId);
    }

    @Test
    public void testSupersededBy() throws Throwable {
        File f = new File(esaDir, "com.ibm.websphere.appserver.appSecurity-1.0.esa");
        EsaResourceImpl resource = (EsaResourceImpl) uploadAsset(f);

        // Note this depends on a knowledge of the implementation of resource.getLinks() returning a list.
        // Which it currently does.
        List<Link> links = (List<Link>) resource.getLinks();

        assertLinkEquals(SUPERSEDES,
                         null,
                         null,
                         "name",
                         Arrays.asList(new String[] { "wlpInformation.supersededBy=appSecurity-1.0&wlpInformation.appliesToFilterInfo.minVersion.value=8.5.5.3" }),
                         links.get(2));

        assertLinkEquals(SUPERSEDED_BY,
                         null,
                         null,
                         "name",
                         Arrays.asList(new String[] { "wlpInformation.shortName=appSecurity-2.0&wlpInformation.appliesToFilterInfo.minVersion.value=8.5.5.3&type=com.ibm.websphere.Feature" }),
                         links.get(3));

        assertLinkEquals(SUPERSEDED_BY,
                         null,
                         " (optional)",
                         "name",
                         Arrays.asList(new String[] {
                                                     "wlpInformation.shortName=ldapRegistry-3.0&wlpInformation.appliesToFilterInfo.minVersion.value=8.5.5.3&type=com.ibm.websphere.Feature",
                                                     "wlpInformation.shortName=servlet-3.0&wlpInformation.appliesToFilterInfo.minVersion.value=8.5.5.3&type=com.ibm.websphere.Feature" }),
                         links.get(4));
        assertThat("supersededBy", resource.getSupersededBy(), containsInAnyOrder("appSecurity-2.0"));
        assertThat("supersededByOption", resource.getSupersededByOptional(), containsInAnyOrder("ldapRegistry-3.0", "servlet-3.0"));
    }

    @Test
    public void testSupersedes() throws Throwable {
        File f = new File(esaDir, "com.ibm.websphere.appserver.appSecurity-2.0.esa");
        EsaResourceImpl resource = (EsaResourceImpl) uploadAsset(f);

        // Note this depends on a knowledge of the implementation of resource.getLinks() returning a list.
        // Which it currently does.
        List<Link> links = (List<Link>) resource.getLinks();

        assertLinkEquals(SUPERSEDES,
                         null,
                         null,
                         "name",
                         Arrays.asList(new String[] { "wlpInformation.supersededBy=appSecurity-2.0&wlpInformation.appliesToFilterInfo.minVersion.value=8.5.5.3" }),
                         links.get(2));

        assertLinkEquals(SUPERSEDED_BY,
                         null,
                         null,
                         "name",
                         null,
                         links.get(3));

        assertLinkEquals(SUPERSEDED_BY,
                         null,
                         " (optional)",
                         "name",
                         null,
                         links.get(4));
        assertNull("supersededBy should be null", resource.getSupersededBy());
        assertNull("supersededByOptional should be null", resource.getSupersededByOptional());
    }

    @Test
    public void testSupersededByMultiple() throws Throwable {
        File f = new File(esaDir, "com.ibm.websphere.appserver.osgi.jpa-1.0.esa");
        EsaResourceImpl resource = (EsaResourceImpl) uploadAsset(f);

        // Note this depends on a knowledge of the implementation of resource.getLinks() returning a list.
        // Which it currently does.
        List<Link> links = (List<Link>) resource.getLinks();

        assertLinkEquals(SUPERSEDES,
                         null,
                         null,
                         "name",
                         Arrays.asList(new String[] { "wlpInformation.supersededBy=osgi.jpa-1.0&wlpInformation.appliesToFilterInfo.minVersion.value=2014.9.0.0" }),
                         links.get(2));

        assertLinkEquals(SUPERSEDED_BY,
                         null,
                         null,
                         "name",
                         Arrays.asList(new String[] {
                                                     "wlpInformation.shortName=jpa-2.0&wlpInformation.appliesToFilterInfo.minVersion.value=2014.9.0.0&type=com.ibm.websphere.Feature",
                                                     "wlpInformation.shortName=blueprint-1.0&wlpInformation.appliesToFilterInfo.minVersion.value=2014.9.0.0&type=com.ibm.websphere.Feature" }),
                         links.get(3));

        assertLinkEquals(SUPERSEDED_BY,
                         null,
                         " (optional)",
                         "name",
                         null,
                         links.get(4));

        assertThat("supersededBy", resource.getSupersededBy(), containsInAnyOrder("jpa-2.0", "blueprint-1.0"));
        assertNull("supersededByOptional should be null", resource.getSupersededByOptional());
    }

    @Test
    public void testNoSupersedesOrSuperseded() throws RepositoryException {
        File enablingEsa = new File(esaDir, "enabling4.esa");
        EsaResourceImpl resource = (EsaResourceImpl) uploadAsset(enablingEsa);

        // Note this depends on a knowledge of the implementation of resource.getLinks() returning a list.
        // Which it currently does.
        List<Link> links = (List<Link>) resource.getLinks();

        assertLinkEquals(SUPERSEDES,
                         null,
                         null,
                         "name",
                         null,
                         links.get(2));

        assertLinkEquals(SUPERSEDED_BY,
                         null,
                         null,
                         "name",
                         null,
                         links.get(3));

        assertLinkEquals(SUPERSEDED_BY,
                         null,
                         " (optional)",
                         "name",
                         null,
                         links.get(4));
        assertNull("supersededBy should be null", resource.getSupersededBy());
        assertNull("supersededByOptional should be null", resource.getSupersededByOptional());

    }

    private void assertLinkEquals(String expectedLabel,
                                  String expectedLinkLabelPrefix,
                                  String expectedLinkLabelSuffix,
                                  String expectedLinkLabelProperty,
                                  List<String> expectedQuery,
                                  Link actual) {

        assertEquals("linkLabel", expectedLabel, actual.getLabel());
        assertEquals("linkLabelPrefix", expectedLinkLabelPrefix, actual.getLinkLabelPrefix());
        assertEquals("linkLabelSuffix", expectedLinkLabelSuffix, actual.getLinkLabelSuffix());
        assertEquals("linkLabelProperty", expectedLinkLabelProperty, actual.getLinkLabelProperty());
        if (expectedQuery == null) {
            assertNull("query", actual.getQuery());
        } else {
            assertThat("query", actual.getQuery(), containsInAnyOrder(expectedQuery.toArray()));
        }
    }

    /**
     * Tests that the uploader won't try to upload something that isn't an ESA
     *
     * @throws Exception
     */
    @Test
    public void testNonEsa() throws Exception {
        File zip = new File(esaDir, "simple.zip");
        assertFalse("Should only be able to upload ESAs", massiveEsa.canUploadFile(zip));
    }

    /**
     * Tests that the uploader will read in and use the "install" visibility type
     *
     * @throws Throwable
     */
    @Test
    public void testInstallVisibility() throws Throwable {
        File esaFile = new File(esaDir, "simple.with.install.visibility.esa");
        EsaResource uploadedEsa = uploadAsset(esaFile);
        assertEquals("Visibility should match source file", Visibility.INSTALL, uploadedEsa.getVisibility());
        assertEquals("\"install\" should be hidden", DisplayPolicy.HIDDEN, uploadedEsa.getDisplayPolicy());
        assertEquals("\"install\" should be hidden", DisplayPolicy.HIDDEN, reflectiveCallNoPrimitives(uploadedEsa, "getWebDisplayPolicy", (Object[]) null));
    }

    private String slurp(AttachmentResource res) throws Exception {
        InputStream is = res.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        try {
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
        } finally {
            try {
                is.close();
            } catch (Exception x) {
            }
        }
        return baos.toString("UTF-8");
    }

    /**
     * Test MassiveEsa code that determines whether a feature is a beta feature and should therefore
     * be hidden.
     */
    @Test
    public void testIsBeta() {
        //Test a null applies to
        String appliesTo = null;
        boolean result = MassiveEsa.isBeta(appliesTo);
        assertEquals("Null appliesTo should not be a beta", false, result);

        //Test a valid beta appliesTo
        appliesTo = "com.ibm.websphere.appserver; productVersion=2014.8.0.0; productInstallType=Archive";
        result = MassiveEsa.isBeta(appliesTo);
        assertEquals("com.ibm.websphere.appserver; productVersion=2014.8.0.0; productInstallType=Archive should be a valid beta appliesTo", true, result);

        //Test a non-beta appliesTo
        appliesTo = "com.ibm.websphere.appserver; productVersion=8.5.5.3";
        result = MassiveEsa.isBeta(appliesTo);
        assertEquals("com.ibm.websphere.appserver; productVersion=8.5.5.3 should not be a valid beta appliesTo", false, result);
    }

    /**
     * Install a Beta feature and ensure that it's webDisplayPolicy and displayPolicy are set to
     * HIDDEN
     *
     * @throws RepositoryException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     */
    @Test
    public void testUploadBetaFeatureVisibilityCheck() throws RepositoryException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        // have chosen to install the jaxrs-2.0 beta feature purely because it
        // was the smallest beta feature at the time
        File sourceEsa = new File(esaDir, "beta/com.ibm.websphere.appserver.jaxrs-2.0.esa");
        if (!sourceEsa.exists()) {
            fail("Whoa ! Where'd the file go ? " + sourceEsa.getAbsolutePath());
        }

        MassiveEsa client = new MassiveEsa(repoConnection);

        Collection<File> files = new ArrayList<File>();
        files.add(sourceEsa);

        Collection<EsaResource> esas = client.addEsasToMassive(files, UploadStrategy.DEFAULT_STRATEGY);
        assertEquals("Expected 1 asset to have been uploaded", 1, esas.size());

        for (EsaResource esa : esas) {
            System.out.println("Uploaded esa with id " + esa.getId());

            Asset ass = getAsset(esa);
            DisplayPolicy dp1 = ass.getWlpInformation().getWebDisplayPolicy();
            assertEquals("webDisplayPolicy on a beta feature should be HIDDEN", DisplayPolicy.HIDDEN, dp1);

            DisplayPolicy dp2 = ass.getWlpInformation().getDisplayPolicy();
            assertEquals("displayPolicy on a beta feature should be VISIBLE", DisplayPolicy.VISIBLE, dp2);
        }

    }

    /**
     * Test that the extend package is uploaded correctly, specifically that short name and
     * description are set in response to defect 145768.
     *
     * @throws Throwable
     */
    @Test
    public void testExtendedPackage() throws Throwable {
        File esaFile = new File(esaDir, "com.ibm.websphere.appserver.extendedPackage-1.0.esa");
        EsaResource uploadedEsa = uploadAsset(esaFile);
        assertEquals("Short name should be set", "extendedPackage-1.0", uploadedEsa.getShortName());
        assertNotNull("Description should be set", uploadedEsa.getShortDescription());
    }

    @Test
    /**
     * Based on a small set of features make searches to see whether the correct results match each query
     * @throws Throwable
     */
    public void testSimpleFinds() throws Throwable {
        File dir8553 = new File(esaDir + "/find/8553");
        System.out.println("dir8553=" + dir8553);
        File dir8554 = new File(esaDir + "/find/8554");
        System.out.println("dir8554=" + dir8554);

        // 8553 uploads
        EsaResourceImpl resource = (EsaResourceImpl) uploadAsset(new File(dir8553, "com.ibm.websphere.appserver.jsp-2.2.esa"));
        resource.approve();
        resource = (EsaResourceImpl) uploadAsset(new File(dir8553, "com.ibm.websphere.appserver.appSecurity-1.0.esa"));
        resource.approve();
        resource = (EsaResourceImpl) uploadAsset(new File(dir8553, "com.ibm.websphere.appserver.appSecurity-2.0.esa"));
        resource.approve();

        // 8554 uploads
        resource = (EsaResourceImpl) uploadAsset(new File(dir8554, "com.ibm.websphere.appserver.adminCenter-1.0.esa"));
        resource.approve();
        resource = (EsaResourceImpl) uploadAsset(new File(dir8554, "com.ibm.websphere.appserver.jsp-2.2.esa"));
        resource.approve();
        resource = (EsaResourceImpl) uploadAsset(new File(dir8554, "com.ibm.websphere.appserver.appSecurity-1.0.esa"));
        resource.approve();
        resource = (EsaResourceImpl) uploadAsset(new File(dir8554, "com.ibm.websphere.appserver.appSecurity-2.0.esa"));
        resource.approve();

        repo.refreshTextIndex(resource.getId());

        String nl = System.getProperty("line.separator");

        //Create ProductDefinition to match against using: productId, version, installType, licenseType, edition
        ProductDefinition prod = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.4", "Archive", "ILAN", "BASE");

        // Check how many hits we get for Admin
        System.out.println(nl + "Looking for Admin should get 1 matches");
        RepositoryConnectionList connectionList = new RepositoryConnectionList(repoConnection);
        Collection<EsaResource> esas = connectionList.findMatchingEsas("Admin", prod, Visibility.PUBLIC);
        for (EsaResource esa : esas) {
            System.out.println("found feature: " + esa.getName() + ", id=" + esa.getId());
        }
        assertEquals("Admin Matches", 1, esas.size());

        // Check how many hits we get for adminCenter-1.0 (the search term is split by the minus and
        // is interpreted as adminCenter and not 1.0).  Not very useful but that what it gives.
        System.out.println(nl + "Looking for adminCenter-1.0 should get 3 matches");
        esas = connectionList.findMatchingEsas("adminCenter-1.0", prod, Visibility.PUBLIC);
        for (EsaResource esa : esas) {
            System.out.println("found feature: " + esa.getName() + ", id=" + esa.getId());
        }
        assertEquals("adminCenter-1.0 matches", 3, esas.size());

        // Check how many hits we get for security
        System.out.println(nl + "Looking for security should get 2 matches");
        esas = connectionList.findMatchingEsas("security", prod, Visibility.PUBLIC);
        for (EsaResource esa : esas) {
            System.out.println("found feature: " + esa.getName() + ", id=" + esa.getId());
        }
        assertEquals("Security Matches", 2, esas.size());

        // Check how many hits we get for "security-1.0"
        System.out.println(nl + "Looking for \"security-1.0\" should get 1 matches");
        esas = connectionList.findMatchingEsas("\"security-1.0\"", prod, Visibility.PUBLIC);
        for (EsaResource esa : esas) {
            System.out.println("found feature: " + esa.getName() + ", id=" + esa.getId());
        }
        assertEquals("\"Security-1.0\" matches", 1, esas.size());

        // Check how many hits we get for "jsp-2.2"
        System.out.println(nl + "Looking for \"jsp-2.2\" should get 1 matches");
        esas = connectionList.findMatchingEsas("\"jsp-2.2\"", prod, Visibility.PUBLIC);
        for (EsaResource esa : esas) {
            System.out.println("found feature: " + esa.getName() + ", id=" + esa.getId());
        }
        assertEquals("\"jsp-2.2\" matches", 1, esas.size());
    }

    /**
     * Verifies that it is possible to upload an asset that has an LA header but not an LI header to
     * LARS.
     */
    @Test
    public void testFeatureWithNoLI() throws Throwable {
        File featureFile = new File(esaDir, "com.ibm.websphere.appserver.blueprint-1.0.esa");

        EsaResourceImpl featureResource = (EsaResourceImpl) uploadAsset(featureFile);
        featureResource.approve();
    }

    /**
     * Verify that the LARS uploader doesn't complain if a feature has an odd combination of license
     * headers
     */
    @Test
    public void testFeatureWithLALIFeatureTerms() throws Throwable {
        File featureFile = new File(esaDir, "li-la-header-featureterms.esa");

        EsaResourceImpl featureResource = (EsaResourceImpl) uploadAsset(featureFile);
        featureResource.approve();
    }

    @Test
    public void testFeatureWithNoSymbolicNameAttributes() throws Throwable {
        File featureFile = new File(esaDir, "no-symbolic-name-attributes.esa");

        EsaResourceImpl featureResource = (EsaResourceImpl) uploadAsset(featureFile);
        featureResource.approve();
    }

}
