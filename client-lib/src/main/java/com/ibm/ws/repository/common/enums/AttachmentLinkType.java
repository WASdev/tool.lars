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
 * Denotes the meaning of the attachment URL
 * <p>
 * Note that when the attachment content is to be stored in the repository server, the attachment link type should be unset.
 */
public enum AttachmentLinkType {

    /**
     * @deprecated Not used
     */
    EFD,

    /**
     * URL links directly to the attachment file
     */
    DIRECT,

    /**
     * URL links to a web page where the attachment file is available
     */
    WEB_PAGE;
}