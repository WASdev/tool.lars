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
package com.ibm.ws.lars.testutils.matchers;

import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.List;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matches against an expected summary result of the form
 *
 * <pre>
 * {
 *      "filterName": field
 *      "filterValue": [value1, value2, ...]
 * }
 * </pre>
 * <p>
 * To match, the map must have only the two keys "filterName" and "filterValue", the field must
 * match and the values must match but their order may be different.
 * <p>
 * If you have multiple results to match you can do
 * <code>assertThat(expected, containsInAnyOrder(summaryResult("foo","foo1","foo3"), summaryResult("bar")))</code>.
 */
public class SummaryResultMatcher extends TypeSafeMatcher<Map<String, Object>> {

    private final String expectedField;
    private final String[] expectedValues;

    private SummaryResultMatcher(String field, String... values) {
        this.expectedField = field;
        this.expectedValues = values;
    }

    /** {@inheritDoc} */
    @Override
    public void describeTo(Description description) {
        description.appendText("{");
        description.appendText("filterName=").appendValue(expectedField);
        description.appendText(", ");
        description.appendText("filterValue=").appendValueList("[", ", ", "]", expectedValues);
        description.appendText("}");
    }

    /** {@inheritDoc} */
    @Override
    protected boolean matchesSafely(Map<String, Object> item) {
        // Check for correct length
        if (item.size() != 2) {
            return false;
        }

        try {
            // Check the field name matches
            String actualField = (String) item.get("filterName");
            if (actualField == null || !actualField.equals(expectedField)) {
                return false;
            }

            // Check the values match
            List<?> actualValues = (List<?>) item.get("filterValue");
            if (actualValues == null || !containsInAnyOrder(expectedValues).matches(actualValues)) {
                return false;
            }
        } catch (ClassCastException ex) {
            // If one of the fields had the wrong type, then it doesn't match
            return false;
        }

        return true;
    }

    /**
     * Creates a {@link SummaryResultMatcher} to match a result with the given field and values
     *
     * @param field the field name
     * @param values the values that should be matched
     * @return the matcher
     */
    public static SummaryResultMatcher summaryResult(String field, String... values) {
        return new SummaryResultMatcher(field, values);
    }

}
