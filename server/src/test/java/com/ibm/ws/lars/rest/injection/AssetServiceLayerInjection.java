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
package com.ibm.ws.lars.rest.injection;

import java.lang.reflect.Field;

import com.ibm.ws.lars.rest.AssetServiceLayer;
import com.ibm.ws.lars.rest.Configuration;
import com.ibm.ws.lars.rest.Persistor;

/**
 * Class for doing injection into the AssetServiceLayer class during tests.
 */
public class AssetServiceLayerInjection {

    private static final String CONFIGURATION_FIELD = "configuration";
    private static final String PERSISTENCE_BEAN_FIELD = "persistenceBean";

    public static void setConfiguration(AssetServiceLayer serviceLayer, Configuration configuration) {
        try {
            Field field = AssetServiceLayer.class.getDeclaredField(CONFIGURATION_FIELD);
            field.setAccessible(true);
            field.set(serviceLayer, configuration);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject configuration", e);
        }
    }

    public static void setPersistenceBean(AssetServiceLayer serviceLayer, Persistor persistor) {
        try {
            Field field = AssetServiceLayer.class.getDeclaredField(PERSISTENCE_BEAN_FIELD);
            field.setAccessible(true);
            field.set(serviceLayer, persistor);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject persistence bean", e);
        }
    }

}
