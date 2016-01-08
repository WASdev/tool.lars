# Building LARS

Make sure you have the following prerequisites installed: (older or
newer versions may work but this is what we've tested with)

* [gradle v2.0](http://gradle.org/downloads) 
* [mongoDB v2.6](https://www.mongodb.org/downloads#previous)
* [WAS Liberty Profile 8.5.5.6](https://developer.ibm.com/wasdev/downloads/#asset/runtimes-8.5.5-wlp-runtime).
  * Your Liberty runtime must contain the following features several features, which can be installed using `installUtility` as follows:
    bin/installUtility install cdi-1.0 servlet-3.0 mongodb-2.0 jaxrs-1.1 cdi-1.0 servlet-3.0 mongodb-2.0 jaxrs-1.1

* [Liberty MongoDB Integration 2.0 Feature](https://developer.ibm.com/assets/wasdev/#asset/features-com.ibm.websphere.appserver.mongodb-2.0)
* A Java 7 JDK

Either clone the repository or download and extract a snapshot

Rename `server/gradle.properties.template` to `server/gradle.properties` and edit to point to your mongodb and liberty
installation directories.

Have a look at `test-utils/src/main/resources/config.properties` and
check that you are happy with the specified test ports

Launch gradle to build the code, run the tests and produce the
distribution archives.

    gradle build dist

You should now have three files in the build/distributions directory,
`larsServer.zip`, `larsClient.zip` and `larsServerPackage.jar`.

