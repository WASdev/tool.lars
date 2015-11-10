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

package com.ibm.ws.repository.exceptions;

import com.ibm.ws.repository.transport.exceptions.BadVersionException;

/**
 * Use this when we pull invalid objects out of the repository. Objects are invalid if, 
 * for instance, they fail a VersionableContent.verify() check. 
 *
 */
public class RepositoryBadDataException extends RepositoryResourceException {

    private static final long serialVersionUID = 1L;
    private final BadVersionException badVersion;

    public RepositoryBadDataException (String message, String resource, BadVersionException bvx) { 
        super(message, resource);
        initCause(bvx);
        badVersion = bvx;
    }
    
    public Throwable getCause() {
        return super.getCause();
    }
    
    public String getMinVersion() { 
        return badVersion.getMinVersion();
    }
    
    public String getMaxVersion() { 
        return badVersion.getMaxVersion();
    }
    
    public String getBadVersion() { 
        return badVersion.getBadVersion();
    }

}
