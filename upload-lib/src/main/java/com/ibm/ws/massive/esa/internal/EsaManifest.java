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

package com.ibm.ws.massive.esa.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Version;

import com.ibm.ws.massive.resources.MassiveResource.Visibility;

public class EsaManifest {

    private static final Set<String> LOCALIZABLE_HEADERS = new HashSet<String>(Arrays.asList("Subsystem-Name", "Subsystem-Description"));

    /**
     * Create a new instance of this class for the supplied ESA file.
     *
     * @param esa The ESA to load
     * @return The {@link EsaManifest} for working with the properties of the ESA
     * @throws ZipException
     * @throws IOException
     */
    public static EsaManifest constructInstance(File esa)
            throws ZipException, IOException {
        // Find the manifest - case isn't guaranteed so do a search
        ZipFile zip = new ZipFile(esa);
        Enumeration<? extends ZipEntry> zipEntries = zip.entries();
        ZipEntry subsystemEntry = null;
        while (zipEntries.hasMoreElements()) {
            ZipEntry nextEntry = zipEntries.nextElement();
            if ("OSGI-INF/SUBSYSTEM.MF".equalsIgnoreCase(nextEntry.getName())) {
                subsystemEntry = nextEntry;
            }
        }
        return new EsaManifest(
                zip.getInputStream(subsystemEntry), zip);
    }

    private final ZipFile esa;
    private final Manifest mf;
    private String symbolicName;
    private Map<String, String> symbolicNameAttrs;

    private EsaManifest(InputStream manifestInputStream,
                        ZipFile esa) throws IOException {
        mf = ManifestProcessor.parseManifest(manifestInputStream);
        this.esa = esa;
    }

    public String getHeader(String header) {
        return mf.getMainAttributes().getValue(header);
    }

    public String getHeader(String header, Locale locale) {
        String headerValue = mf.getMainAttributes().getValue(header);
        if (LOCALIZABLE_HEADERS.contains(header) && headerValue != null && headerValue.startsWith("%")) {
            Properties props = getLocaleTranslations(locale);
            if (props != null) {
                headerValue = props.getProperty(headerValue.substring(1), headerValue);
            }
        }
        return headerValue;
    }

    private Properties getLocaleTranslations(Locale locale) {
        // Look at where to searh for localization files
        String localizationLocation = this.getHeader("Subsystem-Localization");
        if (localizationLocation == null) {
            return null;
        }

        ZipEntry[] entries = new ZipEntry[] {
                                             this.esa.getEntry(localizationLocation + "_"
                                                               + locale.toString() + ".properties"),
                                             this.esa.getEntry(localizationLocation + "_"
                                                               + locale.getLanguage() + ".properties"),
                                             this.esa.getEntry(localizationLocation + ".properties") };

        for (ZipEntry entry : entries) {
            if (entry != null) {
                try {
                    Properties props = new Properties();
                    props.load(new InputStreamReader(
                            this.esa.getInputStream(entry)));
                    return props;
                } catch (IOException e) {
                }
            }
        }

        return null;
    }

    public String getIbmShortName() {
        return getHeader("IBM-ShortName");
    }

    private void parseSymbolicName() {
        if (symbolicName != null) {
            return;
        }

        NameValuePair nvp = org.apache.aries.util.manifest.ManifestHeaderProcessor.parseBundleSymbolicName(getHeader("Subsystem-SymbolicName"));
        symbolicName = nvp.getName();
        symbolicNameAttrs = nvp.getAttributes();
    }

    /**
     * @return
     */
    public String getSymbolicName() {
        parseSymbolicName();
        return symbolicName;
    }

    /**
     * @return
     */
    public Version getVersion() {
        String versionString = getHeader("Subsystem-Version");
        Version version;
        if (versionString == null) {
            version = Version.emptyVersion;
        } else {
            version = new Version(versionString);
        }
        return version;
    }

    /**
     * @return
     */
    public boolean isAutoFeature() {
        return getHeader("IBM-Provision-Capability") != null;
    }

    /**
     * @return
     */
    public String getSupersededBy() {
        parseSymbolicName();
        boolean isSuperseded = Boolean.parseBoolean(symbolicNameAttrs.get("superseded"));
        String supersededBy = symbolicNameAttrs.get("superseded-by");
        if (!isSuperseded && supersededBy == null) {
            return null;
        } else if (isSuperseded && supersededBy != null) {
            return supersededBy;
        } else {
            // TODO: throw exception
            throw new RuntimeException("Superseded and superseded-by not set correctly");
        }
    }

    /**
     * @return
     */
    public Visibility getVisibility() {
        parseSymbolicName();

        String visibilityString = symbolicNameAttrs.get("visibility:");
        Visibility visibility = Visibility.PRIVATE;

        if (visibilityString != null) {
            switch (visibilityString) {
                case "public":
                    visibility = Visibility.PUBLIC;
                    break;
                case "protected":
                    visibility = Visibility.PROTECTED;
                    break;
                case "private":
                    visibility = Visibility.PRIVATE;
                    break;
                case "install":
                    visibility = Visibility.INSTALL;
                    break;
                default:
                    visibility = Visibility.PRIVATE;
            }
        }

        return visibility;
    }

    public List<String> getRequiredFeatures() {
        List<String> result = new ArrayList<String>();

        Map<String, Map<String, String>> featureContentMap = ManifestHeaderProcessor.parseImportString(getHeader("Subsystem-Content"));

        for (Entry<String, Map<String, String>> contentEntry : featureContentMap.entrySet()) {
            String symbolicName = contentEntry.getKey();
            Map<String, String> attributes = contentEntry.getValue();
            if ("osgi.subsystem.feature".equals(attributes.get("type"))) {
                result.add(symbolicName);
            }
        }

        return result;
    }

}
