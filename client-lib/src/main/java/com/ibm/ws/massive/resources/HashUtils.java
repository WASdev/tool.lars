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
package com.ibm.ws.massive.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * HashUtils replaces the old MD5Utils to generate both the MD5 and SHA256 hash keys.
 */
public class HashUtils {

    static final char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    static final String SHA256 = "SHA-256";
    static final String MD5 = "MD5";

    /**
     * Calculate MD5 hash of a File
     *
     * @param file - the File to hash
     * @return the MD5 hash value
     * @throws IOException
     */
    public static String getFileMD5String(File file) throws IOException {
        MessageDigest messageDigest = getMessageDigest(MD5);
        return getFileHashString(file, messageDigest);
    }

    /**
     * Calculate SHA-256 hash of a File
     *
     * @param file - the File to hash
     * @return the SHA-256 hash value
     * @throws IOException
     */
    public static String getFileSHA256String(File file) throws IOException {
        MessageDigest messageDigest = getMessageDigest(SHA256);
        return getFileHashString(file, messageDigest);
    }

    private static String getFileHashString(File file, MessageDigest messagedigest) throws IOException {
        InputStream fis = null;
        try {
            fis = new FileInputStream(file);

            byte[] buffer = new byte[1024];
            int numRead = 0;
            while ((numRead = fis.read(buffer)) > 0) {
                messagedigest.update(buffer, 0, numRead);
            }
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (Exception e) {
                }
        }

        return byteArrayToHexString(messagedigest.digest());
    }

    private static String byteArrayToHexString(byte[] byteArray) {

        StringBuffer stringbuffer = new StringBuffer(2 * byteArray.length);
        for (int i = 0; i < byteArray.length; i++) {
            char upper = hexDigits[(byteArray[i] & 0xf0) >> 4];
            char lower = hexDigits[byteArray[i] & 0xf];
            stringbuffer.append(upper);
            stringbuffer.append(lower);
        }
        return stringbuffer.toString();

    }

    /**
     * this code replaces the creation of the message digest in a static code block
     * as that was found to fail when multi threaded.
     *
     * @param digestType - MD5 or SHA-256
     * @return the MessageDigest of the requested type
     */
    private static MessageDigest getMessageDigest(String digestType) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(digestType);
        } catch (NoSuchAlgorithmException e) {
            //should not happen
            throw new RuntimeException(e);
        }
        return messageDigest;
    }

}
