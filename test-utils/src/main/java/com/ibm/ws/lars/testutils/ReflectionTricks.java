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

package com.ibm.ws.lars.testutils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import com.ibm.ws.massive.LoginInfo;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.resources.MassiveResource;
import com.ibm.ws.massive.sa.client.model.Asset;

public class ReflectionTricks {

    /**
     * Invoke a method reflectively that does not use primitive arguments
     *
     * @param targetObject
     * @param methodName
     * @param varargs
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static Object reflectiveCallNoPrimitives(Object targetObject, String methodName, Object... varargs)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException,
            IllegalArgumentException, InvocationTargetException {

        // Usage example of this method
        // int i = (Integer)reflectiveCallAnyTypes(targetObject,"methodName",1)

        // create a class array from the vararg object array
        @SuppressWarnings("rawtypes")
        Class[] classes;
        if (varargs != null) {
            classes = new Class[varargs.length];
            for (int i = 0; i < varargs.length; i++) {
                classes[i] = varargs[i].getClass();
            }
        } else {
            classes = new Class[0];
        }

        return reflectiveCallAnyTypes(targetObject, methodName, classes, varargs);
    }

    /**
     * Invoke a method reflectively if that does not use primitive arguments
     *
     * @param targetObject
     * @param methodName
     * @param classes
     * @param values
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static Object reflectiveCallAnyTypes(Object targetObject, String methodName, @SuppressWarnings("rawtypes") Class[] classes, Object[] values)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException,
            IllegalArgumentException, InvocationTargetException {

        // Usage example of this method
        // int i = (Integer)reflectiveCallAnyTypes(targetObject,"methodName",
        //       new Class[] {int.class},
        //       new Object[] {9});

        // Get the class of the targetObject
        Class<?> c = null;
        if (targetObject instanceof Class) {
            c = (Class<?>) targetObject;
        } else {
            c = targetObject.getClass();
        }

        Method method = null;
        boolean finished = false;
        while (!finished) {

            try {
                // get method
                method = c.getDeclaredMethod(methodName, classes);
                finished = true;
                method.setAccessible(true);
            } catch (NoSuchMethodException nsme) {
                if (c.getName().equals("java.lang.Object")) {
                    throw nsme;
                } else {
                    c = c.getSuperclass();
                }
            }
        }

        // Invoke MassiveResource.getAsset()
        return method.invoke(targetObject, values);
    }

    public static Asset getAssetReflective(MassiveResource mr) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        // Get MassiveResource
        Class<?> c = Class
                .forName("com.ibm.ws.massive.resources.MassiveResource");

        // Get MassiveResource.getAsset()
        Method method = c.getDeclaredMethod("getAsset");

        // Invoke MassiveResource.getAsset()
        method.setAccessible(true);
        Asset asset = (Asset) method.invoke(mr);

        return asset;
    }

    @SuppressWarnings("unchecked")
    public static Collection<MassiveResource> getAllResourcesWithDupes(LoginInfoEntry loginInfoEntry) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        return (Collection<MassiveResource>) reflectiveCallAnyTypes(MassiveResource.class, "getAllResourcesWithDupes", new Class[] { LoginInfo.class },
                                                                    new Object[] { new LoginInfo(loginInfoEntry) });
    }
}
