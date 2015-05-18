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

package com.ibm.ws.lars.rest.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.ws.lars.rest.InvalidJsonAssetException;

/**
 *
 */
public class AssetList extends RepositoryObjectList<Asset> implements Iterable<Asset> {

    public static AssetList createAttachmentListFromProperties(ArrayList<Map<String, Object>> state) {
        return new AssetList(state);
    }

    private AssetList(List<Map<String, Object>> state) {
        super(state);
    }

    public static AssetList jsonArrayToAssetList(String json) throws InvalidJsonAssetException {
        List<Map<String, Object>> state = readJsonState(json);

        return new AssetList(state);
    }

    public static AssetList createAssetListFromMaps(List<Map<String, Object>> maps) {
        return new AssetList(maps);
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<Asset> iterator() {
        return new AssetIterator(state);
    }

    private static class AssetIterator implements Iterator<Asset> {
        private final Iterator<Map<String, Object>> stateIterator;

        public AssetIterator(List<Map<String, Object>> state) {
            stateIterator = state.iterator();
        }

        @Override
        public boolean hasNext() {
            return stateIterator.hasNext();
        }

        @Override
        public Asset next() {
            return Asset.createAssetFromMap(stateIterator.next());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove() not supported");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Asset wrapState(Map<String, Object> props) {
        return Asset.createAssetFromMap(props);
    }
}
