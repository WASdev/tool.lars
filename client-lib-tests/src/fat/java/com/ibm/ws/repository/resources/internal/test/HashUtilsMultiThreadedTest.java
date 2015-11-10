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

package com.ibm.ws.repository.resources.internal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.junit.Test;

import com.ibm.ws.repository.common.utils.internal.HashUtils;

/**
 *
 */
public class HashUtilsMultiThreadedTest {

    private static final String SHA256 = "SHA-256";
    private static final String MD5 = "MD5";

    /**
     * Calculate the SHA256 hash of all files in a directory using multithreading
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void hashMultiFilesSHA256() throws IOException, InterruptedException {
        System.out.println("============ hashMultiFilesSHA256");
        hashMultiFiles(SHA256);
    }

    /**
     * Calculate the MD5 hash of all files in a directory using multithreading
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void hashMultiFilesMD5() throws IOException, InterruptedException {
        System.out.println("============ hashMultiFilesMD5");
        hashMultiFiles(MD5);
    }

    /**
     * Test that a directory of files can be hashed in parallel
     *
     * @param hashType - SHA256 or MD5
     * @throws InterruptedException
     */
    private void hashMultiFiles(final String hashType) throws IOException, InterruptedException {
        final File dir = new File("resources");
        final Hashtable<String, String> singleThread = new Hashtable<String, String>();
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.isFile()) {
                if (hashType.equals(SHA256)) {
                    singleThread.put(f.getAbsolutePath(), HashUtils.getFileSHA256String(f));
                } else if (hashType.equals(MD5)) {
                    singleThread.put(f.getAbsolutePath(), HashUtils.getFileMD5String(f));
                } else {
                    fail("invalid hash type requested: " + hashType);
                }

            }
        }
        ArrayList<Thread> threads = new ArrayList<Thread>();
        for (final File f : files) {
            if (f.isFile()) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String single = singleThread.get(f.getAbsolutePath());
                            String multi = null;

                            if (hashType.equals(SHA256)) {
                                multi = HashUtils.getFileSHA256String(f);
                            } else if (hashType.equals(MD5)) {
                                multi = HashUtils.getFileMD5String(f);
                            } else {
                                fail("invalid hash type requested: " + hashType);
                            }

                            assertEquals(hashType + " hashcode produced for " + f.getName() + " does not match", multi, single);
                            System.out.println(f + " ok");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
                Thread t = new Thread(runnable);
                threads.add(t);
                t.start();
            }
        }
        // Join the threads so that the output completes before the next test starts
        for (Thread t : threads) {
            t.join();
        }
    }

}
