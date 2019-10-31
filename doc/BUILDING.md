# Downloading and building LARS

Ensure you have the following prerequisites:
* [Gradle v4.10.2](https://gradle.org/releases/)
* [MongoDB server](https://www.mongodb.com/download-center/community) compatible with [Java Driver v3.11](https://docs.mongodb.com/ecosystem/drivers/driver-compatibility-reference/#java-driver-compatibility)
* [WAS Liberty Profile 19.0.0.9](https://developer.ibm.com/wasdev/downloads/#asset/runtimes-wlp-kernel)
* Your Liberty runtime must contain the following features which can be installed using `installUtility` as follows: `bin/installUtility install cdi-1.0 servlet-3.0 passwordUtilities-1.0 jaxrs-1.1`
* A Java 8 JDK

Either download `larsServer.zip` from the [Liberty Repository](https://developer.ibm.com/wasdev/downloads/#asset/tools-Liberty_Asset_Repository_Service) and configure `server/gradle.properties` as mentioned in the second step below or build `larsServer.zip` yourself by doing the following:

* Clone this repo
* Rename `server/gradle.properties.template` to `server/gradle.properties` and edit to point to your MongoDB and Liberty installation directories:
```libertyRoot=/Users/user/Documents/wlp```
```mongodExecutable=/Users/user/Documents/mongod```
* Inside your Liberty installation, from the `bin` directory, run `./installUtility install cdi-1.0 jndi-1.0 appSecurity-2.0 ssl-1.0 mongodb-2.0 jaxrs-1.1 servlet-3.0 passwordUtilities-1.0`
* Look at `test-utils/src/main/resources/config.properties` and check you are happy with the specified test ports
* Launch Gradle to build the code, run the tests and produce the distribution archives: `gradle build dist`
* You should now have three files in the `build/distributions` directory: `larsServer.zip`, `larsClient.zip` and `bluemixServer.zip`
