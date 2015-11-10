/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.repository.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Authenticator;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.connections.RestRepositoryConnectionProxy;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.ProductResourceImpl;
import com.ibm.ws.repository.strategies.writeable.AddNewStrategy;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;

/**
 * Junit tests(s) for proxy support.
 *
 * These tests need to run in order as once a proxy session is established it does not query
 * the Authenticator for new credentials as there is already a session i.e. if you pass in correct
 * credentials then incorrect ones the second test case will succeed.
 *
 * To work round this there is one Junit test below that calls all the tests as just normal method
 * calls rather than Junit tests.
 */
// TODO Following annotation is not available until we go to Junit 4.11.  See comment in testAllProxyTests().
// When we do this test can be restructured to use multiple Junit tests rather than running under one.
// @FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Ignore
// TODO: make this test work!
public class ProxyTest {

    final static String PROXY_AUTH_PORT = "3128"; // squid listens on this port for an authenticated proxy
    final static String PROXY_NOAUTH_PORT = "3129"; // squid listens on this port for an unauthenticated proxy
    static boolean _verbose = false;
    RestRepositoryConnection _proxyLogon = null; // copy of the LoginInfoEntry

    RestRepositoryConnectionProxy proxyAuth = null; // proxy object to represent the authenticated proxy
    RestRepositoryConnectionProxy proxyNoAuth = null; // proxy object to represent the unauthenticated proxy
    private static final Logger logger = Logger.getLogger(ProxyTest.class.getName());

    private final RestRepositoryConnection _restConnection = null;

    /**
     * Constructor
     */
    public ProxyTest() throws FileNotFoundException, IOException {}

    /**
     * Before class only used at the moment to turn verbose output on or off
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setupProxy() throws Exception {
        ProxyTest.setVerbose(true);
    }

    /**
     * Create the proxy objects co-located on the massive server we are running on. Do this in a before
     * rather than a before class to cope with the fail-over code.
     *
     * @throws MalformedURLException
     * @throws RepositoryException
     */
    @Before
    public void before() throws MalformedURLException, RepositoryException {
        URL url = new URL(_restConnection.getRepositoryUrl());
        proxyAuth = new RestRepositoryConnectionProxy(new URL(url.getProtocol() + "://" + url.getHost() + ":" + PROXY_AUTH_PORT));
        proxyNoAuth = new RestRepositoryConnectionProxy(new URL(url.getProtocol() + "://" + url.getHost() + ":" + PROXY_NOAUTH_PORT));

        _proxyLogon = new RestRepositoryConnection(_restConnection.getUserId(), _restConnection.getPassword(), _restConnection.getApiKey(), _restConnection.getRepositoryUrl(), _restConnection.getSoftlayerUserId(), _restConnection.getSoftlayerPassword(), _restConnection.getAttachmentBasicAuthUserId(), _restConnection.getAttachmentBasicAuthPassword());
        _proxyLogon.setUserAgent(_restConnection.getUserAgent());
        RestRepositoryConnectionProxy oldProxy = _proxyLogon.getProxy();
        if (oldProxy != null) {
            _proxyLogon.setProxy(new RestRepositoryConnectionProxy(oldProxy.getProxyURL()));
        }
        loadResources();

        if (_verbose) {
            logger.log(Level.INFO, "@Before Running on " + _proxyLogon.getRepositoryUrl());
            logger.log(Level.INFO, "@Before ProxyAuth set to " + proxyAuth.getProxyURL());
            logger.log(Level.INFO, "@Before ProxyNoAuth set to " + proxyNoAuth.getProxyURL());
        }
    }

    /**
     * This test case will call individual tests to ensure the order that they run in. When we successfully
     * create a connection to a proxy the session is kept alive and further attempts will not have their
     * credentials checked. Because of this we have to do failing tests before successful ones.
     *
     * @throws RepositoryBackendException
     * @throws MalformedURLException
     * @throws ProxyTestException
     *
     * @throws Exception
     */
    @Test
    public void testAllProxyTests() throws MalformedURLException, RepositoryBackendException, ProxyTestException {
        testGetAllWithoutProxy();
        testUnauthenticatedProxy();
        testAuthenticatedProxyInvalid();
        testAuthenticatedProxyValid();
    }

    /**
     * Just test the data is there and can be read it ... a sanity test
     *
     * @throws RepositoryBackendException
     * @throws MalformedURLException
     *
     * @throws Exception
     */
    private void testGetAllWithoutProxy() throws MalformedURLException, RepositoryBackendException {
        logger.log(Level.INFO, "\n--testGetAllWithoutProxy()");
        int results = getAll(null); // no proxy
        assertEquals("Unexpected number of assets found after adding assets",
                     3, results);
    }

    /**
     * Test against an unauthenticated proxy
     *
     * @throws MalformedURLException
     * @throws RepositoryBackendException
     * @throws RepositoryTestException
     *
     * @throws Exception
     */
    private void testUnauthenticatedProxy() throws MalformedURLException, RepositoryBackendException, ProxyTestException {
        logger.log(Level.INFO, "\n--testUnauthenticatedProxy()");
        setAuthenticator("proxyuser1", "AUTHENTICATOR_SHOULDNT_BE_CALLED_AS_UNAUTHENTICATED");
        int results = 0;
        try {
            results = getAll(proxyNoAuth); // unauthenticated proxy
        } catch (RepositoryBackendException rbe) {
            Throwable cause = rbe.getCause();
            if (cause instanceof ConnectException) {
                throw new ProxyTestException(
                                "ConnectionException: check the squid service is started on " + proxyNoAuth.getProxyURL(),
                                rbe);
            } else {
                // unknown cause
                throw rbe;
            }
        }
        assertEquals("Unexpected number of assets found through an unauthenticated proxy",
                     3, results);
    }

    /**
     * Test against an authenticated proxy with invalid password
     *
     * @throws MalformedURLException
     * @throws RepositoryBackendException
     * @throws ProxyTestException
     */
    private void testAuthenticatedProxyInvalid() throws MalformedURLException, RepositoryBackendException, ProxyTestException {
        logger.log(Level.INFO, "\n--testAuthenticatedProxyInvalid()");
        setAuthenticator("proxyuser1", "INVALID_PASSWORD");
        try {
            getAll(proxyAuth); // authenticated proxy
        } catch (RepositoryBackendException rbe) {
            Throwable cause = rbe.getCause();
            if (cause == null) {
                // there was no wrapped exception .. throw original
                throw rbe;
            } else if (cause instanceof RequestFailureException) {
                // A RequestFailureException might be an HTTP 407
                String message = cause.getMessage();
                if (_verbose) {
                    logger.log(Level.INFO, "Message was: " + message);
                }
                if (message.contains("Server returned HTTP response code: 407")) {
                    // expected HTTP 407
                    logger.log(Level.INFO, "Proxy returned HTTP 407 for invalid credentials as expected");
                    return;
                } else {
                    // not 407 so throw the whole exception
                    throw rbe;
                }

            } else if (cause instanceof ConnectException) {
                // A ConnectException could indicate a proxy error
                throw new ProxyTestException(
                                "ConnectionException: check the squid service is started on " + proxyAuth.getProxyURL(),
                                rbe);

            } else {
                // unknown cause
                throw rbe;
            }
        }

        fail("we should have received an HTTP 407 in a RequestFailureException");
    }

    /**
     * Test against an authenticated proxy with valid userid and pasword
     *
     * @throws MalformedURLException
     * @throws ProxyTestException
     * @throws RepositoryBackendException
     */
    private void testAuthenticatedProxyValid() throws MalformedURLException, ProxyTestException, RepositoryBackendException {
        logger.log(Level.INFO, "\n--testAuthenticatedProxyValid()");
        setAuthenticator("proxyuser1", "proxypassword1");

        // the following code will never be exercised until this test runs in its own Junit test
        // as it will fail in the preceding testAuthenticatedProxyInvalid()
        int results = 0;
        try {
            results = getAll(proxyAuth); // authenticated proxy
        } catch (RepositoryBackendException rbe) {
            Throwable cause = rbe.getCause();
            if (cause instanceof ConnectException) {
                throw new ProxyTestException("ConnectionException: check the squid service is started on " + proxyAuth.getProxyURL(),
                                rbe);
            } else {
                // unknown cause
                throw rbe;
            }
        }

        assertEquals("Unexpected number of assets found through an authenticated proxy",
                     3, results);
    }

    /**
     * Get all the resources in the repository using the provided proxy.
     *
     * @param proxy - the proxy or null if we are not using a proxy
     * @return int - the number of records returned
     * @throws MalformedURLException
     * @throws RepositoryBackendException
     */
    private int getAll(RestRepositoryConnectionProxy proxy) throws MalformedURLException, RepositoryBackendException {

        _proxyLogon.setProxy(proxy); // set the passed proxy (null, auth or noauth)
        RepositoryConnectionList loginInfo = new RepositoryConnectionList(_proxyLogon);

        Collection<? extends RepositoryResource> resources = loginInfo.getAllResources();

        logger.log(Level.INFO, "Read " + resources.size() + " resources");
        if (_verbose) {
            for (RepositoryResource res : resources) {
                logger.log(Level.INFO, "Resource=" + res.getName() + ", id=" + res.getId());
            }
        }
        return resources.size();
    }

    /**
     * Return the provided proxy userid and password on a call to the system Authenticator
     *
     * @param pUser
     * @param pPwd
     */
    private void setAuthenticator(final String pUser, final String pPwd) {
        if (_verbose) {
            logger.log(Level.INFO, "Authenticator set to return user=" + pUser + ", pwd=" + pPwd);
        }

        Authenticator.setDefault(new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                if (_verbose) {
                    logger.log(Level.INFO, "AUTHENTICATOR called for type=" + getRequestorType() + ", host=" + getRequestingHost() + ", port=" + getRequestingPort());
                }
                if (getRequestorType() == RequestorType.PROXY) {
                    return new PasswordAuthentication(pUser, pPwd.toCharArray());
                }
                return null;
            }
        });
    }

    /**
     * Populate the repository using the LoginInfoEntry passed in from the framework
     *
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     */
    private void loadResources() throws RepositoryBackendException, RepositoryResourceException {
        logger.log(Level.INFO, "Loading repository data");

        ProductResourceImpl pr = new ProductResourceImpl(_restConnection);
        pr.setName("Alpha");
        pr.uploadToMassive(new AddNewStrategy());

        pr = new ProductResourceImpl(_restConnection);
        pr.setName("Beta");
        pr.uploadToMassive(new AddNewStrategy());

        pr = new ProductResourceImpl(_restConnection);
        pr.setName("Gamma");
        pr.uploadToMassive(new AddNewStrategy());

        RepositoryConnectionList loginInfo = new RepositoryConnectionList(_restConnection);
        Collection<? extends RepositoryResource> resources = loginInfo.getAllResources();
        if (_verbose) {
            logger.log(Level.INFO, "Added " + resources.size() + " resources");
        }
    }

    /**
     * Set the verbose output flag
     *
     * @param verbose - true or false
     * @return the value set
     */
    private static boolean setVerbose(boolean verbose) {
        _verbose = verbose;
        return verbose;
    }

    /**
     * Inner class to wrap exception and return information on proxy connection errors
     */
    private class ProxyTestException extends Exception {

        private static final long serialVersionUID = 1L;

        protected ProxyTestException(String message, Throwable cause) {
            super(message, cause);
        }

        @Override
        public Throwable getCause() {
            return super.getCause();
        }

    }
}
