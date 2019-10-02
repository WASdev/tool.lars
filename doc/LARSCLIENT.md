# Using larsClient

`larsClient` allows you to add assets to LARS, to list assets and to
delete assets. Like Liberty itself, `larsClient` requires Java.

For full details of command line parameters, run

    bin/larsClient help

and

    bin/larsClient help <command-name>

## Adding assets

You can add one asset by specifying the file containing that asset:

    bin/larsClient upload --url=http://localhost:9080/ma/v1 --username=admin --password userFeature.esa

You can also add multiple assets by specifying the name of a directory
that directly contains those assets. Note that larsClient does not
search subdirectories for assets when you do this.

    bin/larsClient upload --url=http://localhost:9080/ma/v1 --username=admin --password /home/liberty/javaee7Bundle/features/8.5.5.6/

## Listing assets

You can list all assets in LARS as follows:

    bin/larsClient listAll --url=http://localhost:9080/ma/v1

## Deleting assets

You can delete an asset from LARS using the delete command by specifying the id of the asset. For example:

    bin/larsClient delete --url=http://localhost:9080/ma/v1 --username=adminUser --password 123456789103456789


