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

import java.util.HashSet;
import java.util.LinkedHashSet;

/**
 * ArrayList of massive resources that doesn't contain dupes.
 */
public class ResourceList<T extends MassiveResource> extends LinkedHashSet<MassiveResource> {

    private static final long serialVersionUID = 6215849151982344711L;

    HashSet<MassiveResourceMatchingData> dupesChecker = new HashSet<MassiveResourceMatchingData>();

    @Override
    public boolean add(MassiveResource res) {
        if (dupesChecker.add(res.createMatchingData())) {
            return super.add(res);
        }
        return false;
    }
}
