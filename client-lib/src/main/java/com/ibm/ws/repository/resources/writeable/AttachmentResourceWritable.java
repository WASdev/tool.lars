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

import com.ibm.ws.repository.resources.AttachmentResource;

/**
 * A writable view of an attachment which can be stored in a repository.
 * <p>
 * An attachment is a file associated with a repository resource
 * <p>
 * The attachment itself can be read by calling {@link #getInputStream()}.
 */
public interface AttachmentResourceWritable extends AttachmentResource {

    /**
     * Sets the image dimensions in the attachment metadata
     * 
     * @param height the height of the image
     * @param width the width of the image
     */
    public void setImageDimensions(int height, int width);

}