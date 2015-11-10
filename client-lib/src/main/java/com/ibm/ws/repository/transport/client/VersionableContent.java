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

package com.ibm.ws.repository.transport.client;

import com.ibm.ws.repository.transport.exceptions.BadVersionException;

/**
 * Classes implementing this interface understand and obey the limited semantic versioning
 * rules we're implementing for Repository / Massive content-things.
 * 
 */
public interface VersionableContent {

    void validate(String version) throws BadVersionException;

    String nameOfVersionAttribute();
}
