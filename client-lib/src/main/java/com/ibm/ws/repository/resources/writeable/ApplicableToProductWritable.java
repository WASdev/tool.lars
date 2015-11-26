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

import com.ibm.ws.repository.resources.ApplicableToProduct;

/**
 * Writable interface to indicate that this resource is something that can hold an applies to field indicating which products it is applicable to.
 */
public interface ApplicableToProductWritable extends ApplicableToProduct {

    /**
     * Sets the applies to field for the resource
     *
     * @param appliesTo the new appliesTo field value
     */
    public void setAppliesTo(String appliesTo);
}
