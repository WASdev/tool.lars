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

package com.ibm.ws.lars.upload.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;

import mockit.Mock;
import mockit.MockUp;

import org.junit.After;
import org.junit.Test;

import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryBackendIOException;
import com.ibm.ws.repository.exceptions.RepositoryBackendRequestFailureException;
import com.ibm.ws.repository.exceptions.RepositoryBadDataException;
import com.ibm.ws.repository.exceptions.RepositoryResourceDeletionException;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.EsaResourceImpl;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;

public class DeleteTest {

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private final PrintStream output = new PrintStream(baos);
    private final Main tested = new Main(output);

    @After
    public void tearDown() {
        output.close();
    }

    @Test
    public void testNoUrl() {

        // Should throw an exception and print out a help message

        try {
            tested.run(new String[] { "--delete" });
        } catch (ClientException e) {
            assertEquals("Unexpected exception message", Main.MISSING_URL, e.getMessage());
            String outputString = baos.toString();
            assertTrue("The expected help output wasn't produced, was:\n" + outputString, TestUtils.checkForHelpMessage(outputString));
            return;
        }
        fail("The expected client exception was not thrown");
    }

    @Test
    public void testDeleteNone() {

        try {
            tested.run(new String[] { "--delete", "--url=http://foobar" });
        } catch (ClientException e) {
            assertEquals("Unexpected exception message", Main.NO_IDS_FOR_DELETE, e.getMessage());
            String outputString = baos.toString();
            assertTrue("The expected help output wasn't produced, was:\n" + outputString, TestUtils.checkForHelpMessage(outputString));
            return;
        }
        fail("The expected ClientException was not thrown");
    }

    @Test
    public void testDeleteNonExistent() throws ClientException {

        new MockUp<RestRepositoryConnection>() {
            @Mock
            public RepositoryResource getResource(String id) throws RepositoryBackendException, RepositoryBadDataException {
                URL url = null;
                try {
                    url = new URL("http://localhost");
                } catch (MalformedURLException e1) {
                    // shouldn't happen
                }
                RequestFailureException e = new RequestFailureException(404, "not found", url, "not found");
                throw new RepositoryBackendRequestFailureException(e, null);
            }
        };

        tested.run(new String[] { "--delete", "--url=http://localhost:9080", "9999" });

        String output = baos.toString();

        assertTrue("Expected message was missing. Output was:\n" + output, output.contains("Asset 9999 not deleted"));
    }

    @Test
    public void testDelete() throws ClientException {

        new MockUp<RestRepositoryConnection>() {
            @Mock
            public RepositoryResource getResource(String id) throws RepositoryBackendException, RepositoryBadDataException {
                RepositoryResource deleteable = new EsaResourceImpl(null) {
                    @Override
                    public void delete() throws RepositoryResourceDeletionException {
                        return;
                    }
                };
                return deleteable;
            }
        };

        tested.run(new String[] { "--delete", "--url=http://localhost:9080", "9999" });

        String output = baos.toString();

        assertTrue("Expected message was missing. Output was:\n" + output, output.contains("Deleted asset 9999"));
    }

    @Test
    public void testMixedDelete() throws ClientException {

        new MockUp<RestRepositoryConnection>() {
            @Mock
            public RepositoryResource getResource(String id) throws RepositoryBackendException, RepositoryBadDataException {
                if (id.equals("1234")) {
                    URL url = null;
                    try {
                        url = new URL("http://localhost");
                    } catch (MalformedURLException e1) {
                        // shouldn't happen
                    }
                    RequestFailureException e = new RequestFailureException(404, "not found", url, "not found");
                    throw new RepositoryBackendRequestFailureException(e, null);
                }
                RepositoryResource deleteable = new EsaResourceImpl(null) {
                    @Override
                    public void delete() throws RepositoryResourceDeletionException {
                        return;
                    }
                };
                return deleteable;
            }
        };

        tested.run(new String[] { "--delete", "--url=http://localhost:9080", "9999", "1234", "abcdef" });

        String output = baos.toString();

        assertTrue("Expected message was missing. Output was:\n" + output, output.contains("Deleted asset 9999"));
        assertTrue("Expected message was missing. Output was:\n" + output, output.contains("Asset 1234 not deleted"));
        assertTrue("Expected message was missing. Output was:\n" + output, output.contains("Deleted asset abcdef"));

    }

    @Test
    public void testDeleteNetworkErrorOnRetrieve() {
        new MockUp<RestRepositoryConnection>() {
            @Mock
            public RepositoryResource getResource(String id) throws RepositoryBackendException, RepositoryBadDataException {
                throw new RepositoryBackendIOException("The network isn't there!", null);
            }
        };

        try {
            tested.run(new String[] { "--delete", "--url=http://localhost:9080", "9999" });
        } catch (ClientException e) {

            String output = baos.toString();

            assertTrue("Expected message was missing. Output was:\n" + output, output.contains("Asset 9999 not deleted"));
            assertTrue("Expected message was missing. Output was:\n" + output, output.contains(Main.CONNECTION_PROBLEM));
            assertTrue("Unexpected message in output. Output was:\n" + output, !output.contains("Deleted asset 9999"));
            return;
        }

        fail("The expected ClientException wasn't thrown");
    }

    @Test
    public void testDeleteServerErrorOnRetrieve() {
        new MockUp<RestRepositoryConnection>() {
            @Mock
            public RepositoryResource getResource(String id) throws RepositoryBackendException, RepositoryBadDataException {
                URL url = null;
                try {
                    url = new URL("http://localhost");
                } catch (MalformedURLException e1) {
                    // shouldn't happen
                }
                RequestFailureException e = new RequestFailureException(500, "Internal Server error", url, "Internal Server error");
                throw new RepositoryBackendRequestFailureException(e, null);
            }
        };

        try {
            tested.run(new String[] { "--delete", "--url=http://localhost:9080", "9999" });
        } catch (ClientException e) {

            String output = baos.toString();

            assertTrue("Expected message was missing. Output was:\n" + output, output.contains("Asset 9999 not deleted"));
            assertTrue("Expected message was missing. Output was:\n" + output, output.contains(Main.SERVER_ERROR));
            assertTrue("Unexpected message in output. Output was:\n" + output, !output.contains("Deleted asset 9999"));
            return;
        }

        fail("The expected ClientException wasn't thrown");
    }

    @Test
    public void testDeleteServerErrorOnDelete() {

        new MockUp<RestRepositoryConnection>() {
            @Mock
            public RepositoryResource getResource(String id) throws RepositoryBackendException, RepositoryBadDataException {
                RepositoryResource deleteable = new EsaResourceImpl(null) {
                    @Override
                    public void delete() throws RepositoryResourceDeletionException {
                        throw new RepositoryResourceDeletionException("The network is gone!", getId());
                    }
                };
                return deleteable;
            }
        };

        try {
            tested.run(new String[] { "--delete", "--url=http://localhost:9080", "9999" });
        } catch (ClientException e) {

            String output = baos.toString();

            assertTrue("Expected message was missing. Output was:\n" + output, !output.contains("Deleted asset 9999"));
            assertTrue("Expected message was missing. Output was:\n" + output, output.contains("Asset 9999 not deleted"));
            return;
        }
        fail("The expected ClientException wasn't thrown");

    }

}
