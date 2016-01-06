/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
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
package com.ibm.ws.repository.strategies.test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.resources.internal.SampleResourceImpl;
import com.ibm.ws.repository.strategies.writeable.AssetOnlyReplacementStrategy;
import com.ibm.ws.repository.strategies.writeable.UpdateInPlaceStrategy;
import com.ibm.ws.repository.strategies.writeable.UploadStrategy;

@RunWith(Parameterized.class)
public class AssetOnlyReplacementStrategyTest extends StrategyTestBaseClass {

    public AssetOnlyReplacementStrategyTest(RepositoryFixture fixture) {
        super(fixture);
    }

    @Before
    public void requireUpdateCapability() {
        assumeThat(fixture.isUpdateSupported(), is(true));
    }

    /**
     * Create a resource then edit it using the new AssetOnlyReplacementStrategy
     *
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     */
    @Test
    public void testEditingResourceUsingAssetOnlyReplacementStrategy() throws RepositoryBackendException, RepositoryResourceException {

        // use overwrite strategy to create the asset before using AssetOnlyReplacementStrategy
        // as this expects the asset to exist
        _testRes.uploadToMassive(new UpdateInPlaceStrategy());
        SampleResourceImpl readBack = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack.equivalent(_testRes));

        // Perform an update but make sure there are no changes
        _testRes.uploadToMassive(new AssetOnlyReplacementStrategy());
        SampleResourceImpl readBack2 = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        checkSame(readBack, readBack2, _testRes, true, true);

        _testRes.setFeaturedWeight("5");
        _testRes.uploadToMassive(new AssetOnlyReplacementStrategy());
        SampleResourceImpl readBack3 = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        checkUpdated(readBack2, readBack3, _testRes, true);

    }

    @Override
    protected State[] getValidTargetStates(State initial) {
        return new State[] { initial };
    }

    @Override
    protected UploadStrategy createStrategy(State ifMatching, State ifNoMatching) {
        return new AssetOnlyReplacementStrategy();
    }

}
