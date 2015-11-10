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
package com.ibm.ws.repository.resources.writeable;

import java.util.Collection;

import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.resources.SampleResource;

/**
 * Represents a Sample Resource which can be uploaded to a repository.
 * <p>
 * This interface allows write access to fields which are specific to samples.
 * <p>
 * Samples represented by this interface can either be of type {@link ResourceType#PRODUCTSAMPLE} or {@link ResourceType#OPENSOURCE}.
 */
public interface SampleResourceWritable extends SampleResource, RepositoryResourceWritable, ApplicableToProductWritable {

    /**
     * Sets the list of required features for this sample
     * 
     * @param requireFeature the list of symbolic names of required features
     */
    public void setRequireFeature(Collection<String> requireFeature);

    /**
     * Sets the short name for this sample
     * <p>
     * The short name should be the name of the server included in the sample.
     * 
     * @param shortName The short name for this sample
     */
    public void setShortName(String shortName);

    /**
     * Sets the type of the sample
     * <p>
     * Samples can either be of type {@link ResourceType#PRODUCTSAMPLE} or {@link ResourceType#OPENSOURCE}.
     * 
     * @param type the type of the sample
     */
    public void setType(ResourceType type);

}