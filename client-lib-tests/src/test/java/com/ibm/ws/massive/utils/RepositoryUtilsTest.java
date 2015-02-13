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

package com.ibm.ws.massive.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Locale;

import org.junit.Test;

/**
 * Tests for {@link RepositoryUtils}
 */
public class RepositoryUtilsTest {

    /**
     * Test of {@link RepositoryUtils#localeForString(String)} just supplying a language.
     */
    @Test
    public void testLocaleForString_languageOnly() {
        Locale locale = RepositoryUtils.localeForString("en");
        assertEquals("The locale should have been parsed correctly", Locale.ENGLISH, locale);
    }

    /**
     * Test of {@link RepositoryUtils#localeForString(String)} supplying a language and country.
     */
    @Test
    public void testLocaleForString_languageAndCountry() {
        Locale locale = RepositoryUtils.localeForString("en_GB");
        assertEquals("The locale should have been parsed correctly", Locale.UK, locale);
    }

    /**
     * Test of {@link RepositoryUtils#localeForString(String)} supplying a language, country and varient.
     */
    @Test
    public void testLocaleForString_allParts() {
        Locale locale = RepositoryUtils.localeForString("en_GB_welsh");
        assertEquals("The locale should have been parsed language", "en", locale.getLanguage());
        assertEquals("The locale should have been parsed country", "GB", locale.getCountry());
        assertEquals("The locale should have been parsed varient", "welsh", locale.getVariant());
    }

    /**
     * Test of {@link RepositoryUtils#localeForString(String)} supplying four parts, the last should be added to the vairent as well.
     */
    @Test
    public void testLocaleForString_fourParts() {
        Locale locale = RepositoryUtils.localeForString("en_GB_welsh_valleys");
        assertEquals("The locale should have been parsed language", "en", locale.getLanguage());
        assertEquals("The locale should have been parsed country", "GB", locale.getCountry());
        assertEquals("The locale should have been parsed varient", "welsh_valleys", locale.getVariant());
    }

    /**
     * Test of {@link RepositoryUtils#localeForString(String)} supplying <code>null</code>.
     */
    @Test
    public void testLocaleForString_null() {
        assertNull("A null entry should make a null locale", RepositoryUtils.localeForString(null));
        assertNull("An empty string should make a null locale", RepositoryUtils.localeForString(""));
    }

}
