# LARS

The Liberty Asset Repository Service implements a repository for Liberty features that you can deploy locally. You can host features you have developed and features retrieved from the Websphere Liberty Repository and then use the new assetManager command to install them into your Liberty servers.

LARS is currently pre-release. Although it has been tested as a repository for a Liberty server, we recommend it is currently used for evaluation only. 

# Building

Make sure you have the following prerequisites installed: (older or newer versions may work but this is what we've tested with)

* [gradle v2.0](http://gradle.org/downloads) 
* [mongoDB v2.6](https://www.mongodb.org/downloads)
* [WAS Liberty Profile 8.5.5.4](https://developer.ibm.com/wasdev/downloads/#asset/runtimes-8.5.5-wlp-developers-runtime)
* [MongoDB Integration 2.0 Feature](https://developer.ibm.com/assets/wasdev/#asset/features-com.ibm.websphere.appserver.mongodb-2.0)
* A Java 7 JDK

Either clone the repository or download and extract a snapshot

Edit `server/gradle.properties` to point to your mongodb and liberty installation directories.

Launch gradle to build the code, run the tests and produce the distribution archives.

    gradle build dist

You should now have two files in the build/distributions directory, `larsServer.zip` and `larsClient.zip`

# Installing

Unzip larsServer.zip into the install directory of your liberty server. (The one which contains the `usr` directory)

Open `usr/servers/larsServer/server.xml` and follow the directions in the comments to create users and assign them to User and Administrator roles in the larsServer application

If your mongodb server is not running on the same machine as your liberty server or uses authentication, you must also edit the mongodb configuration in the server.xml

Start mongodb

Now start the server

    bin/server start larsServer

The server should now be running. Unless you've changed the default config, you should be able to visit http://localhost:9080/ma/v1/assets and see an empty list.

    []

# Adding assets

Unzip `larsClient.zip` and then run `larsClient` to upload an asset.

    bin/larsClient upload --url=http://localhost:9080/ma/v1 --username=admin --password userFeature.esa

Enter your password when prompted. You should see a message saying that the feature has been uploaded

# Listing assets

    bin/larsClient listAll --url=http://localhost:9080/ma/v1

If you changed the config to require authentication for the user role, you'll need to provide your username and password as you did when adding an asset. You should see some output like this:

    Listing all assets in the repository:
    Asset ID                       | Asset Type      | Liberty Version | Asset Name
    54da04d8735612bfdb13bf56       | Feature         |                 | com.ibm.ws.test.userFeature

# Deleting assets

The list command shows the asset id. You can use this to delete the asset from the repository

    bin/larsClient delete --url=http://localhost:9080/ma/v1 --username=adminUser --password 123456789103456789
