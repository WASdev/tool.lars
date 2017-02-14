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
package com.ibm.ws.lars.rest;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.AssetCursor;
import com.ibm.ws.lars.rest.model.AssetOperation;

/**
 * A super simple AssetCursor backed by a collection
 */
public class BasicAssetCursor implements AssetCursor {

    Iterator<Map<String, Object>> assets;
    int count;

    public BasicAssetCursor(Collection<Map<String, Object>> states) {
        count = states.size();
        assets = states.iterator();
    }

    @Override
    public boolean hasNext() {
        return assets.hasNext();
    }

    @Override
    public Asset next() {
        return new Asset(assets.next());
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public void addOperation(AssetOperation filter) {

    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        // Do nothing
    }

}
