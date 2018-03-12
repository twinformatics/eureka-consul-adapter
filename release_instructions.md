# How to release to maven central

- setup the private key for key-signing in the home directory (see )
- update version to the next release number in `pom.xml`
- execute `mvn clean deploy`