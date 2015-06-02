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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipException;

import org.apache.aries.util.VersionRange;
import org.osgi.framework.Version;

import com.ibm.ws.massive.LoginInfo;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.RepositoryException;
import com.ibm.ws.massive.esa.ManifestHeaderProcessor.GenericMetadata;
import com.ibm.ws.massive.esa.internal.EsaManifest;
import com.ibm.ws.massive.resources.AppliesToProcessor;
import com.ibm.ws.massive.resources.EsaResource;
import com.ibm.ws.massive.resources.EsaResource.InstallPolicy;
import com.ibm.ws.massive.resources.ImageDetails;
import com.ibm.ws.massive.resources.MassiveResource.AttachmentResource;
import com.ibm.ws.massive.resources.MassiveResource.AttachmentType;
import com.ibm.ws.massive.resources.MassiveResource.DisplayPolicy;
import com.ibm.ws.massive.resources.MassiveResource.LicenseType;
import com.ibm.ws.massive.resources.MassiveResource.Visibility;
import com.ibm.ws.massive.resources.Provider;
import com.ibm.ws.massive.resources.RepositoryResourceCreationException;
import com.ibm.ws.massive.resources.RepositoryResourceException;
import com.ibm.ws.massive.resources.RepositoryResourceUpdateException;
import com.ibm.ws.massive.resources.UploadStrategy;
import com.ibm.ws.massive.sa.client.model.Asset;
import com.ibm.ws.massive.upload.RepositoryArchiveEntryNotFoundException;
import com.ibm.ws.massive.upload.RepositoryArchiveIOException;
import com.ibm.ws.massive.upload.RepositoryArchiveInvalidEntryException;
import com.ibm.ws.massive.upload.RepositoryUploader;
import com.ibm.ws.massive.upload.internal.MassiveUploader;

/**
 * <p>
 * This class contains methods for working with ESAs inside MaaSive.
 * </p>
 */
public class MassiveEsa extends MassiveUploader implements RepositoryUploader<EsaResource> {

    /**  */
    private static final String JAVA_FILTER_KEY = "JavaSE";

    /**  */
    private static final String VERSION_FILTER_KEY = "version";

    /**  */
    private static final String OSGI_EE_NAMESPACE_ID = "osgi.ee";

    /**  */
    private static final String REQUIRE_CAPABILITY_HEADER_NAME = "Require-Capability";

    /** Map of symbolic name to asset to make finding other features easy */
    private final Map<EsaIdentifier, EsaResource> allFeatures;

    private final Logger logger = Logger.getLogger(MassiveEsa.class.getName());

    /**
     * Construct a new instance and load all of the existing features inside MaaSive.
     *
     * @param userId The userId to use to connect to Massive
     * @param password The password to use to connect to Massive
     * @param apiKey The API key to use to connect to Massive
     * @throws RepositoryException
     */
    public MassiveEsa(LoginInfoEntry loginInfo)
        throws RepositoryException {
        super(loginInfo);

        /*
         * Find all of the features that are already in MaaSive so we can set the enabled
         * information for them
         */
        Collection<EsaResource> allEsas = EsaResource.getAllFeatures(new LoginInfo(_loginInfoResource));
        this.allFeatures = new HashMap<EsaIdentifier, EsaResource>();
        for (EsaResource res : allEsas) {
            // All features must provide a single symbolic name so no
            // null/size check
            logger.log(Level.FINE, "Resource features " + res.getProvideFeature());
            EsaIdentifier identifier = new EsaIdentifier(res.getProvideFeature(), res.getVersion(), res.getAppliesTo());
            allFeatures.put(identifier, res);
        }
    }

    /**
     * This method will add a collection of ESAs into MaaSive
     *
     * @param esas The ESAs to add
     * @return the new {@link EsaResource}s added to massive (will not included any resources that
     *         were modified as a result of this operation)
     * @throws ZipException
     * @throws RepositoryResourceCreationException
     * @throws RepositoryResourceUpdateException
     */
    public Collection<EsaResource> addEsasToMassive(Collection<File> esas, UploadStrategy strategy) throws RepositoryException {
        Collection<EsaResource> resources = new HashSet<EsaResource>();
        for (File esa : esas) {
            EsaResource resource = uploadFile(esa, strategy, null);
            resources.add(resource);
        }

        return resources;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.massive.upload.RepositoryUploader#canUploadFile(java.io.File)
     */
    @Override
    public boolean canUploadFile(File assetFile) {
        return assetFile.getName().endsWith(".esa");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.massive.upload.RepositoryUploader#uploadFile(java.io.File,
     * com.ibm.ws.massive.resources.UploadStrategy)
     */
    @Override
    public EsaResource uploadFile(File esa, UploadStrategy strategy, String contentUrl) throws RepositoryException {

        ArtifactMetadata artifactMetadata = explodeArtifact(esa);

        // Read the meta data from the esa
        EsaManifest feature;
        try {
            feature = EsaManifest
                    .constructInstance(esa);
        } catch (IOException e) {
            throw new RepositoryArchiveIOException(e.getMessage(), esa, e);
        }

        /*
         * First see if we already have this feature in MaaSive, note this means we can only have
         * one version of the asset in MaaSive at a time
         */
        EsaResource resource = new EsaResource(_loginInfoResource);
        String symbolicName = feature.getSymbolicName();
        String version = feature.getVersion().toString();
        String appliesTo = feature.getHeader("IBM-AppliesTo");
        EsaIdentifier identifier = new EsaIdentifier(symbolicName, version, appliesTo);

        // Massive assets are always English, find the best name
        String subsystemName = feature.getHeader("Subsystem-Name",
                                                 Locale.ENGLISH);
        String shortName = feature.getIbmShortName();
        String metadataName = artifactMetadata != null ? artifactMetadata.getName() : null;
        final String name;

        /*
         * We want to be able to override the name in the built ESA with a value supplied in the
         * metadata so use this in preference of what is in the ESA so that we can correct any typos
         * post-GM
         */
        if (metadataName != null && !metadataName.isEmpty()) {
            name = metadataName;
        } else if (subsystemName != null && !subsystemName.isEmpty()) {
            name = subsystemName;
        } else if (shortName != null && !shortName.isEmpty()) {
            name = shortName;
        } else {
            // symbolic name is always set
            name = symbolicName;
        }

        resource.setName(name);
        String shortDescription = null;
        if (artifactMetadata != null) {
            shortDescription = artifactMetadata.getShortDescription();
            resource.setDescription(artifactMetadata.getLongDescription());
        }
        if (shortDescription == null) {
            shortDescription = feature.getHeader("Subsystem-Description", Locale.ENGLISH);
        }
        resource.setShortDescription(shortDescription);
        resource.setVersion(version);

        //Add icon files
        processIcons(esa, feature, resource);

        String provider = feature.getHeader("Subsystem-Vendor");
        if (provider != null && !provider.isEmpty()) {
            Provider massiveProvider = new Provider();
            massiveProvider.setName(provider);
            if ("IBM".equals(provider)) {
                massiveProvider.setUrl("http://www.ibm.com");
            }
            resource.setProvider(massiveProvider);
        } else {
            // Massive breaks completely if the provider is not filled in so
            // make sure it is!
            throw new InvalidParameterException("Subsystem-Vendor must be set in the manifest headers");
        }

        // Add custom attributes for WLP
        resource.setProvideFeature(symbolicName);
        resource.setAppliesTo(feature.getHeader("IBM-AppliesTo"));
        Visibility visibility = feature.getVisibility();
        resource.setVisibility(visibility);

        /*
         * Two things affect the display policy - the visibility and the install policy. If a
         * private auto feature is set to manual install we need to make it visible so people know
         * that it exists and can be installed
         */
        DisplayPolicy displayPolicy;
        DisplayPolicy webDisplayPolicy;

        if (visibility == Visibility.PUBLIC) {
            displayPolicy = DisplayPolicy.VISIBLE;
            webDisplayPolicy = DisplayPolicy.VISIBLE;
        } else {
            displayPolicy = DisplayPolicy.HIDDEN;
            webDisplayPolicy = DisplayPolicy.HIDDEN;
        }

        if (feature.isAutoFeature()) {
            resource.setProvisionCapability(feature.getHeader("IBM-Provision-Capability"));
            String IBMInstallPolicy = feature.getHeader("IBM-Install-Policy");

            // Default InstallPolicy is set to MANUAL
            InstallPolicy installPolicy;
            if (IBMInstallPolicy != null && ("when-satisfied".equals(IBMInstallPolicy))) {
                installPolicy = InstallPolicy.WHEN_SATISFIED;
            } else {
                installPolicy = InstallPolicy.MANUAL;
                // As discussed above set the display policy to visible for any manual auto features
                displayPolicy = DisplayPolicy.VISIBLE;
                webDisplayPolicy = DisplayPolicy.VISIBLE;
            }
            resource.setInstallPolicy(installPolicy);
        }

        // if we are dealing with a beta feature hide it otherwise apply the
        // display policies from above
        if (isBeta(resource.getAppliesTo())) {
            resource.setWebDisplayPolicy(DisplayPolicy.HIDDEN);
        } else {
            resource.setWebDisplayPolicy(webDisplayPolicy);
        }

        // Always set displayPolicy
        resource.setDisplayPolicy(displayPolicy);

        // handle required iFixes
        String requiredFixes = feature.getHeader("IBM-Require-Fix");
        if (requiredFixes != null && !requiredFixes.isEmpty()) {
            String[] fixes = requiredFixes.split(",");
            for (String fix : fixes) {
                fix = fix.trim();
                if (!fix.isEmpty()) {
                    resource.addRequireFix(fix);
                }
            }
        }

        resource.setShortName(shortName);

        // Calculate which features this relies on
        for (String requiredFeature : feature.getRequiredFeatures()) {
            resource.addRequireFeature(requiredFeature);
        }

        // feature.supersededBy is a comma-separated list of shortNames. Add
        // each of the elements to either supersededBy or supersededByOptional.
        String supersededBy = feature.getSupersededBy();
        if (supersededBy != null && !supersededBy.trim().isEmpty()) {
            String[] supersededByArray = supersededBy.split(",");
            for (String f : supersededByArray) {
                // If one of the elements is surrounded by [square brackets] then we
                // strip the brackets off and treat it as optional
                if (f.startsWith("[")) {
                    f = f.substring(1, f.length() - 1);
                    resource.addSupersededByOptional(f);
                } else {
                    resource.addSupersededBy(f);
                }
            }
        }

        String attachmentName = symbolicName + ".esa";
        addContent(resource, esa, attachmentName, artifactMetadata, contentUrl);

        // Set the license type if we're using the feature terms agreement so that we know later
        // that there won't be a license information file.
        String subsystemLicense = feature.getHeader("Subsystem-License");
        if (subsystemLicense != null && subsystemLicense.equals("http://www.ibm.com/licenses/wlp-featureterms-v1")) {
            resource.setLicenseType(LicenseType.UNSPECIFIED);
        }

        if (artifactMetadata != null) {
            attachLicenseData(artifactMetadata, resource);
        }

        // Now look for LI, LA files inside the .esa
        // We expect to find them in wlp/lafiles/LI_{Locale} or /LA_{Locale}
        try {
            processLAandLI(esa, resource, feature);
        } catch (IOException e) {
            throw new RepositoryArchiveIOException(e.getMessage(), esa, e);
        }
        resource.setLicenseId(feature.getHeader("Subsystem-License"));

        setJavaRequirements(esa, resource);

        // Publish to massive
        try {
            resource.uploadToMassive(strategy);
        } catch (RepositoryException re) {
            throw re;
        }
        this.allFeatures.put(identifier, resource);
        return resource;
    }

    protected static boolean isBeta(String appliesTo) {
        // Use the appliesTo string to determine whether a feature is a Beta or a regular feature.
        // Beta features are of the format:
        // "com.ibm.websphere.appserver; productVersion=2014.8.0.0; productInstallType=Archive",
        if (appliesTo == null) {
            return false;
        } else {
            String regex = ".*productVersion=" + AppliesToProcessor.BETA_REGEX;
            boolean matches = appliesTo.matches(regex);
            return matches;
        }
    }

    private void processIcons(File esa, EsaManifest feature, EsaResource resource) throws RepositoryException {
        //checking icon file
        int size = 0;
        String current = "";
        String sizeString = "";
        String iconName = "";
        ImageDetails details = null;
        String subsystemIcon = feature.getHeader("Subsystem-Icon");

        if (subsystemIcon != null) {
            subsystemIcon = subsystemIcon.replaceAll("\\s", "");

            StringTokenizer s = new StringTokenizer(subsystemIcon, ",");
            while (s.hasMoreTokens()) {
                current = s.nextToken();

                if (current.contains(";")) { //if the icon has an associated size
                    StringTokenizer t = new StringTokenizer(current, ";");
                    while (t.hasMoreTokens()) {
                        sizeString = t.nextToken();

                        if (sizeString.contains("size=")) {
                            String sizes[] = sizeString.split("size=");
                            size = Integer.parseInt(sizes[sizes.length - 1]);
                            details = new ImageDetails();
                            details.setWidth(size);
                            details.setHeight(size);
                        } else {
                            iconName = sizeString;
                        }
                    }

                } else {
                    iconName = current;
                }

                File icon = this.extractFileFromArchive(esa.getAbsolutePath(), iconName).getExtractedFile();
                if (icon.exists()) {
                    AttachmentResource at = resource.addAttachment(icon, AttachmentType.THUMBNAIL);
                    if (details != null) {
                        at.setImageDetails(details);
                    }
                } else {
                    throw new RepositoryArchiveEntryNotFoundException("Icon does not exist", esa, iconName);
                }
                details = null;
            }
        }
    }

    /**
     * Utility method to delete all the features in MaaSive.
     *
     * @throws IOException
     */
    public void deleteAllFeatures() throws RepositoryResourceException {
        Iterator<EsaResource> featureIterator = this.allFeatures
                .values().iterator();
        while (featureIterator.hasNext()) {
            EsaResource featureResource = featureIterator.next();
            logger.log(Level.INFO, "Deleting " + featureResource.get_id());
            featureResource.delete();
            featureIterator.remove();
        }
    }

    /**
     * Utility method to delete certain features in MaaSive. All versions of the feature will be
     * deleted.
     *
     * @param featureNames The feature symbolic names or IBM short names to delete
     * @throws RepositoryResourceCreationException
     * @throws RepositoryResourceUpdateException
     */
    public void deleteFeatures(Collection<String> featureNames)
            throws IOException, RepositoryException {
        Iterator<EsaResource> featureIterator = this.allFeatures
                .values().iterator();

        /*
         * When we delete a feature we will also need to remove any enabling information that
         * reference it so keep track of which other assets we will need to update at the end.
         */
        //Collection<EsaResource> assetsForUpdating = new HashSet<EsaResource>();
        while (featureIterator.hasNext()) {
            EsaResource featureResource = featureIterator.next();
            String symbolicName = featureResource.getProvideFeature();
            if (featureNames.contains(symbolicName)
                || featureNames.contains(featureResource.getShortName())) {
                //   removeEnablingInformationForFeature(featureResource, assetsForUpdating);
                featureResource.delete();
                featureIterator.remove();
            }
        }

        /*
         * There is an edge case where the removeEnablingInformationForFeature method is over
         * zealous at removing enabling information so run through the add just in case it needs
         * adding back in. This will also process all of the assets that we need to update in
         * Massive. First make sure we haven't deleted the assets we thought needed updating!
         */
        //assetsForUpdating.retainAll(this.allFeatures.values());
        //  addEnablingInformation(assetsForUpdating);

    }

    /**
     * Returns the first feature with the supplied symbolic name. Unlike {@link #getAllFeatures()}
     * this returns the complete feature with attachments.
     *
     * @param symbolicName The symbolic name of the feature to get
     * @return The Asset representing the feature in MaaSive or <code>null</code> if one doesn't
     *         exist
     * @throws IOException
     * @throws RepositoryException
     */
    public EsaResource getFeature(String symbolicName) throws IOException, RepositoryException {
        EsaResource summaryResource = findFeature(symbolicName);
        if (summaryResource != null) {
            return EsaResource.getEsa(_loginInfoResource, summaryResource.get_id());
        } else {
            return null;
        }
    }

    /**
     * Returns the feature with the supplied symbolic name and version. Unlike
     * {@link #getAllFeatures()} this returns the complete feature with attachments.
     *
     * @param symbolicName The symbolic name of the feature to get
     * @param version The version of the feature to get
     * @return The EsaResource representing the feature in Massive or <code>null</code> if one
     *         doesn't exist
     * @throws IOException
     * @throws RepositoryException
     */
    public EsaResource getFeature(String symbolicName, String version, String appliesTo) throws IOException, RepositoryException {
        EsaResource summaryResource = this.allFeatures.get(new EsaIdentifier(symbolicName, version, appliesTo));
        if (summaryResource != null) {
            return EsaResource.getEsa(_loginInfoResource, summaryResource.get_id());
        } else {
            return null;
        }
    }

    /**
     * Returns all features inside Massive. Note that these are just the summary of features
     * returned from Massive's GET all request. Most of the fields will be filled in but the
     * attachments will not be. In order to get the attachments as well call
     * {@link #getFeature(String)}.
     *
     * @return A collection of {@link Asset}s representing the features in Massive or an empty
     *         collection if none exists
     */
    public Collection<EsaResource> getAllFeatures() {
        /*
         * A map's values() collection is backed by the map so if someone clears it we'll be in
         * trouble... stop them!
         */
        return Collections.unmodifiableCollection(this.allFeatures.values());
    }

    /**
     * Finds a feature with the provided symbolic name
     *
     * @param symbolicName The symbolic name to look for, must not be <code>null</code>
     * @return the first EsaResource with the provided symbolicName that is found or
     *         <code>null</code> if none is found
     */
    private EsaResource findFeature(String symbolicName) {
        for (EsaResource esa : this.allFeatures.values()) {
            if (symbolicName.equals(esa.getProvideFeature())) {
                return esa;
            }
        }
        return null;
    }

    @Override
    protected void checkRequiredProperties(ArtifactMetadata artifact) throws RepositoryArchiveInvalidEntryException {
        checkPropertySet(PROP_DESCRIPTION, artifact);
    }

    /**
     * This class holds identification information about an ESA to uniquely identify it and
     * implements equals and hash code so it can be used as a lookup key.
     */
    private static class EsaIdentifier {

        private final String symbolicName;
        private final String version;
        private final String appliesTo;

        /**
         * @param symbolicName The symbolic name of the ESA
         * @param version The version of the ESA
         */
        public EsaIdentifier(String symbolicName, String version, String appliesTo) {
            this.symbolicName = symbolicName;
            this.version = version;
            this.appliesTo = appliesTo;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((symbolicName == null) ? 0 : symbolicName.hashCode());
            result = prime * result + ((version == null) ? 0 : version.hashCode());
            result = prime * result + ((appliesTo == null) ? 0 : appliesTo.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            EsaIdentifier other = (EsaIdentifier) obj;
            if (symbolicName == null) {
                if (other.symbolicName != null)
                    return false;
            } else if (!symbolicName.equals(other.symbolicName))
                return false;
            if (version == null) {
                if (other.version != null)
                    return false;
            } else if (!version.equals(other.version))
                return false;
            if (appliesTo == null) {
                if (other.appliesTo != null)
                    return false;
            } else if (!appliesTo.equals(other.appliesTo))
                return false;
            return true;
        }

    }

    /**
     * Look in the esa for bundles with particular java version requirements. Create an aggregate
     * requirement of the esa as a whole, and write the data into the supplied resource
     *
     * @param esa
     * @param resource
     * @throws RepositoryException If there are any IOExceptions reading the esa, or if the the
     *             bundles have conflicting Java version requirements.
     */
    private static void setJavaRequirements(File esa, EsaResource resource) throws RepositoryException {

        Map<String, String> bundleRequirements = new HashMap<String, String>();
        Path zipfile = esa.toPath();
        VersionRange esaRange = null;

        try (final FileSystem zipSystem = FileSystems.newFileSystem(zipfile, null)) {

            class BundleFinder extends SimpleFileVisitor<Path> {
                ArrayList<Path> bundles = new ArrayList<Path>();
                // Bundles should be jars in the root of the zip
                PathMatcher bundleMatcher = zipSystem.getPathMatcher("glob:/*.jar");

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
                    if (bundleMatcher.matches(file)) {
                        bundles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            }

            Iterable<Path> roots = zipSystem.getRootDirectories();
            BundleFinder finder = new BundleFinder();
            for (Path root : roots) {
                // Bundles should be in the root of the zip, so depth is 1
                Files.walkFileTree(root, new HashSet<FileVisitOption>(), 1, finder);
            }

            for (Path bundle : finder.bundles) {

                // Need to extract the bundles to read their manifest, can't find a way to do this in place.
                Path extractedJar = Files.createTempFile(null, ".jar", new FileAttribute[0]);
                extractedJar.toFile().deleteOnExit();
                Files.copy(bundle, extractedJar, StandardCopyOption.REPLACE_EXISTING);

                Manifest bundleJarManifest = null;
                try (JarFile bundleJar = new JarFile(extractedJar.toFile())) {
                    bundleJarManifest = bundleJar.getManifest();
                }
                Attributes bundleManifestAttrs = bundleJarManifest.getMainAttributes();
                String requireCapabilityAttr = bundleManifestAttrs.getValue(REQUIRE_CAPABILITY_HEADER_NAME);
                if (requireCapabilityAttr == null) {
                    continue;
                }

                // The Require-Capability attribute will look a little like this:
                // Require-Capability: osgi.ee; filter:="(&(osgi.ee=JavaSE)(version>=1.7))"
                List<GenericMetadata> requirementMetadata = ManifestHeaderProcessor.parseRequirementString(requireCapabilityAttr);
                GenericMetadata eeVersionMetadata = null;
                for (GenericMetadata metaData : requirementMetadata) {
                    if (metaData.getNamespace().equals(OSGI_EE_NAMESPACE_ID)) {
                        eeVersionMetadata = metaData;
                        break;
                    }
                }

                if (eeVersionMetadata == null) {
                    // No version requirements, go to the next bundle
                    continue;
                }

                Map<String, String> dirs = eeVersionMetadata.getDirectives();
                for (Entry<String, String> directive : dirs.entrySet()) {
                    String key = directive.getKey();
                    if (!key.equals("filter")) {
                        continue;
                    }

                    Map<String, String> filter = null;
                    filter = ManifestHeaderProcessor.parseFilter(dirs.get(key));

                    // The interesting filter should contain osgi.ee=JavaSE and version=XX
                    if (!(filter.containsKey(OSGI_EE_NAMESPACE_ID) && filter.get(OSGI_EE_NAMESPACE_ID).equals(JAVA_FILTER_KEY)
                    && filter.containsKey(VERSION_FILTER_KEY))) {
                        continue; // Uninteresting filter
                    }

                    // Store the raw filter to add to the resource later.
                    bundleRequirements.put(bundle.getFileName().toString(), directive.getValue());

                    VersionRange range = ManifestHeaderProcessor.parseVersionRange(filter.get(VERSION_FILTER_KEY));
                    if (esaRange == null) {
                        esaRange = range;
                    } else {
                        VersionRange newRange = range.intersect(esaRange);
                        if (newRange == null) {
                            // VersionRange.intersect returns null if the ranges are incompatible
                            throw new RepositoryException("ESA " + zipfile.getFileName() + " is invalid, two bundles " +
                                                          "require incompatible JavaSE versions");
                        }
                        esaRange = newRange;
                    }

                    // Assume there is only one Java version filter, so stop looking
                    break;
                }

            }

        } catch (IOException e) {
            // Any IOException means that the version info isn't reliable, so only thing to do is ditch out.
            throw new RepositoryArchiveIOException(e.getMessage(), esa, e);
        }

        ArrayList<String> rawRequirements = new ArrayList<String>();
        for (Entry<String, String> bundleRequirement : bundleRequirements.entrySet()) {
            rawRequirements.add(bundleRequirement.getKey() + ": " + bundleRequirement.getValue());
        }
        if (rawRequirements.size() == 0) {
            rawRequirements = null;
        }

        String minimum = null;
        String maximum = null;
        if (esaRange != null) {
            Version minimumVersion = esaRange.getMinimumVersion();
            if (minimumVersion != null) {
                minimum = minimumVersion.toString();
            }
            Version maximumVersion = esaRange.getMaximumVersion();
            if (maximumVersion != null) {
                maximum = maximumVersion.toString();
            }
        }

        validateJavaVersionRange(esaRange);

        resource.setJavaSEVersionRequirements(minimum, maximum, rawRequirements);

    }

    /**
     * Check that the version range in this ESA is acceptable. Acceptable is defined by what the
     * tooling can deal with, and what is currently known about Liberty.
     *
     * @param range
     * @throws RepositoryException if the version range is unexpected
     */
    private static void validateJavaVersionRange(VersionRange range) throws RepositoryException {
        if (range == null) {
            // This is fine. The ESA is implicitly valid on whatever liberty supports.
            return;
        }
        Version max = range.getMaximumVersion();
        Version min = range.getMinimumVersion();
        if (max != null) {
            // If this is the case then the ESA should have specified an exact required version.
            // If the min version is either not specified or the min version != max version, then
            // treat it as badly formed, as a range is not currently expected, and there is no logic to
            // handle displaying an ESA like this.
            if (min == null || !min.equals(max)) {
                throw new RepositoryException("Unexpected upper bound to Java version range");
            }
        }

        if (min == null) {
            throw new RepositoryException("Lower bound to Java version range shouldn't be empty");
        }

        Version version6 = new Version(1, 6, 0);
        Version version7 = new Version(1, 7, 0);
        if (min.equals(version6) || min.equals(version7)) {
            return;
        }

        throw new RepositoryException("Lower bound to Java version range is expected to be either Java 6 or Java 7."
                                      + " Actually was " + min);

    }

}
