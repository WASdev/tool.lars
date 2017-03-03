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
package com.ibm.ws.lars.testutils.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

import com.ibm.ws.repository.resources.RepositoryResource;

/**
 * Matches Resources against each other by comparing only the IDs.
 * <p>
 * If you have added some resources and then retrieved them, you can check the list of retrieved
 * resources is correct with
 *
 * <pre>
 * assertThat(returnedResources, containsInAnyOrder(byId(res1), byId(res2), ...))
 * </pre>
 */
public class ResourceByIdMatcher extends TypeSafeMatcher<RepositoryResource> {

    private final String expectedId;

    private ResourceByIdMatcher(String expectedId) {
        this.expectedId = expectedId;
    }

    /** {@inheritDoc} */
    @Override
    public void describeTo(Description description) {
        description.appendText("resource id should be ").appendValue(expectedId);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean matchesSafely(RepositoryResource resource) {
        return resource.getId().equals(expectedId);
    }

    @Factory
    public static ResourceByIdMatcher hasId(String id) {
        return new ResourceByIdMatcher(id);
    }

    @Factory
    public static ResourceByIdMatcher hasId(RepositoryResource res) {
        return new ResourceByIdMatcher(res.getId());
    }

}
