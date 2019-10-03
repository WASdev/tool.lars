# LARS

The Liberty Asset Repository Service implements a repository for
Liberty features that you can deploy locally. You can host features
you have developed and features retrieved from the Websphere Liberty
Repository, and then use your Liberty server's installUtility command
to install the hosted features into your server.

For documentation on how to use LARS with Eclipse and how to use LARS as an on-site mirror for WebSphere Liberty features please check out the [Wiki](https://github.com/WASdev/tool.lars/wiki).

## Prerequisites

* [Gradle v4.10.2](https://gradle.org/releases/)
* [MongoDB v3.11+](https://www.mongodb.com/download-center/community)
* [WAS Liberty Profile 19.0.0.9](https://developer.ibm.com/wasdev/downloads/#asset/runtimes-wlp-kernel)
* Your Liberty runtime must contain the following features which can be installed using `installUtility` as follows: `bin/installUtility install cdi-1.0 servlet-3.0 passwordUtilities-1.0 jaxrs-1.1`
* A Java JDK (we tested with Java 8)

## Downloading LARS

Either download `larsServer.zip` from the [Liberty Repository](https://developer.ibm.com/wasdev/downloads/#asset/tools-Liberty_Asset_Repository_Service) or build `larsServer.zip` yourself by doing the following:

* Clone this repo
* Rename `server/gradle.properties.template` to `server/gradle.properties` and edit to point to your MongoDB and Liberty installation directories
* Look at `test-utils/src/main/resources/config.properties` and check you are happy with the specified test ports
* Launch Gradle to build the code, run the tests and produce the distribution archives: `gradle build dist`
* You should now have three files in the `build/distributions` directory: `larsServer.zip`, `larsClient.zip` and `bluemixServer.zip`


## Installing and configuring LARS

LARS is a Java EE application running on top of Liberty and can be
configured like any other Liberty application. When LARS is installed,
it creates a new Liberty server called `larsServer`.

Unzip `larsServer.zip/larsServer/servers/` into the `WLP_USER_DIR` (usually `wlp/usr/servers`) directory of an existing Liberty 19.0.0.9 (or newer) runtime.

Install LARS's pre-requisite features, using
`installUtility` after extracting the zip:
`bin/installUtility install larsServer`

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
server will listen. You can change these ports to suit your
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

## Starting the server

* Start mongodb
* `bin/server start larsServer`

The server should now be running. Unless you've changed the default
config, you should be able to visit http://localhost:9080/ma/v1/assets
and see an empty list.

    []

## Using larsClient

`larsClient` allows you to add assets to LARS, to list assets and to delete assets. Like Liberty itself, `larsClient` requires Java. For full details of command line parameters, run `bin/larsClient help`.

### Adding assets

Unzip `larsClient.zip` and then run `larsClient` to upload an asset. You can add an asset by specifying the file containing that asset:

`bin/larsClient upload --url=http://localhost:9080/ma/v1 --username=admin --password userFeature.esa`

Enter your password when prompted. You should see a message saying that the feature has been uploaded. You can also add multiple assets by specifying the name of a directory that directly contains those assets. Note that larsClient does not search subdirectories for assets when you do this.

`bin/larsClient upload --url=http://localhost:9080/ma/v1 --username=admin --password /home/liberty/javaee7Bundle/features/8.5.5.6/`

### Listing assets

You can list all assets in LARS as follows:

    bin/larsClient listAll --url=http://localhost:9080/ma/v1

If you changed the config to require authentication for the user role,
you'll need to provide your username and password as you did when
adding an asset. You should see some output like this:

    Listing all assets in the repository:
    Asset ID                       | Asset Type      | Liberty Version | Asset Name
    54da04d8735612bfdb13bf56       | Feature         |                 | com.ibm.ws.test.userFeature

### Deleting assets

You can delete an asset from LARS using the delete command by specifying the ID of the asset. The list command shows the asset ID:

    bin/larsClient delete --url=http://localhost:9080/ma/v1 --username=adminUser --password 123456789103456789

