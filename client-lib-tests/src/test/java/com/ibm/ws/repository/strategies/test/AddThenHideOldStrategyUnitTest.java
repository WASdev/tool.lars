/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.junit.Test;

import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.AdminScriptResourceImpl;
import com.ibm.ws.repository.resources.internal.ConfigSnippetResourceImpl;
import com.ibm.ws.repository.resources.internal.EsaResourceImpl;
import com.ibm.ws.repository.resources.internal.ProductResourceImpl;
import com.ibm.ws.repository.resources.internal.SampleResourceImpl;
import com.ibm.ws.repository.resources.internal.ToolResourceImpl;
import com.ibm.ws.repository.resources.writeable.AdminScriptResourceWritable;
import com.ibm.ws.repository.resources.writeable.ConfigSnippetResourceWritable;
import com.ibm.ws.repository.resources.writeable.EsaResourceWritable;
import com.ibm.ws.repository.resources.writeable.ProductResourceWritable;
import com.ibm.ws.repository.resources.writeable.SampleResourceWritable;
import com.ibm.ws.repository.resources.writeable.ToolResourceWritable;
import com.ibm.ws.repository.strategies.writeable.AddThenHideOldStrategy;
import com.ibm.ws.repository.strategies.writeable.AddThenHideOldStrategy.MinAndMaxVersion;
import com.ibm.ws.repository.strategies.writeable.Version4Digit;

/**
 * Unit Tests for AddThenHideOldStrategy
 */
public class AddThenHideOldStrategyUnitTest {

    private static final String APPLIES_TO_8555 = "com.ibm.websphere.appserver; productEdition=\"BASE,BASE_ILAN,DEVELOPERS,EXPRESS,ND,zOS\"; productVersion=8.5.5.5";
    private static final String APPLIES_TO_8559 = "com.ibm.websphere.appserver; productEdition=\"BASE,BASE_ILAN,DEVELOPERS,EXPRESS,ND,zOS\"; productVersion=8.5.5.9";
    private static final String APPLIES_TO_JAN_BETA = "com.ibm.websphere.appserver; productVersion=2016.1.0.0";
    private static final String APPLIES_TO_FEB_BETA = "com.ibm.websphere.appserver; productVersion=2016.2.0.0";
    private static final String APPLIES_TO_PRODUCT = "com.ibm.websphere.appserver"; // no version specified
    private static final String APPLIES_TO_8555_PLUS = "com.ibm.websphere.appserver; productVersion=8.5.5.5+";
    private static final String APPLIES_TO_8559_PLUS = "com.ibm.websphere.appserver; productVersion=8.5.5.9+";
    private static final Version4Digit MAX_VERSION = new Version4Digit(Integer.MAX_VALUE, 0, 0, "0");
    private static final Version4Digit MIN_VERSION = new Version4Digit(0, 0, 0, "0");

    private static AddThenHideOldStrategy _athos = new AddThenHideOldStrategy();

    private Object invoke(Object object, String methodName, Object... args) throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        Class defaultType = com.ibm.ws.repository.resources.RepositoryResource.class;
        Class[] carg = new Class[args.length];
        int i=0;
        for(Object arg: args) {
            if(defaultType.isInstance(arg)) {
                carg[i++] = defaultType;
            } else {
                carg[i++] = arg.getClass();
            }
        }
        Method method = object.getClass().getDeclaredMethod(methodName, carg);
        method.setAccessible(true);
        Object r = method.invoke(object, args);
        method.setAccessible(false);
        return r;
    }

    @Test
    public void testIsBeta() throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        String methodName = "isBeta";

        EsaResourceWritable esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo(APPLIES_TO_8559);
        assertEquals("Unexpected return for 8559: ", false, invoke(_athos, methodName, esa1));
        esa1.setAppliesTo(APPLIES_TO_JAN_BETA);
        assertEquals("Unexpected return for beta: ", true, invoke(_athos, methodName, esa1));
    }

    @Test
    public void testReturnNonBetaResourceOrNull() throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

        String methodName = "returnNonBetaResourceOrNull";
        EsaResourceWritable esa1;
        EsaResourceWritable esa2;

        // both beta
        esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo(APPLIES_TO_JAN_BETA);
        esa2 = new EsaResourceImpl(null);
        esa2.setAppliesTo(APPLIES_TO_FEB_BETA);
        assertEquals("Unexpected return comparing betas: ", null, invoke(_athos, methodName, esa1, esa2));

        // both SAME beta
        esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo(APPLIES_TO_JAN_BETA);
        esa2 = new EsaResourceImpl(null);
        esa2.setAppliesTo(APPLIES_TO_JAN_BETA);
        assertEquals("Unexpected return comparing same beta: ", null, invoke(_athos, methodName, esa1, esa2));

        // both non beta
        esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo(APPLIES_TO_8559);
        esa2 = new EsaResourceImpl(null);
        esa2.setAppliesTo(APPLIES_TO_8555);
        assertEquals("Unexpected return comparing versions: ", null, invoke(_athos, methodName, esa1, esa2));

        // first beta
        esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo(APPLIES_TO_JAN_BETA);
        esa2 = new EsaResourceImpl(null);
        esa2.setAppliesTo(APPLIES_TO_8559);
        assertEquals("Unexpected return comparing beta and non-beta: ", esa2, invoke(_athos, methodName, esa1, esa2));

        // 2nd beta
        esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo(APPLIES_TO_8559);
        esa2 = new EsaResourceImpl(null);
        esa2.setAppliesTo(APPLIES_TO_JAN_BETA);
        assertEquals("Unexpected return comparing in reverse order: ", esa1, invoke(_athos, methodName, esa1, esa2));
    }

    @Test
    public void testGetNewerResource() throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        final String methodName = "getNewerResource";
        EsaResourceWritable esa1, esa2;

        esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo(APPLIES_TO_8555);
        esa2 = new EsaResourceImpl(null);
        esa2.setAppliesTo(APPLIES_TO_8559);
        assertEquals("Wrong resource returned comparing 8555/9: ", esa2, invoke(_athos, methodName, esa1, esa2));

        esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo(APPLIES_TO_8559);
        esa2 = new EsaResourceImpl(null);
        esa2.setAppliesTo(APPLIES_TO_8559_PLUS);
        assertEquals("Wrong resource returned comparing 8559/8559+: ", esa2, invoke(_athos, methodName, esa1, esa2));

        esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo(APPLIES_TO_8555_PLUS);
        esa2 = new EsaResourceImpl(null);
        esa2.setAppliesTo(APPLIES_TO_8559);
        assertEquals("Wrong resource returned comparing 8559/8559+: ", esa2, invoke(_athos, methodName, esa1, esa2));

        esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo(APPLIES_TO_8555_PLUS);
        esa2 = new EsaResourceImpl(null);
        esa2.setAppliesTo(APPLIES_TO_8559_PLUS);
        assertEquals("Wrong resource returned comparing 8559+/8559+: ", esa2, invoke(_athos, methodName, esa1, esa2));

        esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo(APPLIES_TO_PRODUCT);
        esa2 = new EsaResourceImpl(null);
        esa2.setAppliesTo(APPLIES_TO_8559);
        assertEquals("Wrong resource returned comparing product/8559: ", esa2, invoke(_athos, methodName, esa1, esa2));
    }

    @Test
    public void testCompareNonProductResourceAppliesTo() throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        final String methodName = "compareNonProductResourceAppliesTo";
        EsaResourceWritable esa1, esa2;

        esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo(APPLIES_TO_PRODUCT);
        esa2 = new EsaResourceImpl(null);
        esa2.setAppliesTo(APPLIES_TO_8559);
        assertEquals("Wrong resource returned comparing base with 8559 appliesTo: ", esa2, invoke(_athos, methodName, esa1, esa2));

        esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo(APPLIES_TO_8559);
        esa2 = new EsaResourceImpl(null);
        esa2.setAppliesTo(APPLIES_TO_8555);
        assertEquals("Wrong resource returned, comparing 8559 and 8555: ", esa1, invoke(_athos, methodName, esa1, esa2));

        esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo(APPLIES_TO_8559);
        esa1.setVersion("1.0.0");
        esa2 = new EsaResourceImpl(null);
        esa2.setAppliesTo(APPLIES_TO_8559);
        esa2.setVersion("2.0.0");
        assertEquals("Wrong resource returned comparing 8559 with different versions: ", esa2, invoke(_athos, methodName, esa1, esa2));
        
        esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo(APPLIES_TO_8559);
        esa2 = new EsaResourceImpl(null);
        esa2.setAppliesTo(APPLIES_TO_8559_PLUS);
        assertEquals("Wrong resource returned, comparing 8559 and 8559+: ", esa2, invoke(_athos, methodName, esa1, esa2));
    }

    @Test
    public void testGetNonProductResourceWithHigherVersion() throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        final String methodName = "getNonProductResourceWithHigherVersion";
        EsaResourceWritable esa1, esa2;

        // return higher version
        esa1 = new EsaResourceImpl(null);
        esa1.setVersion("1.0.0");
        esa2 = new EsaResourceImpl(null);
        esa2.setVersion("2.0.0");
        assertEquals("Wrong valid resource returned: ", esa2, invoke(_athos, methodName, esa1, esa2));

        // if one (only) of the versions are valid return parm1
        esa1 = new EsaResourceImpl(null);
        esa1.setVersion("version 1");
        esa2 = new EsaResourceImpl(null);
        esa2.setVersion("2.0.0");
        assertEquals("Wrong resource returned when one is valid: ", esa1, invoke(_athos, methodName, esa1, esa2));

        // if both of the versions are invalid return parm1
        esa1 = new EsaResourceImpl(null);
        esa1.setVersion("version 1");
        esa2 = new EsaResourceImpl(null);
        esa2.setVersion("Version 2");
        assertEquals("Wrong resource returned when one is valid: ", esa1, invoke(_athos, methodName, esa1, esa2));

        // if version null return parm1
        esa1 = new EsaResourceImpl(null);
        esa2 = new EsaResourceImpl(null);
        assertEquals("Wrong resource returned: ", esa1, invoke(_athos, methodName, esa1, esa2));
    }

    @Test
    public void testGetMaxAppliesToVersionFromAppliesTo() throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

        String methodName = "getMinAndMaxAppliesToVersionFromAppliesTo";

        MinAndMaxVersion ver = (MinAndMaxVersion)invoke(_athos, methodName, APPLIES_TO_8555);
        assertEquals("Incorrect applies to version returned: ", "8.5.5.5", ver.min.toString());
        assertEquals("Incorrect applies to version returned: ", "8.5.5.5", ver.max.toString());

        ver = (MinAndMaxVersion)invoke(_athos, methodName, APPLIES_TO_PRODUCT);
        assertEquals("Incorrect applies to version returned: ", MIN_VERSION.toString(), ver.min.toString());
        assertEquals("Incorrect applies to version returned: ", MAX_VERSION.toString(), ver.max.toString());

        ver = (MinAndMaxVersion)invoke(_athos, methodName, APPLIES_TO_JAN_BETA);
        assertEquals("Incorrect applies to version returned: ", "2016.1.0.0", ver.min.toString());
        assertEquals("Incorrect applies to version returned: ", "2016.1.0.0", ver.max.toString());

        ver = (MinAndMaxVersion)invoke(_athos, methodName, APPLIES_TO_8555_PLUS);
        assertEquals("Incorrect applies to version returned: ", "8.5.5.5", ver.min.toString());
        assertEquals("Incorrect applies to version returned: ", MAX_VERSION.toString(), ver.max.toString());
    }

    @Test
    public void testIsVisibleAndWebDisplayable() throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

        String methodName = "isVisibleAndWebDisplayable";

        // webdisplayable (null is visible)
        ProductResourceWritable prod = new ProductResourceImpl(null);
        assertEquals("Unexpected return for null visibility product: ", true, invoke(_athos, methodName, prod));
        prod.setWebDisplayPolicy(DisplayPolicy.HIDDEN);
        assertEquals("Unexpected return for hidden product: ", false, invoke(_athos, methodName, prod));

        EsaResourceWritable esa = new EsaResourceImpl(null);
        assertEquals("Unexpected return for null visibility feature: ", true, invoke(_athos, methodName, esa));
        esa.setWebDisplayPolicy(DisplayPolicy.HIDDEN);
        assertEquals("Unexpected return for hidden product: ", false, invoke(_athos, methodName, esa));

        ToolResourceWritable tool = new ToolResourceImpl(null);
        assertEquals("Unexpected return for null visibility tool: ", true, invoke(_athos, methodName, tool));
        tool.setWebDisplayPolicy(DisplayPolicy.HIDDEN);
        assertEquals("Unexpected return for hidden tool: ", false, invoke(_athos, methodName, tool));

        // not webdisplayable
        AdminScriptResourceWritable admin = new AdminScriptResourceImpl(null);
        assertEquals("Unexpected return fo admin script: ", false, invoke(_athos, methodName, admin));

        ConfigSnippetResourceWritable config = new ConfigSnippetResourceImpl(null);
        assertEquals("Unexpected return for config snippet: ", false, invoke(_athos, methodName, config));

        SampleResourceWritable sample = new SampleResourceImpl(null);
        assertEquals("Unexpected return for sample: ", false, invoke(_athos, methodName, sample));
    }
}
