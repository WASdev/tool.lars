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
package com.ibm.ws.lars.testutils.fixtures;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;

import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.RepositoryWriteableClient;
import com.ibm.ws.repository.transport.client.RestClient;

/**
 * A repository fixture for a legacy Massive server.
 */
public class MassiveRepositoryFixture extends LarsRepositoryFixture {

    public static final MassiveRepositoryFixture createFixture(String repositoryUrl, String apiKey, String adminId, String adminPassword) {
        RestRepositoryConnection adminConnection = new RestRepositoryConnection(adminId, adminPassword, apiKey, repositoryUrl);
        RestRepositoryConnection userConnection = new RestRepositoryConnection(null, null, apiKey, repositoryUrl);

        RepositoryReadableClient adminClient = adminConnection.createClient();
        RepositoryWriteableClient writableClient = (RepositoryWriteableClient) adminClient;
        RepositoryReadableClient userClient = userConnection.createClient();
        return new MassiveRepositoryFixture(adminConnection, userConnection, adminClient, writableClient, userClient, repositoryUrl);
    }

    /**
     * @param adminConnection
     * @param userConnection
     * @param adminClient
     * @param writableClient
     * @param userClient
     * @param repositoryUrl
     */
    protected MassiveRepositoryFixture(RepositoryConnection adminConnection, RepositoryConnection userConnection, RepositoryReadableClient adminClient,
                                       RepositoryWriteableClient writableClient, RepositoryReadableClient userClient, String repositoryUrl) {
        super(adminConnection, userConnection, adminClient, writableClient, userClient, repositoryUrl);
    }

    @Override
    public boolean isUpdateSupported() {
        return true;
    }

    @Override
    public String getHostedFileRoot() throws URISyntaxException {
        // Test files are served under / on the same host and port as the test repository
        URI uri = new URI(repositoryUrl);
        URI baseUri = new URI("http", null, uri.getHost(), -1, null, null, null);
        return baseUri.toString();
    }

    @Override
    public void refreshTextIndex(String assetId) throws IOException {
        boolean refreshed = false;
        int retryCount = 0;
        while (!refreshed && retryCount < 5) {
            try {
                retryCount++;
                // Trigger indexing on elastic search...
                URL urlToRepo = new URL(repositoryUrl);
                URL refreshUrl = new URL("http", urlToRepo.getHost(), 9200, "/assets/_refresh");
                HttpURLConnection urlCon = (HttpURLConnection) refreshUrl.openConnection();
                urlCon.setRequestMethod("POST");
                urlCon.getResponseCode();

                URL checkIfRefreshed = new URL("http", urlToRepo.getHost(), 9200, "/assets/asset/" + assetId);
                HttpURLConnection checkUrlCon = (HttpURLConnection) checkIfRefreshed.openConnection();
                checkUrlCon.setDoInput(true);
                checkUrlCon.setRequestMethod("GET");
                int respCode = checkUrlCon.getResponseCode();

                if (respCode == 200) {
                    InputStream inputStream = checkUrlCon.getInputStream();
                    String inputStreamString = null;
                    if (inputStream != null) {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        int read = -1;
                        while ((read = inputStream.read()) != -1) {
                            outputStream.write(read);
                        }
                        inputStreamString = outputStream.toString(RestClient.getCharset(checkUrlCon.getContentType()));

                        if (inputStreamString.contains("\"found\":true")) {
                            refreshed = true;
                        } else {
                            // Give up after 5 fails
                            if (retryCount >= 5) {
                                refreshed = true;
                            }
                            try {
                                // TODO this should be enough ... if it DOES happen again we might add an inner loop
                                // to retry the checkIfRefreshed URL
                                Thread.sleep(1000);
                            } catch (InterruptedException ie) {
                                // Just swallow this
                            }
                        }
                    }
                }

                // Now validate that the index that elastic search has created is usable by Massive
                if (refreshed) {
                    refreshed = verifyElasticSearchIndexThroughMassive(userConnection, assetId);
                }

            } catch (IOException ex) {
                // I think an exception can be thrown if the refresh hasn't finished, wait 1 second and retry. Give up after 5 times
                // Give up after 5 fails and let exception propogate up
                if (retryCount >= 5) {
                    throw ex;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    // Just swallow this
                }

            } finally {
                if (retryCount >= 5 && !refreshed) {
                    throw new IOException("Failed to refresh elastic ");
                }
            }
        }
    }

    private boolean verifyElasticSearchIndexThroughMassive(RepositoryConnection connection, String assetId) {
        boolean refreshed = true;
        try {
            // check that we can get the target asset through a massive find
            RepositoryResource mr = connection.getResource(assetId);
            String name = mr.getName();
            Collection<? extends RepositoryResource> results = new RepositoryConnectionList(connection).findResources(name, null, null, null);

            boolean found = false;
            // Loop through the results from the find to ensure the asset we are keying on is in the returned list
            for (RepositoryResource result : results) {
                if (result.getId().equals(assetId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                refreshed = false; // invalidate the index
            }
        } catch (Exception e) {
            refreshed = false;
            e.printStackTrace();
        }

        return refreshed;
    }

    @Override
    public String toString() {
        return "Massive Repo";
    }

}
