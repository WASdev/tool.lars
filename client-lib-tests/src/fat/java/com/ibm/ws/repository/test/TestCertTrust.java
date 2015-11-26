package com.ibm.ws.repository.test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class TestCertTrust {

    public static void trustAll() {
        HostnameVerifier trustAll = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(trustAll);
    }

    public static void disableSNIExtension() {
        System.setProperty("jsse.enableSNIExtension", "false");
    }
}
