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

import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.resources.writeable.RepositoryResourceWritable;
import com.ibm.ws.repository.strategies.writeable.UploadStrategy;

/**
 * Instances of this interface are responsible for uploading artifacts into the Liberty Repository.
 * An instance will be asked if it can handle a particular file before being asked to upload it. The
 * file passed to methods on this interface will either be an asset binary with a sibling metadata
 * zip file OR just the metadata zip itself.
 *
 * @param <T>
 */
public interface RepositoryUploader<T extends RepositoryResourceWritable> {

    /**
     * Returns <code>true</code> if this uploader is capable of uploading the supplied
     * <code>assetFile</code>
     *
     * @param assetFile The file that we want to upload
     */
    public boolean canUploadFile(File assetFile);

    /**
     * Uploads the <code>assetFile</code> to the asset service repository.
     *
     * @param assetFile The file that we want to upload
     * @param strategy The {@link UploadStrategy} to use when uploading the file
     * @param contentUrl The URL pointing to the content. If it is <code>null</code> the value
     *            supplied in "downloadURL" the repository metadata ZIP will be used and if that is
     *            also <code>null</code> then the content file will be uploading into the
     *            repository.
     * @throws RepositoryException if something goes wrong writing the file to the repository
     */
    public T uploadFile(File assetFile, UploadStrategy strategy, String contentUrl) throws RepositoryException;

}
