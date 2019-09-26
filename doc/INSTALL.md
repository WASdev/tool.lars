# Downloading LARS

There are two ways that you can download LARS:

 * Downloading larsServer.zip from the Liberty Repository
 * By building larsServer.zip yourself

Whichever download method you use, you will also need to configure
LARS.

## Downloading from the Liberty Repository

Visit [this
page](https://developer.ibm.com/wasdev/downloads/#asset/tools-Liberty_Asset_Repository_Service)
and click Download to download `larsServer.zip`. 

## By building and then unzipping larsServer.zip

If you have built LARS yourself then you can find larsServer.zip in
the `build/distributions` directory.

# Installing and configuring LARS

Unzip larsServer.zip into the `WLP_USER_DIR` (usually `wlp/usr/`) directory of an existing Liberty 19.0.0.6 (or newer) runtime.

Install LARS's pre-requisite features, using
`installUtility` after extracting the zip:
`bin/installUtility install larsServer`

## Configuring LARS

LARS is a Java EE application running on top of Liberty and can be
configured like any other Liberty application. When LARS is installed,
it creates a new Liberty server called `larsServer`.

To configure LARS, edit the files `wlp/usr/servers/larsServer/server.xml` and `wlp/usr/servers/larsServer/bootstrap.properties`.

For a basic LARS server, you will need to configure the following:

### User registry

In order to secure LARS (and it is not recommended to run LARS without
security), you need a user registry. The default `server.xml` that is
created when LARS is installed contains a `<basicRegistry>`, commented
out, that can be uncommented and used as a starting point. For more
information on configuring a user registry for Liberty, see [Configuring a user registry in Liberty](https://www.ibm.com/support/knowledgecenter/SSEQTP_liberty/com.ibm.websphere.wlp.doc/ae/twlp_sec_registries.html)

### HTTP endpoint

The `<httpEndpoint>` element determines upon which ports the LARS
server will listen. You can changed these ports to suit your
requirements. In the default configuration, the LARS server only
listens for connections from localhost (ie connections from the same
host that LARS is running on). You can also add a `host=` attribute to
cause LARS to listen for requests from other hosts. For more
information on configuring Liberty's HTTP endpoint properties, see
[HTTP Endpoint](https://www.ibm.com/support/knowledgecenter/SSEQTP_liberty/com.ibm.websphere.liberty.autogen.base.doc/ae/rwlp_config_httpEndpoint.html)

### User to role mappings

The default `server.xml` configuration contains a commented-out
`<application-bnd>` element. You can uncomment this and then customize
it to your requirements. For more information on configuring
authorization for applications on Liberty, see [Configuring authorization for applications in Liberty](https://www.ibm.com/support/knowledgecenter/SSEQTP_liberty/com.ibm.websphere.wlp.doc/ae/twlp_sec_rolebased.html).

### MongoDB configuration

If your MongoDB instance uses authentication or if other parameters, such as the MongoDB port, 
are non-default then you may need to customize the properties in `bootstrap.properties`.
