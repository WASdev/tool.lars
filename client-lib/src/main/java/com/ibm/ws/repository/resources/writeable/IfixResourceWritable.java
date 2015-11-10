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
import java.util.Date;

import com.ibm.ws.repository.resources.IfixResource;

/**
 * Represents an IFix Resource which can be uploaded to a repository.
 * <p>
 * This interface allows write access to fields which are specific to ifixes.
 */
public interface IfixResourceWritable extends IfixResource, RepositoryResourceWritable, WebDisplayable, ApplicableToProductWritable {

    /**
     * Sets the list of APAR IDs which are provided in the ifix resource
     * 
     * @param provides the list of APAR IDs
     */
    public void setProvideFix(Collection<String> provides);

    /**
     * Sets the modified date of the most recently modified file to be replaced by the update
     * 
     * @param date the date
     */
    public void setDate(Date date);

}