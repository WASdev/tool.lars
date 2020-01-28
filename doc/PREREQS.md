## In order to run LARS you need the following prerequisites

* [MongoDB server](https://www.mongodb.com/download-center/community) compatible with [Java Driver v3.12](https://docs.mongodb.com/ecosystem/drivers/driver-compatibility-reference/#java-driver-compatibility)
* [WAS Liberty Profile 19.0.0.9](https://developer.ibm.com/wasdev/downloads/#asset/runtimes-wlp-kernel) or newer
* A Java 8 JDK

## To build LARS you will additionally require

* [Gradle v4.10.2](https://gradle.org/releases/)
* Your Liberty runtime must contain the following features which can be installed using `installUtility` as follows: `bin/installUtility install cdi-1.0 servlet-3.0 passwordUtilities-1.0 jaxrs-1.1`
