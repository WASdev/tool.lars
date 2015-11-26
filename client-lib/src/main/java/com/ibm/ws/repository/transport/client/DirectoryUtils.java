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
package com.ibm.ws.repository.transport.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.zip.ZipFile;

/**
 *
 */
public class DirectoryUtils {

    public static FileInputStream createFileInputStream(final File file) throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<FileInputStream>() {
                @Override
                public FileInputStream run() throws IOException {
                    return new FileInputStream(file);
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getCause();
        }
    }

    public static FileOutputStream createFileOutputStream(final File file) throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<FileOutputStream>() {
                @Override
                public FileOutputStream run() throws IOException {
                    return new FileOutputStream(file);
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getCause();
        }
    }

    public static boolean exists(final File file) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return file.exists();
            }
        });
    }

    public static boolean isDirectory(final File file) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return file.isDirectory();
            }
        });
    }

    public static boolean mkDirs(final File file) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return file.mkdirs();
            }
        });
    }

    public static long length(final File file) {
        return AccessController.doPrivileged(new PrivilegedAction<Long>() {
            @Override
            public Long run() {
                return file.length();
            }
        });
    }

    public static String[] list(final File file) {
        return AccessController.doPrivileged(new PrivilegedAction<String[]>() {
            @Override
            public String[] run() {
                return file.list();
            }
        });
    }

    public static File[] listFiles(final File f) {
        return AccessController.doPrivileged(new PrivilegedAction<File[]>() {
            @Override
            public File[] run() {
                return f.listFiles();
            }
        });
    }

    public static File[] listFiles(final File f, final FilenameFilter filter) {
        return AccessController.doPrivileged(new PrivilegedAction<File[]>() {
            @Override
            public File[] run() {
                return f.listFiles(filter);
            }
        });
    }

    public static boolean delete(final File f) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return f.delete();
            }
        });
    }

    public static ZipFile createZipFile(final File f) throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<ZipFile>() {
                @Override
                public ZipFile run() throws IOException {
                    return new ZipFile(f);
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getCause();
        }
    }
}
