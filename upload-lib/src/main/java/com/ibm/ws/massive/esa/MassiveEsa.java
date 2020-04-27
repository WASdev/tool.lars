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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.ZipException;

import com.ibm.ws.massive.esa.internal.EsaManifest;
import com.ibm.ws.massive.upload.RepositoryArchiveEntryNotFoundException;
import com.ibm.ws.massive.upload.RepositoryArchiveIOException;
import com.ibm.ws.massive.upload.RepositoryArchiveInvalidEntryException;
import com.ibm.ws.massive.upload.RepositoryUploader;
import com.ibm.ws.massive.upload.internal.MassiveUploader;
import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.common.enums.InstallPolicy;
import com.ibm.ws.repository.common.enums.LicenseType;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.exceptions.RepositoryResourceCreationException;
import com.ibm.ws.repository.exceptions.RepositoryResourceUpdateException;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.internal.AppliesToProcessor;
import com.ibm.ws.repository.resources.writeable.AttachmentResourceWritable;
import com.ibm.ws.repository.resources.writeable.EsaResourceWritable;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;
import com.ibm.ws.repository.strategies.writeable.UploadStrategy;

/**
 * <p>
 * This class contains methods for working with ESAs inside MaaSive.
 * </p>
 */
public class MassiveEsa extends MassiveUploader implements RepositoryUploader<EsaResourceWritable> {

    /**
     * Construct a new instance and load all of the existing features inside MaaSive.
     *
     * @param userId The userId to use to connect to Massive
     * @param password The password to use to connect to Massive
     * @param apiKey The API key to use to connect to Massive
     * @throws RepositoryException
     */
    public MassiveEsa(RepositoryConnection repoConnection) throws RepositoryException {
        super(repoConnection);
    }

    /**
     * This method will add a collection of ESAs into MaaSive
     *
     * @param esas The ESAs to add
     * @return the new {@link EsaResource}s added to massive (will not included any resources that were
     *         modified as a result of this operation)
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
    @SuppressWarnings("deprecation")
    public EsaResourceWritable uploadFile(File esa, UploadStrategy strategy, String contentUrl) throws RepositoryException {

        ArtifactMetadata artifactMetadata = explodeArtifact(esa);

        // Read the meta data from the esa
        EsaManifest feature;
        try {
            feature = EsaManifest.constructInstance(esa);
        } catch (IOException e) {
            throw new RepositoryArchiveIOException(e.getMessage(), esa, e);
        }

        /*
         * First see if we already have this feature in MaaSive, note this means we can only have one
         * version of the asset in MaaSive at a time
         */
        EsaResourceWritable resource = WritableResourceFactory.createEsa(repoConnection);
        String symbolicName = feature.getSymbolicName();
        String version = feature.getVersion().toString();

        // Massive assets are always English, find the best name
        String subsystemName = feature.getHeader("Subsystem-Name",
                                                 Locale.ENGLISH);
        String shortName = feature.getIbmShortName();
        String metadataName = artifactMetadata != null ? artifactMetadata.getName() : null;
        final String name;

        /*
         * We want to be able to override the name in the built ESA with a value supplied in the metadata so
         * use this in preference of what is in the ESA so that we can correct any typos post-GM
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
            resource.setProviderName(provider);
        }

        // Add custom attributes for WLP
        resource.setProvideFeature(symbolicName);
        resource.setAppliesTo(feature.getHeader("IBM-AppliesTo"));
        Visibility visibility = feature.getVisibility();
        resource.setVisibility(visibility);

        /*
         * Two things affect the display policy - the visibility and the install policy. If a private auto
         * feature is set to manual install we need to make it visible so people know that it exists and can
         * be installed
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
        Map<String, List<String>> requiredFeaturesWithTolerates = feature.getRequiredFeatureWithTolerates();
        for (Map.Entry<String, List<String>> entry : requiredFeaturesWithTolerates.entrySet()) {
            resource.addRequireFeatureWithTolerates(entry.getKey(), entry.getValue());
        }

        // Old version which does not collect and store tolerates info
        // Calculate which features this relies on
//        for (String requiredFeature : feature.getRequiredFeatures()) {
//            resource.addRequireFeature(requiredFeature);
//        }

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

        // Set the license type if we're using the feature terms agreement
        String subsystemLicense = feature.getHeader("Subsystem-License");
        if (subsystemLicense != null && subsystemLicense.equals("http://www.ibm.com/licenses/wlp-featureterms-v1")) {
            resource.setLicenseType(LicenseType.UNSPECIFIED);
        }

        if (artifactMetadata != null) {
            attachLicenseData(artifactMetadata, resource);
        }

        // Now look for LI, LA files inside the .esa
        try {
            processLAandLI(esa, resource, feature);
        } catch (IOException e) {
            throw new RepositoryArchiveIOException(e.getMessage(), esa, e);
        }
        resource.setLicenseId(feature.getHeader("Subsystem-License"));

        // Publish to massive
        try {
            resource.uploadToMassive(strategy);
        } catch (RepositoryException re) {
            throw re;
        }

        resource.dump(System.out);
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

    private void processIcons(File esa, EsaManifest feature, EsaResourceWritable resource) throws RepositoryException {
        //checking icon file
        int size = 0;
        String current = "";
        String sizeString = "";
        String iconName = "";
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
                        } else {
                            iconName = sizeString;
                        }
                    }

                } else {
                    iconName = current;
                }

                File icon = this.extractFileFromArchive(esa.getAbsolutePath(), iconName).getExtractedFile();
                if (icon.exists()) {
                    AttachmentResourceWritable at = resource.addAttachment(icon, AttachmentType.THUMBNAIL);
                    if (size != 0) {
                        at.setImageDimensions(size, size);
                    }
                } else {
                    throw new RepositoryArchiveEntryNotFoundException("Icon does not exist", esa, iconName);
                }
            }
        }
    }

    @Override
    protected void checkRequiredProperties(ArtifactMetadata artifact) throws RepositoryArchiveInvalidEntryException {
        checkPropertySet(PROP_DESCRIPTION, artifact);
    }

}
