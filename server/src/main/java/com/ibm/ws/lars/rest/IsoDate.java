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
package com.ibm.ws.lars.rest;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Contains utility methods for parsing and writing the ISO 8601 datetime format
 */
public class IsoDate {

    private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSXXX";

    private static final ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            DateFormat df = new SimpleDateFormat(DATE_FORMAT);
            df.setTimeZone(UTC_TZ);
            return df;
        }
    };

    /**
     * Format a date using the ISO 8601 format
     *
     * @param date the date to format
     * @return the date in ISO 8601
     */
    public static String format(Date date) {
        return dateFormat.get().format(date);
    }

    /**
     * Parse an ISO 8601 date/time
     *
     * @param source the ISO 8601 date/time
     * @return the date object
     * @throws ParseException if source does not represent an ISO 8601 date/time
     */
    public static Date parse(String source) throws ParseException {
        return dateFormat.get().parse(source);
    }

}
