# Building LARS

Make sure you have the following prerequisites installed: (older or
newer versions may work but this is what we've tested with)

* [gradle v5.6.2](http://gradle.org/downloads) 
* [mongoDB v3.11+](https://www.mongodb.com/download-center/community)
* [WAS Liberty Profile 19.0.0.9](https://developer.ibm.com/wasdev/downloads/#asset/runtimes-wlp-kernel)
  * Your Liberty runtime must contain the following features several features, which can be installed using `installUtility` as follows: `bin/installUtility install cdi-1.0 servlet-3.0 passwordUtilities-1.0 jaxrs-1.1`
* A Java JDK (we tested with Java 8)

Either clone the repository or download and extract a snapshot

Rename `server/gradle.properties.template` to `server/gradle.properties` and edit to point to your mongodb and liberty
installation directories.

Have a look at `test-utils/src/main/resources/config.properties` and
check that you are happy with the specified test ports

Launch gradle to build the code, run the tests and produce the
distribution archives.

    gradle build dist

You should now have three files in the build/distributions directory,
`larsServer.zip`, `larsClient.zip` and `bluemixServer.zip`
