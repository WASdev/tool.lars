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
package com.ibm.ws.repository.common.enums.test;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.ibm.ws.repository.common.enums.FilterableAttribute;

public class FilterableAttributeTest {

    @Test
    public void testGetType() {

        for (FilterableAttribute attr : FilterableAttribute.values()) {
            Class<?> type = attr.getType();
            assertNotNull("All enum instances should have a real type '" + attr.getAttributeName() + "' did not", type);
        }

    }

}
