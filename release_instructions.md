# How to release to maven central

- setup the private key for key-signing in the home directory (see https://dzone.com/articles/publish-your-artifacts-to-maven-central)
- update version to the next release number in `pom.xml`
- execute `mvn clean deploy`