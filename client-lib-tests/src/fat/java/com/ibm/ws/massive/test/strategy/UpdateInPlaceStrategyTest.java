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

package com.ibm.ws.massive.test.strategy;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import com.ibm.ws.massive.RepositoryBackendException;
import com.ibm.ws.massive.resources.MassiveResource.State;
import com.ibm.ws.massive.resources.RepositoryResourceException;
import com.ibm.ws.massive.resources.SampleResource;
import com.ibm.ws.massive.resources.UpdateInPlaceStrategy;
import com.ibm.ws.massive.resources.UploadStrategy;

public class UpdateInPlaceStrategyTest extends StrategyTestBaseClass {

    public UpdateInPlaceStrategyTest() throws FileNotFoundException,
        IOException {
        super();
    }

    @Test
    @Ignore
    // needs update assets
    public void testAddingToRepoUsingUpdateInPlaceStrategy() throws RepositoryBackendException, RepositoryResourceException {
        _testRes.uploadToMassive(new UpdateInPlaceStrategy());
        SampleResource readBack = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack.equivalent(_testRes));

        // Perform an update but make sure there are no changes
        _testRes.uploadToMassive(new UpdateInPlaceStrategy());
        SampleResource readBack2 = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        checkSame(readBack, readBack2, _testRes, true, true);

        _testRes.setFeaturedWeight("5");
        _testRes.uploadToMassive(new UpdateInPlaceStrategy());
        SampleResource readBack3 = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        checkUpdated(readBack2, readBack3, _testRes, true);
    }

    @Test
    @Ignore
    // needs update assets
    public void testAddingToRepoUsingUpdateInPlaceStrategyWithForceReplace() throws RepositoryBackendException, RepositoryResourceException {
        _testRes.uploadToMassive(new UpdateInPlaceStrategy());
        SampleResource readBack = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack.equivalent(_testRes));

        // Perform an update but make sure there are no changes
        _testRes.uploadToMassive(new UpdateInPlaceStrategy(State.DRAFT, State.DRAFT, true));
        SampleResource readBack2 = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        checkSame(readBack, readBack2, _testRes, true, false);

        _testRes.setFeaturedWeight("5");
        _testRes.uploadToMassive(new UpdateInPlaceStrategy());
        SampleResource readBack3 = SampleResource.getSample(_loginInfoEntry, _testRes.get_id());
        checkUpdated(readBack2, readBack3, _testRes, true);
    }

    @Override
    protected UploadStrategy createStrategy(State ifMatching, State ifNoMatching) {
        return new UpdateInPlaceStrategy(ifMatching, ifNoMatching, false);
    }

}
