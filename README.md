# LARS

The Liberty Asset Repository Service implements a repository for
Liberty features that you can deploy locally. You can host features
you have developed and features retrieved from the Websphere Liberty
Repository, and then use your Liberty server's installUtility command
to install the hosted features into your server.

## Documentation

The LARS documentation is organized into a number of pages:

 * [Building LARS] (doc/BUILDING.md)
 * [Installing LARS] (doc/INSTALL.md)
 * [Using `larsClient`] (doc/LARSCLIENT.md)

## Getting started

If you don't wish to build LARS from source, follow the [Installing LARS]
(doc/INSTALL.md) instructions to install the latest binary release from the [Liberty Repository] (http://www.wasdev.net/downloads), and perform the necessary `server.xml` customization steps.

Start mongodb

Now start the server

    bin/server start larsServer

The server should now be running. Unless you've changed the default
config, you should be able to visit http://localhost:9080/ma/v1/assets
and see an empty list.

    []

## Adding assets

Unzip `larsClient.zip` and then run `larsClient` to upload an asset.

    bin/larsClient upload --url=http://localhost:9080/ma/v1 --username=admin --password userFeature.esa

Enter your password when prompted. You should see a message saying
that the feature has been uploaded

## Listing assets

    bin/larsClient listAll --url=http://localhost:9080/ma/v1

If you changed the config to require authentication for the user role,
you'll need to provide your username and password as you did when
adding an asset. You should see some output like this:

    Listing all assets in the repository:
    Asset ID                       | Asset Type      | Liberty Version | Asset Name
    54da04d8735612bfdb13bf56       | Feature         |                 | com.ibm.ws.test.userFeature

## Deleting assets

The list command shows the asset id. You can use this to delete the
asset from the repository

    bin/larsClient delete --url=http://localhost:9080/ma/v1 --username=adminUser --password 123456789103456789

