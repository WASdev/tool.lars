# Using larsClient

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
