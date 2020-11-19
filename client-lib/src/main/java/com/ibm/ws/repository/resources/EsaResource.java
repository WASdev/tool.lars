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
package com.ibm.ws.repository.resources;

import java.util.Collection;
import java.util.Map;

import com.ibm.ws.repository.common.enums.InstallPolicy;
import com.ibm.ws.repository.common.enums.Visibility;

/**
 * Represents a Feature Resource in a repository.
 * <p>
 * This interface allows read access to fields which are specific to features.
 */
public interface EsaResource extends RepositoryResource, ApplicableToProduct {

    /**
     * Gets the symbolic name of the feature
     *
     * @return the symbolic name of the feature, or null if it has not been set
     */
    public String getProvideFeature();

    /**
     * Returns a collection of iFix IDs that this feature require
     *
     * @return a collection of iFix IDs that this feature requires, or null if no iFixes are required
     */
    public Collection<String> getRequireFix();

    /**
     * Gets the list of required features for this feature
     *
     * @deprecated Should use {@link #getRequireFeatureWithTolerates()} instead
     *
     * @return The list of required features for this feature, or null if no features are required
     */
    @Deprecated
    public Collection<String> getRequireFeature();

    /**
     * Get the required features for this feature. Returns a map of
     * 'required feature + version' -> 'other tolerated versions (if any)'
     * The collection of tolerated versions will be empty if
     * 1. There are no other tolerated versions
     * 2. No tolerates information is available in the repository.
     * Returns null if there are no required features.
     */
    public Map<String, Collection<String>> getRequireFeatureWithTolerates();

    /**
     * Gets the short name for this feature, as defined by the IBM-ShortName header
     *
     * @return The short name for this feature, or null if it has not been set
     */
    public String getShortName();

    /**
     * Gets a lower case version of the {@link #getShortName()}.
     *
     * @return The lower cased short name, or null if it has not been set
     */
    public String getLowerCaseShortName();

    /**
     * Returns the value of the ibmProvisionCapability field (from the
     * "IBM-Provision-Capability" header in the ESA's manifest) which gives
     * information about the capabilities required to be present for the feature
     * to be provisioned.
     *
     * @return the value of the "IBM-Provision-Capability" header, or null if it has not been set
     */
    public String getProvisionCapability();

    /**
     * Returns the install policy for this feature.
     * <p>
     * A feature with an install policy of {@link InstallPolicy#WHEN_SATISFIED} should be installed automatically
     * if all of its provisioning capabilities are satisfied.
     *
     * @return The install policy for this feature, or null if it has not been set
     */
    public InstallPolicy getInstallPolicy();

    /**
     * Returns the {@link Visibility} for this feature.
     * <p>
     * This is taken from the visibility directive in the SubsystemSymbolicName header in the feature manifest.
     *
     * @return the visibility, or null if it has not been set
     */
    public Visibility getVisibility();

    /**
     * Checks if this feature is a singleton
     *
     * @return the singleton value (which can be "true", "false" or null)
     */
    public String getSingleton();

    /**
     * Checks if this feature is a singleton. This is a helper function
     * that calls getSingleton and converts it from a String value to
     * a boolean
     *
     * @return true if this feature is a singleton and false otherwise
     */
    public boolean isSingleton();

    /**
     * Gets the IBM install too property from the feature.
     *
     * @return The IBM-InstallTo header property
     */
    public String getIBMInstallTo();
}
