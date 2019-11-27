/*******************************************************************************
* Copyright (c) 2018 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/
package com.ibm.ws.lars.rest.mongo;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;

import com.ibm.websphere.crypto.PasswordUtil;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.DB;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;
import java.util.Iterator;
import java.util.Collections;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;


@ApplicationScoped
public class MongoProducer {
    private static final Logger logger = Logger.getLogger(MongoProducer.class.getCanonicalName());

    private String dbName = null;
    private String authDbName = null;
    private String user = null;
    private String encodedPass = null;
    private String requestedWriteConcern = null;
    private boolean sslEnabled = false;
    private String sslConfig = null;
    private ArrayList<ServerAddress> servers = new ArrayList<ServerAddress>(2);

    @PostConstruct
    private void readConfig() {
        Properties sysprops = System.getProperties();

        // database name
        dbName = sysprops.getProperty("lars.mongo.dbname", "larsDB");

        // user and password (optional - if not set, use unauthenticated access)
        user = sysprops.getProperty("lars.mongo.user");
        encodedPass = sysprops.getProperty("lars.mongo.pass.encoded");
        authDbName = sysprops.getProperty("lars.mongo.authdb");
        if(authDbName == null) {
            authDbName = dbName;
        }

        // writeConcern (optional - if not set use the default "ACKNOWLEDGED")
        requestedWriteConcern = sysprops.getProperty("lars.mongo.writeConcern");

        // sslEnabled (optional - if not set, assume false)
        if("true".equalsIgnoreCase(sysprops.getProperty("lars.mongo.sslEnabled","false"))) {
            sslEnabled = true;
        }

        // sslConfig (optional, only used if ssl is enabled)
        sslConfig = sysprops.getProperty("lars.mongo.sslConfig");

        // look for all lars.mongo.hostname* properties, in alphabetical order
        Enumeration keysEnum = sysprops.keys();
        Vector<String> keyList = new Vector<String>();
        while(keysEnum.hasMoreElements()){
            keyList.add((String)keysEnum.nextElement());
        }
        Collections.sort(keyList);
        Iterator<String> iterator = keyList.iterator();
        while (iterator.hasNext()) {
            String prop = iterator.next();
            if(prop.startsWith("lars.mongo.hostname")) {
                String hostname = sysprops.getProperty(prop,"localhost");
                int port = Integer.parseInt(sysprops.getProperty(prop.replace("hostname","port"),"27017"));
                ServerAddress sa = new ServerAddress(hostname, port);
                servers.add(sa);
                logger.info("createMongo: found mongodb server setting "+hostname+":"+port+" from property "+prop);
            }
        }

        // add default server if none defined
        if(servers.isEmpty()) {
            ServerAddress sa = new ServerAddress("localhost", 27017);
            servers.add(sa);
            logger.info("createMongo: no mongodb servers specified, defaulting to localhost:27017");
        }


    }

    @Produces
    public MongoClient createMongo() {
        MongoClientOptions.Builder builder = MongoClientOptions.builder();

        // set the WriteConcern, if specified
        if(requestedWriteConcern != null) {
            WriteConcern wc;
            switch(requestedWriteConcern)
            {
                case "ACKNOWLEDGED":
                    wc = WriteConcern.ACKNOWLEDGED;
                    break;
                case "FSYNC_SAFE":
                    wc = WriteConcern.ACKNOWLEDGED;
                    break;
                case "FSYNCED":
                    wc = WriteConcern.ACKNOWLEDGED;
                    break;
                case "JOURNAL_SAFE":
                    wc = WriteConcern.JOURNAL_SAFE;
                    break;
                case "JOURNALED":
                    wc = WriteConcern.JOURNALED;
                    break;
                case "MAJORITY":
                    wc = WriteConcern.MAJORITY;
                    break;
                case "NORMAL":
                    wc = WriteConcern.NORMAL;
                    break;
                case "REPLICA_ACKNOWLEDGED":
                    wc = WriteConcern.REPLICA_ACKNOWLEDGED;
                    break;
                case "REPLICAS_SAFE":
                    wc = WriteConcern.REPLICAS_SAFE;
                    break;
                case "SAFE":
                    wc = WriteConcern.SAFE;
                    break;
                case "UNACKNOWLEDGED":
                    wc = WriteConcern.UNACKNOWLEDGED;
                    break;
                default:
                    wc = WriteConcern.ACKNOWLEDGED;
                    logger.warning("No WriteConcern named " + requestedWriteConcern + " found. Using default WriteConcern of ACKNOWLEDGED.");
            }
            builder = builder.writeConcern(wc);
            logger.info("createMongo: using write concern " + requestedWriteConcern);
        } else {
            logger.info("createMongo: using default write concern");
        }

        // Configure SSL
        if(sslEnabled) {
            try {
                SSLContext sslContext;
                if(sslConfig == null) {
                    sslContext = SSLContext.getDefault();
                } else {
                    sslContext = com.ibm.websphere.ssl.JSSEHelper.getInstance().getSSLContext(sslConfig, Collections.emptyMap(), null);
                }
                logger.info("createMongo: SSL enabled");
                builder = builder.sslEnabled(sslEnabled).sslContext(sslContext);
            } catch(com.ibm.websphere.ssl.SSLException ex) {
                logger.severe("createMongo: Failed to initialize SSL: "+ex.getMessage());
                return null;
            } catch(java.security.NoSuchAlgorithmException ex) {
                logger.severe("createMongo: Failed to initialize SSL: "+ex.getMessage());
                return null;
            }
        }
        MongoClientOptions opts = builder.build();

        // Configure credentials, and connect
        if(encodedPass == null) {            
            logger.info("createMongo: connecting using unauthenticated access");
            return new MongoClient(servers, opts);
        } else {
            String password = PasswordUtil.passwordDecode(encodedPass);
            MongoCredential creds = MongoCredential.createCredential(user, authDbName, password.toCharArray());
            logger.info("createMongo: connecting using user "+user+" and authentication database "+authDbName);
            return new MongoClient(servers, creds, opts);
        }
    }

    @Produces
    public DB createDB(MongoClient client) {
        logger.info("createMongo: connecting to database "+dbName);
        return client.getDB(dbName);
    }

    public void close(@Disposes MongoClient toClose) {
        toClose.close();
    }
}
