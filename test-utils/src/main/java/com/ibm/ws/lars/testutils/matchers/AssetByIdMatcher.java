/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
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
