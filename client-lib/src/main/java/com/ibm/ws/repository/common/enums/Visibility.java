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

package com.ibm.ws.repository.common.enums;

/**
 * Enumeration of the available visibility settings for a feature
 *
 * @see <a href="https://www-01.ibm.com/support/knowledgecenter/SSEQTP_8.5.5/com.ibm.websphere.wlp.doc/ae/rwlp_feat_definition.html?lang=en">IBM Knowledge Center</a>
 */
public enum Visibility {
    /**
     * Feature is considered API, can be included in server.xml
     */
    PUBLIC,

    /**
     * Feature is considered internal, should not be relied upon and may change at any time
     */
    PRIVATE,

    /**
     * Feature is considered SPI, can be relied upon by other features but cannot be specified in server.xml
     */
    PROTECTED,

    /**
     * Feature is only used as a shortcut at install time to install all of its dependencies. Can never be started in a running server.
     */
    INSTALL
}
