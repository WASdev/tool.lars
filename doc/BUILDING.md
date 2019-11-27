# Building LARS

* Download all the [Prerequisites](PREREQS.md)
* Clone this repo
* Rename `server/gradle.properties.template` to `server/gradle.properties` and edit to point to your MongoDB and Liberty installation directories:
```libertyRoot=/Users/user/Documents/wlp```
```mongodExecutable=/Users/user/Documents/mongod```
* Inside your Liberty installation, from the `bin` directory, run `./installUtility install cdi-1.0 jndi-1.0 appSecurity-2.0 ssl-1.0 mongodb-2.0 jaxrs-1.1 servlet-3.0 passwordUtilities-1.0`
* Look at `test-utils/src/main/resources/config.properties` and check you are happy with the specified test ports
* Launch Gradle to build the code, run the tests and produce the distribution archives: `gradle build dist`
* You should now have two files in the `build/distributions` directory: `larsServer.zip`and `larsClient.zip`

Now `larsServer.zip` can be [installed, configured and started](INSTALL.md#install-lars-into-liberty)