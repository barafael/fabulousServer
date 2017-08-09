# Innovationslabor Passau

Server program for viewing and controlling the lab status

## Prerequisites

* Gradle (v3.4.1) will pull all compile and runtime dependencies as defined in fabulousServer/WebServer/build.gradle.
* java (1.8.0 or greater)
* OracleJRE (if running on Raspberry Pi).
* MySQL (v14.14 or greater)

## Compilation and Build Targets

Main build target in `fabulousServer/WebServer/build.gradle` is `fatJar`. This will build a `.jar` file including all dependencies.

### Environment Variables<a name="envVar"></a>

`export FHEMMOCKDIR=/tmp` should be in your `.bashrc` or equivalent. Adjust the path to a suitable location.
The scripts in [Pulling and Pushing Content to the Host](#pullPush) will use this variable too.

## MySQL setup

`createMySQL` contains all SQL commands to set up the needed database structure.
It also contains a set of dummy data. 

## Network and Server Setup

`Main.java` contains the default HOST and PORT configuration for the server.
The server unit tests emulate an client. The HOST and PORT variables have to be the same here.

## Pulling and Pushing Content to the Host<a name="pullPush"></a>

Multiple scripts are useful to either obtain data from a running system or push data to it:

* `pull.sh` will pull filelogs and jsonList2 to the local system, permitting you to use the data for local unit tests and mocking.
* `jull.sh` will only pull jsonList2 which will be sufficient for most tests but also much faster.
* `push.sh` will compile the project as a fatJat and push it to the defined host.
* `rush.sh` will push the rules (`rules.json`) to the host. You might have to restart the server afterwards if there was a file read/write conflict.
* `eush.sh` will push the events (`events.json`) to the host. You might have to restart the server afterwards if there was a file read/write conflict.

Most scripts will only work if port forwarding to the actual host system is configured. You might have to tweak hostnames and paths.
You will have to set the environment variables from the scripts in your `.bashrc`, see [Environment Variables](#envVar).

## Simple Functionality Test

The system, if working, will (by default) serve json data on port 8080. This can easily be changed (in Main.java) and is useful for testing.

## Running the Unit Tests

Due to the coupling to FHEM, mocking data, environment variables and a database have to be configured in order for the tests to pass.
The unit tests are located in `fabulousServer/WebServer/src/test/java` and require `jUnit4` to run.
We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/your/project/tags). 

## Library Licenses

The licenses of used libraries are in `fabulousServer/WebServer/LICENSES/`.
