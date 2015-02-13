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

package com.ibm.ws.massive.upload;

import java.io.File;

public class RepositoryArchiveEntryNotFoundException extends RepositoryArchiveInvalidEntryException {

    private static final long serialVersionUID = 5182741767440154685L;

    public RepositoryArchiveEntryNotFoundException(String message, File archive, String entryPath) {
        super(message, archive, entryPath);
    }

    public RepositoryArchiveEntryNotFoundException(String message, File archive, String entryPath, Throwable cause) {
        super(message, archive, entryPath, cause);
    }

}
