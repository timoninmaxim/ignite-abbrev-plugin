* [Prerequisites](#prerequisites)
* [Quick Start](#quick-start)
* [Building Ignite](#building-ignite)
* [Building Ignite](#test-in-ide)
***

## Prerequisites
- Java 17
- Gradle 8.5+
- IntelliJ IDEA 2022.2+ (For development)
***

## Building Ignite Abbreviation plugin
Build: the plugin and prepares the ZIP archive for testing and deployment.
```shell
./gradlew clean build -x test -x integrationTest
```
The archive will be located in build/distributions
***

## Test in IDE
Run the IDE instance using the currently selected IntelliJ Platform with the built plugin loaded:
```shell
./gradlew runIde
```
***

## Other
To see full list of available commands with info:
```shell
./gradlew tasks
```
