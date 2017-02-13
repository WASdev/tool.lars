/*******************************************************************************
 * Copyright (c) 2017 IBM Corp.
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
package com.ibm.ws.lars.rest.model;

import java.io.Closeable;
import java.util.Iterator;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * An iterator over a set of Assets
 * <p>
 * The caller can iterate over a set of Assets, without regard for where they're coming from.
 * <p>
 * For instance, the implementation may be iterating over an in-memory collection, or it may be
 * retrieving results from a database and transforming them into assets on demand before returning
 * them to the caller.
 * <p>
 * Any {@link AssetOperation}s added to the cursor will be applied to each Asset before it is
 * returned.
 */
@JsonSerialize(as = Iterator.class)
public interface AssetCursor extends Iterator<Asset>, Closeable {

    /**
     * Get the total number of assets which will be returned from this cursor
     *
     * @return the number of assets
     */
    public int size();

    /**
     * Add an {@link AssetOperation} to be applied to each asset before it is returned from this
     * cursor
     * <p>
     * Operations are applied in the order they were added to the cursor.
     *
     * @param op the operation to add
     */
    public void addOperation(AssetOperation op);
}
