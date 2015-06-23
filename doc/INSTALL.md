# Installing and configuring LARS

There are three ways that you can install LARS:

 * Directly from the Liberty Repository using installUtility
 * Using the self-extracting jar installer from the Liberty Repository
 * By building and then unzipping larsServer.zip

Whichever installation method you use, you will also need to configure
LARS.

## Installing LARS directly from the Liberty Repository using `installUtility`

This is the simplest method of installing LARS. From the `wlp`
directory of your Liberty 8.5.5.6 runtime, run the following command:

    bin/installUtility install larsServer

`installUtility` downloads LARS together with the application server
features upon which it depends.

## Using the self-extracting jar installer from the Liberty Repository

Visit [this
page](https://developer.ibm.com/wasdev/downloads/#asset/opensource-Liberty_Asset_Repository_Service)
and click Download to download `larsServerPackage.jar`. Once the
download is complete, run `larsServerPackage.jar`. For example, on the
command line, run the following:

    java -jar larsServerPackage.jar

You must specify the installation directory of an existing Liberty
runtime. If that Libety runtime does not contain any of LARS's
prerequiste features then you can install them using `installUtility`:

    bin/installUtility install cdi-1.0 servlet-3.0 mongodb-2.0 jaxrs-1.1 cdi-1.0 servlet-3.0 mongodb-2.0 jaxrs-1.1

## By building and then unzipping larsServer.zip

If you have built LARS yourself then you can find larsServer.zip in
the `build/distributions` directory. Unzip larsServer.zip into the
`wlp` directory of an existing Liberty runtime.

Note that your Liberty runtime must contain LARS's pre-requising features, which you can install using `installUtility` as above.
    

## Configuring LARS

LARS is a Java EE application running on top of Liberty and can be
configured like any other Liberty application. When LARS is installed,
it creates a new Liberty server called `larsServer`. To configure
LARS, edit the file `wlp/usr/servers/larsServer/server.xml`. For a
basic LARS server, you will need to configure the following:

### User registry

In order to secure LARS (and it is not recommended to run LARS without
security), you need a user registry. The default `server.xml` that is
created when LARS is installed contains a `<basicRegistry>`, commented
out, that can be uncommented and used as a starting point. For more
information on configuring a user registry for Liberty, see [Configuring a user registry for the Liberty profile](http://www-01.ibm.com/support/knowledgecenter/SSAW57_8.5.5/com.ibm.websphere.wlp.nd.multiplatform.doc/ae/twlp_sec_registries.html?cp=SSAW57_8.5.5%2F3-12-1-2-0).

### HTTP endpoint

The `<httpEndpoint>` element determines upon which ports the LARS
server will listen. You can changed these ports to suit your
requirements. In the default configuration, the LARS server only
listens for connections from localhost (ie connections from the same
host that LARS is running on). You can also add a `host=` attribute to
cause LARS to listen for requests from other hosts. For more
information on configuring Liberty's HTTP endpoint properties, see
[Liberty profile: Configuration elements in the server.xml
file](http://www-01.ibm.com/support/knowledgecenter/SSAW57_8.5.5/com.ibm.websphere.wlp.nd.multiplatform.doc/autodita/rwlp_metatype_4ic.html?cp=SSAW57_8.5.5%2F3-0-2-1-0).

### User to role mappings

The default `server.xml` configuration contains a commented-out
`<application-bnd>` element. You can uncomment this and then customize
it to your requirements. For more information on configuring
authorization for applications on Liberty, see [Configuring authorization for applications on the Liberty profile](http://www-01.ibm.com/support/knowledgecenter/SSAW57_8.5.5/com.ibm.websphere.wlp.nd.multiplatform.doc/ae/twlp_sec_rolebased.html?cp=SSAW57_8.5.5%2F3-12-1-3-0).

### MongoDB configuration

If your MongoDB instance uses authentication or if other parameters, such as the MongoDB port, are non-default then you may need to customize the `<mongo>` and `<mongoDB>` elements in server.xml. For more information on configuring Liberty's MongoDB feature, see [Creating Liberty applications that use MongoDB] (http://www-01.ibm.com/support/knowledgecenter/SSAW57_8.5.5/com.ibm.websphere.wlp.nd.multiplatform.doc/ae/twlp_mongodb_create.html?cp=SSAW57_8.5.5%2F3-8-1-2-17-0-1).
