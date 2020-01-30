/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.ibm.ws.lars.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.rules.ExternalResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.ws.lars.rest.exceptions.InvalidJsonAssetException;
import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.AssetList;
import com.ibm.ws.lars.rest.model.Attachment;
import com.ibm.ws.lars.rest.model.AttachmentList;
import com.ibm.ws.lars.testutils.FatUtils;
import com.mongodb.gridfs.GridFS;

/**
 * Context used by testcases to perform HTTP operations against one LARS server. Tests should use
 * this class rather than directly calling httpclient (or the java.net APIs!) directly.
 *
 * A RepositoryContext aims to encapsulate every parameter needed to communicate with the server
 * (host, port, protocol, user, password etc). Some tests may use more than one RepositoryContext
 * (for example, security tests that need to log on as more than one user).
 */
public class RepositoryContext extends ExternalResource {

    private final String hostname;
    private final int portNumber;
    private final String protocol;

    private final String fullURL;

    private static final CloseableHttpClient httpClient;

    private final String user;
    private final String password;
    private UsernamePasswordCredentials credentials;

    private final Redirects followRedirects;

    private HttpHost targetHost;
    private HttpClientContext httpClientContext;
    private RequestConfig requestConfig;

    private final static ObjectMapper jsonReader = new ObjectMapper();

    enum Protocol {
        HTTP, HTTPS
    }

    enum Redirects {
        FOLLOW, NO_FOLLOW
    }

    static final Map<Protocol, String> DEFAULT_URLS;
    static {
        DEFAULT_URLS = new HashMap<Protocol, String>();
        DEFAULT_URLS.put(Protocol.HTTP, "http://localhost:" + FatUtils.LIBERTY_PORT_HTTP + FatUtils.LARS_APPLICATION_ROOT);
        DEFAULT_URLS.put(Protocol.HTTPS, "https://localhost:" + FatUtils.LIBERTY_PORT_HTTPS + FatUtils.LARS_APPLICATION_ROOT);
    };

    /**
     * Special constant that can be passed as an expected response code to indicate that either a
     * 401 Not Authorized or a 403 Forbidden response is expected.
     */
    public static final int RC_REJECT = -2;

    @Override
    protected void before() throws InvalidJsonAssetException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        cleanRepo();
    }

    @Override
    protected void after() {
        try {
            cleanRepo();
        } catch (IOException | InvalidJsonAssetException e) {
            e.printStackTrace();
            fail("Unexpected exception cleaning repository or closing httpClient: " + e);
        }
    }

    static {
        /* Create the HTTPClient that we use to make all HTTP calls */
        HttpClientBuilder b = HttpClientBuilder.create();

        b.disableCookieManagement();

        // Trust all certificates
        SSLContext sslContext;
        try {
            sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    return true;
                }
            }).build();
            b.setSslcontext(sslContext);
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException(e);
        }

        // By default, it will verify the hostname in the certificate, which should be localhost
        // and therefore should match. If we start running these tests against a LARS server on
        // a different host then we may need disable hostname verification.

        httpClient = b.build();
    }

    private void setupHttpClient() {
        targetHost = new HttpHost(hostname, portNumber, protocol);

        httpClientContext = HttpClientContext.create();

        /*
         * Create the HTTPClientContext with the appropriate credentials. We'll use this whenever we
         * make an HTTP call.
         */
        if (user != null && password != null) {
            credentials = new UsernamePasswordCredentials(user, password);

            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()), credentials);

            AuthCache authCache = new BasicAuthCache();
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(targetHost, basicAuth);

            httpClientContext.setCredentialsProvider(credsProvider);
            httpClientContext.setAuthCache(authCache);
        }

        // Set up the config to be used for each request by this instance
        requestConfig = RequestConfig.custom()
                .setRedirectsEnabled(followRedirects == Redirects.FOLLOW ? true : false)
                .build();
    }

    public RepositoryContext(String url, String user, String password, Redirects followRedirects) {

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException ex) {
            // test code, hide this exception
            throw new RuntimeException(ex);
        }

        this.protocol = uri.getScheme();
        this.hostname = uri.getHost();
        this.portNumber = uri.getPort();
        this.user = user;
        this.password = password;
        this.followRedirects = followRedirects;

        fullURL = uri.toString();

        setupHttpClient();
    }

    public RepositoryContext(String url, String user, String password) {
        this(url, user, password, Redirects.FOLLOW);
    }

    protected static RepositoryContext createAsAdmin(String url) {
        return new RepositoryContext(url, FatUtils.ADMIN_USERNAME, FatUtils.ADMIN_PASSWORD);
    }

    protected static RepositoryContext createAsAdmin(Protocol protocol) {
        String url = DEFAULT_URLS.get(protocol);
        if (url == null) {
            throw new AssertionError("This should never happen. Didn't find url for " + protocol);
        }
        return createAsAdmin(url);
    }

    protected static RepositoryContext createAsUser(Protocol protocol) {
        String url = DEFAULT_URLS.get(protocol);
        if (url == null) {
            throw new AssertionError("This should never happen. Didn't find url for " + protocol);
        }
        return new RepositoryContext(url, FatUtils.USER_ROLE_USERNAME, FatUtils.USER_ROLE_PASSWORD);
    }

    protected static RepositoryContext toUserContext(RepositoryContext adminContext) {
        return new RepositoryContext(adminContext.fullURL, FatUtils.USER_ROLE_USERNAME, FatUtils.USER_ROLE_PASSWORD);
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    public String getFullURL() {
        return fullURL;
    }

    public String doPost(String url, String content, int expectedStatusCode)
            throws ClientProtocolException, IOException {
        HttpPost post = new HttpPost(fullURL + url);
        return doRequest(post, content, expectedStatusCode);
    }

    public String doPut(String url, String content, int expectedStatusCode)
            throws ClientProtocolException, IOException {
        HttpPut put = new HttpPut(fullURL + url);
        return doRequest(put, content, expectedStatusCode);
    }

    public String doGet(String url, int expectedStatusCode)
            throws ClientProtocolException, IOException {
        HttpGet get = new HttpGet(fullURL + url);
        return doRequest(get, expectedStatusCode);
    }

    public CloseableHttpResponse doRawGet(String url) throws ClientProtocolException, IOException {
        HttpGet get = new HttpGet(fullURL + url);
        get.setConfig(requestConfig);
        return httpClient.execute(targetHost, get, httpClientContext);
    }

    public String doDelete(String url, int expectedStatusCode)
            throws ClientProtocolException, IOException {
        HttpDelete delete = new HttpDelete(fullURL + url);
        return doRequest(delete, expectedStatusCode);
    }

    public CloseableHttpResponse doHead(String url, int expectedStatusCode)
            throws ClientProtocolException, IOException {
        HttpHead head = new HttpHead(fullURL + url);
        head.setConfig(requestConfig);
        CloseableHttpResponse response = httpClient.execute(targetHost, head, httpClientContext);

        assertStatusCode(expectedStatusCode, response);
        return response;
    }

    public String doRequest(HttpEntityEnclosingRequestBase request,
                            String content,
                            int expectedStatusCode)
            throws ClientProtocolException, IOException {
        HttpEntity requestEntity = new ByteArrayEntity(content.getBytes(StandardCharsets.UTF_8),
                ContentType.APPLICATION_JSON);
        request.setEntity(requestEntity);

        return doRequest(request, expectedStatusCode);
    }

    public String doPostMultipart(String url,
                                  String name,
                                  String json,
                                  byte[] content,
                                  ContentType contentType,
                                  int expectedStatusCode)
            throws ClientProtocolException, IOException {
        HttpPost post = new HttpPost(fullURL + url);
        HttpEntity requestEntity = MultipartEntityBuilder.create()
                .addPart("attachmentInfo", new StringBody(json, ContentType.APPLICATION_JSON))
                .addPart(name, new ByteArrayBody(content, contentType, name))
                .build();
        post.setEntity(requestEntity);

        return doRequest(post, expectedStatusCode);
    }

    public String doRequest(HttpRequestBase request, int expectedStatusCode)
            throws ClientProtocolException, IOException {

        request.setConfig(requestConfig);

        try (CloseableHttpResponse response = httpClient.execute(targetHost, request, httpClientContext)) {
            assertStatusCode(expectedStatusCode, response);
            HttpEntity responseEntity = response.getEntity();
            String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
            return responseString;
        }
    }

    public byte[] doGetAsByteArray(String url, int expectedStatusCode)
            throws ClientProtocolException, IOException {
        HttpGet get = new HttpGet(fullURL + url);
        get.setConfig(requestConfig);

        try (CloseableHttpResponse response = httpClient.execute(targetHost, get, httpClientContext)) {
            assertStatusCode(expectedStatusCode, response);
            return EntityUtils.toByteArray(response.getEntity());
        }
    }

    private void assertStatusCode(int expectedStatusCode, HttpResponse response) throws ParseException, IOException {
        StatusLine statusLine = response.getStatusLine();

        int actualStatusCode = statusLine.getStatusCode();

        boolean statusCodeMatches = expectedStatusCode == actualStatusCode
                                    || (expectedStatusCode == RC_REJECT && (actualStatusCode == 403 || actualStatusCode == 401));

        if (!statusCodeMatches) {
            String failMessage = "Unexpected status code: " + actualStatusCode + "; expected: " + expectedStatusCode + "\n";
            failMessage += "Full HTTP response:";
            try {
                failMessage += EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            } catch (UnsupportedEncodingException e) {
                failMessage += "Unable to get full HTTP response!";

            }

            fail(failMessage);
        }
    }

    private void cleanRepo() throws InvalidJsonAssetException, IOException {
        deleteAllAssets();
        AssetList assets = doGetAllAssets();
        assertEquals("Assets remaining in the repository", 0, assets.size());
        assertEquals("Attachments remaining in the repository", 0, FatUtils.getMongoDB().getCollection("attachments").count());
        assertEquals("Files remaining in repository", 0, new GridFS(FatUtils.getMongoDB()).getFileList().size());
    }

    void deleteAllAssets() throws IOException, InvalidJsonAssetException {
        AssetList assets = doGetAllAssets();
        for (Asset asset : assets) {
            deleteAsset(asset.get_id(), -1);
        }
    }

    protected void doGetAllAssetsBad(int expectedRC) throws ClientProtocolException, IOException, InvalidJsonAssetException {
        doGet("/assets", expectedRC);
    }

    protected AssetList doGetAllAssets(int expectedRC) throws ClientProtocolException, IOException, InvalidJsonAssetException {
        String response = doGet("/assets", expectedRC);

        // It is never acceptable for the server to return us a non-valid
        // JSON string in response to this call so we can parse the JSON
        // and return it to the client.
        return AssetList.jsonArrayToAssetList(response);
    }

    protected AssetList doGetAllAssets() throws IOException, InvalidJsonAssetException, ParseException {
        return doGetAllAssets(200);
    }

    /**
     * expectedStatusCode should be -1 to indicate a standard response is expected
     *
     * @param id the id of the attachment to delete or, oddly, if it's null then we want to DELETE
     *            the /assets URL (which should fail).
     * @param expectedStatusCode
     * @throws IOException
     */
    protected String deleteAsset(String id, int expectedStatusCode) throws IOException {
        // 204 for 'deleted, no content returned'
        if (expectedStatusCode == -1) {
            expectedStatusCode = 204;
        }

        String url = "/assets";
        if (id != null) {
            url += "/" + id;
        }

        return doDelete(url, expectedStatusCode);
    }

    boolean repositoryIsEmpty() throws IOException, InvalidJsonAssetException {
        AssetList assets = doGetAllAssets();
        if (assets.size() == 0) {
            return true;
        }
        return false;
    }

    Asset updateAssetNoAttachments(Asset toUpdate, int expectedResponse) throws IOException, InvalidJsonAssetException {
        String assetJson = doPut("/assets/" + toUpdate.get_id(),
                                 toUpdate.toJson(),
                                 expectedResponse);

        if (assetJson != null && !!!assetJson.equals("")) {
            return Asset.deserializeAssetFromJson(assetJson);
        } else {
            return null;
        }
    }

    Asset getAsset(String id) throws IOException, InvalidJsonAssetException {
        String assetJson = doGet("/assets/" + id, 200);
        return Asset.deserializeAssetFromJson(assetJson);
    }

    List<Map<String, Object>> getAssetReviews(String id) throws IOException, InvalidJsonAssetException {
        String resultJson = doGet("/assets/" + id + "/assetreviews", 200);
        return jsonReader.readValue(resultJson, new TypeReference<List<Map<String, Object>>>() {});
    }

    void getAssetReviewsBad(String id, int expectedStatusCode, String expectedMessage) throws IOException {
        String message = doGet("/assets/" + id + "/assetreviews", expectedStatusCode);

        if (expectedMessage != null) {
            String errorMessage = parseErrorObject(message.toString());
            assertEquals("Unexpected message from server", expectedMessage, errorMessage);
        }
    }

    /**
     * Get asset with the expectation that it will fail
     */
    String getBadAsset(String id, int expectedStatusCode) throws IOException {
        return doGet("/assets/" + id, expectedStatusCode);
    }

    AssetList getAllAssets(String filter) throws IOException, InvalidJsonAssetException {
        // escape '|' character used in or-filtering
        filter = filter.replace("|", "%7C");
        String assetsJson = doGet("/assets?" + filter, 200);
        return AssetList.jsonArrayToAssetList(assetsJson);
    }

    List<Map<String, Object>> getAssetSummary(String parameters) throws JsonParseException, JsonMappingException, IOException {
        String resultString = doGet("/assets/summary?" + parameters, 200);
        return jsonReader.readValue(resultString, new TypeReference<List<Map<String, Object>>>() {});
    }

    void getBadAssetSummary(String parameters, int expectedRC) throws IOException {
        doGet("/assets/summary?" + parameters, expectedRC);
    }

    int getAssetCount(String parameters) throws IOException {
        try (CloseableHttpResponse response = doHead("/assets?" + parameters, HttpStatus.SC_NO_CONTENT)) {
            return Integer.parseInt(response.getFirstHeader("count").getValue());
        }
    }

    protected Attachment doPostAttachmentNoContent(String assetId, String name, Attachment attachment) throws ClientProtocolException, IOException, InvalidJsonAssetException {
        List<NameValuePair> qparams = new ArrayList<>();
        qparams.add(new BasicNameValuePair("name", name));
        String url = "/assets/" + assetId + "/attachments?" + URLEncodedUtils.format(qparams, "UTF-8");

        String response = doPost(url, attachment.toJson(), 200);

        Attachment createdAttachment = Attachment.jsonToAttachment(response);

        return createdAttachment;
    }

    protected void doPostBadAttachmentNoContent(String assetId, String name, Attachment attachment, int expectedRC, String expectedMessage) throws ClientProtocolException, IOException, InvalidJsonAssetException {
        List<NameValuePair> qparams = new ArrayList<>();
        qparams.add(new BasicNameValuePair("name", name));
        String url = "/assets/" + assetId + "/attachments?"
                     + URLEncodedUtils.format(qparams, "UTF-8");
        String message = doPost(url, attachment.toJson(), expectedRC);

        if (expectedMessage != null) {
            String errorMessage = parseErrorObject(message.toString());
            assertEquals("Unexpected message from server", expectedMessage, errorMessage);
        }
    }

    protected Attachment doPostAttachmentWithContent(String assetId, String name, Attachment attachment, byte[] content, ContentType contentType) throws ClientProtocolException, IOException, InvalidJsonAssetException {

        List<NameValuePair> qparams = new ArrayList<>();
        qparams.add(new BasicNameValuePair("name", name));
        String url = "/assets/" + assetId + "/attachments?"
                     + URLEncodedUtils.format(qparams, "UTF-8");
        String response = doPostMultipart(url,
                                          name,
                                          attachment.toJson(),
                                          content,
                                          contentType,
                                          200);

        return Attachment.jsonToAttachment(response);
    }

    protected void doPostBadAttachmentWithContent(String assetId,
                                                  String name,
                                                  Attachment attachment,
                                                  byte[] content,
                                                  ContentType contentType,
                                                  int expectedRC,
                                                  String expectedMessage)
            throws ClientProtocolException, IOException, InvalidJsonAssetException {

        List<NameValuePair> qparams = new ArrayList<>();
        qparams.add(new BasicNameValuePair("name", name));
        String url = "/assets/" + assetId + "/attachments?"
                     + URLEncodedUtils.format(qparams, "UTF-8");

        String response = doPostMultipart(url,
                                          name,
                                          attachment.toJson(),
                                          content,
                                          contentType,
                                          expectedRC);

        if (expectedMessage != null) {
            String errorMessage = parseErrorObject(response);
            assertEquals("Unexpected message from server", expectedMessage, errorMessage);
        }
    }

    protected AttachmentList doGetAllAttachmentsForAsset(String assetId) throws ClientProtocolException, IOException, InvalidJsonAssetException {
        String response = doGet("/assets/" + assetId + "/attachments/", 200);
        return AttachmentList.jsonToAttachmentList(response.getBytes(StandardCharsets.UTF_8));
    }

    protected String doGetAllAttachmentsForAssetBad(String assetId, int expectedStatusCode) throws ClientProtocolException, IOException {
        return doGet("/assets/" + assetId + "/attachments/", expectedStatusCode);
    }

    /**
     * If an asset has only one attachment, this method retrieves it, asserts that its id is correct
     * and then returns it.
     *
     * @throws InvalidJsonAssetException
     * @throws IOException
     * @throws ClientProtocolException
     */
    protected Attachment doGetOnlyAttachment(String assetId, String attachmentId) throws ClientProtocolException, IOException, InvalidJsonAssetException {
        AttachmentList retrievedAttachments = doGetAllAttachmentsForAsset(assetId);
        assertEquals("Asset is expected to have just one attachment", 1, retrievedAttachments.size());
        Attachment retrievedAttachment = retrievedAttachments.get(0);

        assertEquals("Incorrect attachment id", attachmentId, retrievedAttachment.get_id());
        assertEquals("Asset id on attachment is expected to match owning asset", assetId, retrievedAttachment.getAssetId());

        return retrievedAttachment;
    }

    /**
     * Designed to be called to try to retrieve an attachment and to expect an error return code to
     * come back from the server (ie only use this method in error cases).
     *
     * @throws IOException
     */
    protected void doGetAttachmentContentInError(String assetId,
                                                 String attachmentId,
                                                 String attachmentName,
                                                 int expectedStatusCode,
                                                 String expectedErrorMessage)
            throws IOException {
        String url = "/assets/" + assetId + "/attachments/" + attachmentId + "/" + attachmentName;
        String response = doGet(url, expectedStatusCode);
        if (expectedErrorMessage != null) {
            assertEquals(expectedErrorMessage, parseErrorObject(response));
        }
    }

    protected byte[] doGetAttachmentContent(String assetId, String attachmentId, String attachmentName, int expectedRC) throws IOException {
        String url = "/assets/" + assetId + "/attachments/" + attachmentId + "/" + attachmentName;
        return doGetAsByteArray(url, expectedRC);
    }

    protected byte[] doGetAttachmentContent(String assetId, String attachmentId, String attachmentName) throws IOException {
        return doGetAttachmentContent(assetId, attachmentId, attachmentName, 200);
    }

    protected void doDeleteAttachment(String assetId, String attachmentId) throws ClientProtocolException, IOException {
        doDeleteAttachment(assetId, attachmentId, 204);
    }

    protected void doDeleteAttachment(String assetId, String attachmentId, int expectedResponseCode) throws ClientProtocolException, IOException {
        doDelete("/assets/" + assetId + "/attachments/" + attachmentId, expectedResponseCode);
    }

    protected void addBadAsset(Asset toAdd, int expectedResponseCode) throws IOException, InvalidJsonAssetException {
        doPost("/assets", toAdd.toJson(), expectedResponseCode);
    }

    protected Asset addAssetNoAttachments(Asset toAdd) throws IOException, InvalidJsonAssetException {
        String assetJson = doPost("/assets", toAdd.toJson(), 200);
        return Asset.deserializeAssetFromJson(assetJson);
    }

    protected Asset addAssetNoAttachmentsWithState(Asset toAdd, Asset.State targetState) throws IOException, InvalidJsonAssetException {
        String assetJson = doPost("/assets", toAdd.toJson(), 200);
        Asset result = Asset.deserializeAssetFromJson(assetJson);
        moveAssetFromDraftToState(result.get_id(), targetState);
        // return an updated version of the asset to ensure the state is correct.
        return getAsset(result.get_id());
    }

    protected void moveAssetFromDraftToState(String id, Asset.State targetState) throws InvalidJsonAssetException, IOException {
        switch (targetState) {
            case DRAFT:
                // nothing to do
                break;
            case AWAITING_APPROVAL:
                updateAssetState(id, Asset.StateAction.PUBLISH, 200);
                break;
            case PUBLISHED:
                updateAssetState(id, Asset.StateAction.PUBLISH, 200);
                updateAssetState(id, Asset.StateAction.APPROVE, 200);
                break;
            case NEED_MORE_INFO:
                updateAssetState(id, Asset.StateAction.PUBLISH, 200);
                updateAssetState(id, Asset.StateAction.NEED_MORE_INFO, 200);
                break;
            default:

        }
    }

    String updateAssetState(String id, Asset.StateAction stateAction, int expectedStatusCode) throws IOException, InvalidJsonAssetException {
        String json = "{\"action\":\"" + stateAction.getValue() + "\"}";

        String response = doPut("/assets/" + id + "/state", json, expectedStatusCode);

        // A state update will get back nothing for a good request, or an error
        // for a bad one
        return response;
    }

    /**
     * Used to test requesting a bad state update. Normally
     * {@link #updateAssetState(String, Asset.StateAction, int)} should be used.
     */
    String updateAssetStateBad(String id, String stateAction, int expectedStatusCode) throws IOException, InvalidJsonAssetException {
        String json = "{\"action\":\"" + stateAction + "\"}";

        String response = doPut("/assets/" + id + "/state", json, expectedStatusCode);

        // A state update will get back nothing for a good request, or an error
        // for a bad one
        return response;
    }

    /**
     * If the supplied errorObject is valid JSON, look for a field called 'error' and return it. If
     * the object is not JSON, just return it (expecting that it is an HTML error message). If valid
     * JSON, but no 'error' field found, returns null.
     *
     * @param errorObject
     * @return
     * @throws ServerException if the errorObject is badly formed JSON (as supposed to not JSON at
     *             all).
     */
    public String parseErrorObject(String errorObject) throws ServerException {
        if (errorObject == null) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<?, ?> jsonMap = null;
        try {
            jsonMap = mapper.readValue(errorObject, Map.class);
        } catch (JsonParseException e) {
            // Assume the string is an html error message.
            return errorObject;
        } catch (JsonMappingException e) {
            // Assume the string is an html error message.
            return errorObject;
        } catch (IOException e) {
            // No idea what would cause this.
            throw new ServerException(e);
        }
        String errorMessage = readJsonStringValue(jsonMap, "message");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            return errorMessage;
        } else {
            return errorObject;
        }
    }

    private static String readJsonStringValue(Map<?, ?> jsonMap, String key) {
        Object value = jsonMap.get(key);
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return (String) value;
        }

        return null;
    }

    /**
     * Thrown when a test fails in such a way that it seems as though the server has fallen over (as
     * supposed to having produced an expected error message). This is likely to translate to a 500
     * error or similar.
     */
    static class ServerException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        ServerException(Throwable cause) {
            super(cause);
        }

        ServerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
