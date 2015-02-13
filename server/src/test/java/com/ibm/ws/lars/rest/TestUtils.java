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

package com.ibm.ws.lars.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.AssetList;

/**
 *
 */
public class TestUtils {

    /**
     * Reads the specified InputStream and returns a byte array containing all the bytes read.
     */
    public static byte[] slurp(InputStream is) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((length = is.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }

        return baos.toByteArray();
    }

    /**
     * Assert that an AssetList contains exactly the given list of assets
     * <p>
     * This method assumes that all assets have an ID and there are no duplicates in the asset list.
     */
    public static void assertAssetList(AssetList list, Asset... assets) {
        Map<String, Asset> assetIdMap = new HashMap<>();

        for (Asset asset : assets) {
            if (assetIdMap.put(asset.get_id(), asset) != null) {
                throw new AssertionError("Duplicate found in list of expected assets:\n" + asset.toJson());
            }
        }

        for (Asset asset : list) {
            if (assetIdMap.remove(asset.get_id()) == null) {
                throw new AssertionError("Unexpected asset found in the asset list:\n" + asset.toJson());
            }
        }

        if (!assetIdMap.isEmpty()) {
            StringBuilder message = new StringBuilder("Assets missing from asset list:\n");
            for (Asset asset : assetIdMap.values()) {
                message.append(asset.toJson());
                message.append("\n");
            }
            throw new AssertionError(message.toString());
        }
    }
}
