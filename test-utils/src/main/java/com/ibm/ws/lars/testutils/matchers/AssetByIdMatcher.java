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
package com.ibm.ws.lars.testutils.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

import com.ibm.ws.repository.transport.model.Asset;

/**
 * Matches Assets against each other by comparing only the IDs.
 * <p>
 * If you have added some assets and then retrieved them, you can check the list of retrieved assets
 * is correct with
 *
 * <pre>
 * assertThat(returnedAssets, containsInAnyOrder(byId(asset1), byId(asset2), ...))
 * </pre>
 */
public class AssetByIdMatcher extends TypeSafeMatcher<Asset> {

    private final String expectedId;

    private AssetByIdMatcher(String expectedId) {
        this.expectedId = expectedId;
    }

    /** {@inheritDoc} */
    @Override
    public void describeTo(Description description) {
        description.appendText("asset id should be ").appendValue(expectedId);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean matchesSafely(Asset asset) {
        return asset.get_id().equals(expectedId);
    }

    @Factory
    public static AssetByIdMatcher hasId(String id) {
        return new AssetByIdMatcher(id);
    }

    @Factory
    public static AssetByIdMatcher hasId(Asset asset) {
        return new AssetByIdMatcher(asset.get_id());
    }

}
