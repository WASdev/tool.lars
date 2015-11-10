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

package com.ibm.ws.repository.transport.exceptions;


public class BadVersionException extends Exception { 
    
    private static final long serialVersionUID = 1L;
    
    private final String _minVersion;
    private final String _maxVersion;
    private final String _badVersion;

    public BadVersionException(String min, String max, String bad) {
        super();
        _minVersion = min;
        _maxVersion = max;
        _badVersion = bad;
    }
    
    public String getMinVersion() { 
        return _minVersion;
    }
    
    public String getMaxVersion() { 
        return _maxVersion;
    }
    
    public String getBadVersion() { 
        return _badVersion;
    }
}
