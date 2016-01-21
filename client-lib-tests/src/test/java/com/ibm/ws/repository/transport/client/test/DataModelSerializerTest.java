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

package com.ibm.ws.repository.transport.client.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.transport.client.DataModelSerializer;
import com.ibm.ws.repository.transport.client.JSONIgnore;
import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.WlpInformation;

public class DataModelSerializerTest {

    @BeforeClass
    public static void setup() {
        DataModelSerializer.IGNORE_UNKNOWN_FIELDS = false;
    }

    @AfterClass
    public static void teardown() {
        DataModelSerializer.IGNORE_UNKNOWN_FIELDS = true;
    }

    public static class IntTest {
        public int getTestValue() {
            return 1337;
        }
    }

    public static class ByteTest {
        public byte getTestValue() {
            return 19;
        }
    }

    public static class ShortTest {
        public short getTestValue() {
            return 259;
        }
    }

    public static class LongTest {
        public long getTestValue() {
            return 2147484984L;
        }
    }

    public static class BooleanTest {
        public boolean getTestValue() {
            return true;
        }
    }

    public static class CharTest {
        public char getTestValue() {
            return 'x';
        }
    }

    public static class NullTest {
        public Object getTestValue() {
            return null;
        }
    }

    @Test
    public void testPrimitiveSerialization() throws IOException {
        String result;

        //normal primitive tests.

        Object testInt = new IntTest();
        result = DataModelSerializer.serializeAsString(testInt);
        assertTrue("Primitive int encoded incorrectly : " + result, result.matches("(?s).*\"testValue\"[\\s]*:[\\s]*1337.*"));

        Object testByte = new ByteTest();
        result = DataModelSerializer.serializeAsString(testByte);
        assertTrue("Primitive byte encoded incorrectly : " + result, result.matches("(?s).*\"testValue\"[\\s]*:[\\s]*19.*"));

        Object testShort = new ShortTest();
        result = DataModelSerializer.serializeAsString(testShort);
        assertTrue("Primitive short encoded incorrectly : " + result, result.matches("(?s).*\"testValue\"[\\s]*:[\\s]*259.*"));

        Object testLong = new LongTest();
        result = DataModelSerializer.serializeAsString(testLong);
        assertTrue("Primitive long encoded incorrectly : " + result, result.matches("(?s).*\"testValue\"[\\s]*:[\\s]*2147484984.*"));

        Object testBool = new BooleanTest();
        result = DataModelSerializer.serializeAsString(testBool);
        assertTrue("Primitive boolean encoded incorrectly : " + result, result.matches("(?s).*\"testValue\"[\\s]*:[\\s]*true.*"));

        //tests unique to the wibblifier's current implementation.

        //we choose not to serialize nulls, else every object would be huge.
        Object testNull = new NullTest();
        result = DataModelSerializer.serializeAsString(testNull);
        assertFalse("Primitive null encoded incorrectly : " + result, result.matches("(?s).*\"testValue\"[\\s]*:[\\s]*null.*"));

        //we do not support char for serialization.
        Object testChar = new CharTest();
        try {
            result = DataModelSerializer.serializeAsString(testChar);
            fail("char is supported, this is most unexpected. " + result);
        } catch (IOException io) {
            assertTrue("char is unsupported. : " + io.getCause().getMessage(),
                       io.getCause().getMessage().contains("Unknown data model entity getTestValue of type java.lang.Character"));
        }
    }

    public static class BasicArrayTest {
        public int[] getTestValue() {
            return new int[] { 1, 2, 3 };
        }
    }

    @Test
    public void testBasicArraySerialization() throws IOException {

        //the current implementation does not handle arrays of any sort,
        //(it will handle Collections, use those instead!)

        String result;
        Object testBasicArray = new BasicArrayTest();
        try {
            result = DataModelSerializer.serializeAsString(testBasicArray);
            fail("basic array returns are not implemented! this should have failed! " + result);
        } catch (IOException io) {
            assertTrue("basic array is unsupported. : " + io.getCause().getMessage(), io.getCause().getMessage().contains("Unknown data model entity getTestValue of type [I "));
        }
    }

    public static class BasicIntegerCollectionTest {
        public Collection<Integer> getTestValue() {
            return Arrays.asList(new Integer[] { 1, 2, 3 });
        }
    }

    public static class BasicStringCollectionTest {
        public Collection<String> getTestValue() {
            return Arrays.asList(new String[] { "a", "b", "c" });
        }
    }

    @Test
    public void testCollectionSerialization() throws IOException {
        String result;
        Object testBasicIntegerCollection = new BasicIntegerCollectionTest();
        try {
            result = DataModelSerializer.serializeAsString(testBasicIntegerCollection);
            fail("collections other than string/datamodel are not implemented! this should have failed! " + result);
        } catch (IOException io) {
            assertTrue("collection of non string is unsupported. : " + io.getCause().getMessage(),
                       io.getCause().getMessage().contains("serialization only supported for Collections of String, or other Data Model elements"));
        }

        Object testBasicStringCollection = new BasicStringCollectionTest();
        result = DataModelSerializer.serializeAsString(testBasicStringCollection);

        //regexp ftw! =)
        //  (?s)     enables matching over multiple lines.
        //  [\\s]*   0 or more white space chars.
        //  \\{      literal {
        //  \\[      literal [
        //  \\]      literal ]
        //  \\"      literal "
        assertTrue("Collection of string encoded incorrectly : " + result, result.matches("(?s)"
                                                                                          + "\\{"
                                                                                          + "[\\s]*\"testValue\"[\\s]*:[\\s]*"
                                                                                          + "[\\s]*\\["
                                                                                          + "[\\s]*\"a\""
                                                                                          + "[\\s]*,"
                                                                                          + "[\\s]*\"b\""
                                                                                          + "[\\s]*,"
                                                                                          + "[\\s]*\"c\""
                                                                                          + "[\\s]*\\]"
                                                                                          + "[\\s]*\\}"));
    }

    public static class GetterTestClass {
        public String getFish() {
            return "Fish";
        }

        public String getFish(int argument) {
            return "ERROR";
        }

        public String getFish(String... args) {
            return "ERROR";
        }
    }

    @Test
    public void testDuplicateGetters() throws IOException {
        String result;
        Object testGetters = new GetterTestClass();
        result = DataModelSerializer.serializeAsString(testGetters);
        assertTrue("Incorrect getter was used : " + result, result.matches("(?s).*\"fish\"[\\s]*:[\\s]*\"Fish\".*"));
    }

    public static class StringGetter {
        public StringGetter() {}

        //ignore the warning.. eclipse is trying to tell me this is bad ;p
        //but that's the whole point of this test =)
        public String getString() {
            return "Fish";
        }
    }

    @Test
    public void testSerializeBadlyNamedClass() throws IOException {
        String result;
        Object testGetters = new StringGetter();
        result = DataModelSerializer.serializeAsString(testGetters);
        assertTrue("Incorrect getter was used : " + result, result.matches("(?s).*\"string\"[\\s]*:[\\s]*\"Fish\".*"));
    }

    public static class UnwantedFieldsTest {
        String fred;

        public String getFred() {
            return fred;
        }

        public void setFred(String fred) {
            this.fred = fred;
        }
    }

    @Test
    public void testValidation() throws Exception {
        WlpInformation test = DataModelSerializer.deserializeObject(
                                                                    new ByteArrayInputStream("{ \"wlpInformationVersion\":\"1.1\"}".getBytes()), WlpInformation.class);
        assertEquals("Wrong version", "1.1", test.getWlpInformationVersion());

        try {
            test = DataModelSerializer.deserializeObject(
                                                         new ByteArrayInputStream("{ \"wlpInformationVersion\":\"0.9\"}".getBytes()), WlpInformation.class);
            fail("Version 0.9 is too low: exception expected");
        } catch (BadVersionException bvx) {
            assertEquals("1.0", bvx.getMinVersion());
            assertEquals("2.0", bvx.getMaxVersion());
            assertEquals("0.9", bvx.getBadVersion());
        }

        try {
            test = DataModelSerializer.deserializeObject(
                                                         new ByteArrayInputStream("{ \"wlpInformationVersion\":\"2.0\"}".getBytes()), WlpInformation.class);
            fail("Version 2.0 is too high: exception expected");
        } catch (BadVersionException bvx) {
            assertEquals("2.0", bvx.getBadVersion());
        }
    }

    /**
     * A new "install" visibility was added for story 141811, test that we can read it
     *
     * @throws Exception
     */
    @Test
    public void testVisibility() throws Exception {
        WlpInformation test = DataModelSerializer.deserializeObject(
                                                                    new ByteArrayInputStream("{ \"wlpInformationVersion\":\"1.0\", \"visibility\":\"INSTALL\"}".getBytes()),
                                                                    WlpInformation.class);
        assertEquals("Wrong visibility read in", Visibility.INSTALL, test.getVisibility());
    }

    // Do not make this class implement VersionableContent.
    public static class JustAnotherBean {
        public JustAnotherBean() {}

        String field;

        public void setField(String s) {
            field = s;
        }

        public String getField() {
            return field;
        }
    }

    @Test
    public void testItsOkNotToHaveAVersion() throws Exception {
        JustAnotherBean test = DataModelSerializer.deserializeObject(
                                                                     new ByteArrayInputStream("{ \"field\":\"muddy\"}".getBytes()), JustAnotherBean.class);
        assertEquals("Field not muddy enough", "muddy", test.getField());
    }

    @Test
    public void testDeserializeOkWithUnwantedFieldPresent() throws IOException, BadVersionException {
        //for this test, we need to re-enable ignoring unknowns..
        try {
            DataModelSerializer.IGNORE_UNKNOWN_FIELDS = true;

            UnwantedFieldsTest uft1 = DataModelSerializer.deserializeObject(new ByteArrayInputStream("{ \"fred\":\"fish fish\",\"barney\":21 }".getBytes()),
                                                                            UnwantedFieldsTest.class);
            assertEquals("Fred was not set correctly during unwanted field test. Fred was " + uft1.getFred(), uft1.getFred(), "fish fish");
            UnwantedFieldsTest uft2 = DataModelSerializer.deserializeObject(new ByteArrayInputStream("{\"barney\":21, \"fred\":\"fish fish\"}".getBytes()),
                                                                            UnwantedFieldsTest.class);
            assertEquals("Fred was not set correctly during unwanted field test. Fred was " + uft2.getFred(), uft2.getFred(), "fish fish");
            UnwantedFieldsTest uft3 = DataModelSerializer.deserializeObject(new ByteArrayInputStream("{\"wilma\":\"monkey\", \"fred\":\"fish fish\"}".getBytes()),
                                                                            UnwantedFieldsTest.class);
            assertEquals("Fred was not set correctly during unwanted field test. Fred was " + uft3.getFred(), uft3.getFred(), "fish fish");

            //now retry with the ignore turned off again..
            try {
                DataModelSerializer.IGNORE_UNKNOWN_FIELDS = false;
                DataModelSerializer.deserializeObject(new ByteArrayInputStream("{\"fred\":\"fish fish\",\"barney\":21}".getBytes()), UnwantedFieldsTest.class);
                fail("Wibblifier failed to report unknown field when told not to ignore them");
            } catch (IllegalStateException ex) {
                //expected.
            }
        } finally {
            //reset it for the other tests (finally block to really make sure!)
            DataModelSerializer.IGNORE_UNKNOWN_FIELDS = false;
        }
    }

    public static class NullFieldTest {
        String nullField;

        public String getNullField() {
            return nullField;
        }

        public void setNullField(String nullField) {
            this.nullField = nullField;
        }
    }

    @Test
    public void testDeserializeNull() throws Exception {

        NullFieldTest nft = DataModelSerializer.deserializeObject(new ByteArrayInputStream("{ \"nullField\": null }".getBytes()),
                                                                  NullFieldTest.class);
        assertEquals("Null field was not set correctly during null test,",
                     null, nft.getNullField());
    }

    public static class LocaleTest {
        Locale locale;

        public Locale getLocale() {
            return locale;
        }

        public void setLocale(Locale locale) {
            this.locale = locale;
        }
    }

    @Test
    public void testLocaleRoundTrip() throws Exception {
        LocaleTest lt = new LocaleTest();
        Locale l = new Locale("en", "GB", "pirate");
        lt.setLocale(l);
        String data = DataModelSerializer.serializeAsString(lt);
        LocaleTest gotBack = DataModelSerializer.deserializeObject(new ByteArrayInputStream(data.getBytes()), LocaleTest.class);
        assertEquals("Locale did not roundtrip", gotBack.getLocale(), l);
    }

    public static class JSONIgnoreGetterTest {
        String testData;

        @JSONIgnore
        public String getTestData() {
            return testData;
        }

        public void setTestData(String testData) {
            this.testData = testData;
        }
    }

    @Test
    public void testJSONIgnoreSetter() throws Exception {
        JSONIgnoreGetterTest ignore = new JSONIgnoreGetterTest();
        String testString = "wibble fish monkey";
        ignore.setTestData(testString);
        String data = DataModelSerializer.serializeAsString(ignore);
        assertFalse("Getter was invoked when it should not have been, so the data should not contain wibble fish monkey",
                    data.contains(testString));
    }

    public static class JSONIgnoreSetterTest {
        String testData;

        @JSONIgnore
        public void setTestData(String testData) {
            this.testData = testData;
        }

        public String getTestData() {
            return testData;
        }
    }

    @Test
    public void testJSONIgnoreGetter() throws Exception {
        JSONIgnoreSetterTest ignore = new JSONIgnoreSetterTest();
        String testString = "wibble fish monkey";
        ignore.setTestData(testString);
        String data = DataModelSerializer.serializeAsString(ignore);
        assertTrue("Getter wasn't invoked when it should have been, so the data should contain wibble fish monkey",
                   data.contains(testString));
        System.out.println("Data is " + data);
        JSONIgnoreSetterTest gotBack = DataModelSerializer.deserializeObject(new ByteArrayInputStream(data.getBytes()), JSONIgnoreSetterTest.class);
        System.out.println("Read back " + gotBack.getTestData());
        assertNull("Setter was invoked when it should not have been, so the data should be null", gotBack.getTestData());
    }

    @Test
    public void testIncompatibleChangesPutSomewhereElse() throws Exception {
        DataModelSerializer.IGNORE_UNKNOWN_FIELDS = true;
        Asset asset = new Asset();
        WlpInformation wlpInformation = new WlpInformation();
        wlpInformation.setVisibility(Visibility.INSTALL);
        asset.setWlpInformation(wlpInformation);
        String json = DataModelSerializer.serializeAsString(asset);
        JsonObject jsonObject = parseStringToJson(json);
        assertTrue("There should be a wlpInformation2 field", jsonObject.containsKey("wlpInformation2"));
        JsonObject wlpInformation1 = jsonObject.getJsonObject("wlpInformation");
        assertFalse("The first wlpinformation shouldn't contain the visibility: " + wlpInformation1, wlpInformation1.containsKey("visibility"));
        JsonObject wlpInformation2 = jsonObject.getJsonObject("wlpInformation2");
        assertEquals("The incompatible field should be stored in wlpInformation2", Visibility.INSTALL.toString(), wlpInformation2.getString("visibility"));
        Asset reReadAsset = DataModelSerializer.deserializeObject(new ByteArrayInputStream(json.getBytes()), Asset.class);
        assertEquals("The read in wlp inforamtion should say it has a visiblity of installer", Visibility.INSTALL, reReadAsset.getWlpInformation().getVisibility());
    }

    @Test
    public void testIncompatibleChangesPutSomewhereElseAndOriginalDataLoaded() throws Exception {
        DataModelSerializer.IGNORE_UNKNOWN_FIELDS = true;
        Asset asset = new Asset();
        WlpInformation wlpInformation = new WlpInformation();
        wlpInformation.setAppliesTo("wibble");
        wlpInformation.setVisibility(Visibility.INSTALL);
        asset.setWlpInformation(wlpInformation);
        String json = DataModelSerializer.serializeAsString(asset);
        JsonObject jsonObject = parseStringToJson(json);
        assertTrue("There should be a wlpInformation2 field", jsonObject.containsKey("wlpInformation2"));
        JsonObject wlpInformation1 = jsonObject.getJsonObject("wlpInformation");
        assertFalse("The first wlpinformation shouldn't contain the visibility: " + wlpInformation1, wlpInformation1.containsKey("visibility"));
        assertEquals("The first wlpinformation should contain the applies to: " + wlpInformation1, "wibble", wlpInformation1.getString("appliesTo"));
        JsonObject wlpInformation2 = jsonObject.getJsonObject("wlpInformation2");
        assertEquals("The incompatible field should be stored in wlpInformation2", Visibility.INSTALL.toString(), wlpInformation2.getString("visibility"));
        Asset reReadAsset = DataModelSerializer.deserializeObject(new ByteArrayInputStream(json.getBytes()), Asset.class);
        assertEquals("The read in wlp inforamtion should say it has a visiblity of installer", Visibility.INSTALL, reReadAsset.getWlpInformation().getVisibility());
        assertEquals("The read in wlp inforamtion should say it has the applies to set", "wibble", reReadAsset.getWlpInformation().getAppliesTo());
    }

    @Test
    public void testCompatibleChangeNotPutSomewhereElse() throws Exception {
        DataModelSerializer.IGNORE_UNKNOWN_FIELDS = true;
        Asset asset = new Asset();
        WlpInformation wlpInformation = new WlpInformation();
        wlpInformation.setVisibility(Visibility.PUBLIC);
        asset.setWlpInformation(wlpInformation);
        String json = DataModelSerializer.serializeAsString(asset);
        JsonObject jsonObject = parseStringToJson(json);
        assertFalse("There shouldn't be a wlpInformation2 field", jsonObject.containsKey("wlpInformation2"));
        JsonObject wlpInformation1 = jsonObject.getJsonObject("wlpInformation");
        assertEquals("The first wlpinformation should contain the visibility: " + wlpInformation1, Visibility.PUBLIC.toString(), wlpInformation1.getString("visibility"));
        Asset reReadAsset = DataModelSerializer.deserializeObject(new ByteArrayInputStream(json.getBytes()), Asset.class);
        assertEquals("The read in wlp inforamtion should say it has a visiblity of installer", Visibility.PUBLIC, reReadAsset.getWlpInformation().getVisibility());
    }

    private JsonObject parseStringToJson(String string) {
        StringReader reader = new StringReader(string);
        JsonReader jsonReader = Json.createReader(reader);
        JsonObject parsedObject = jsonReader.readObject();
        jsonReader.close();
        return parsedObject;
    }
}