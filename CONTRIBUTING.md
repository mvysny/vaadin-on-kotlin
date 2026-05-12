# Contributing

Thank you so much for making the library better.

The library is in Kotlin, just use the default Kotlin formatter rules.

## Tests

All tests are written using [JUnit 6 (Jupiter)](https://junit.org/).

### Running Tests

In order to launch the suite, simply navigate to the project in question
in your Intellij, then right-click the `test` folder and select
*Run All Tests*.

You can run all tests simply by running Gradle from the command-line:

```
./gradlew test
```

# Developing

Please feel free to open bug reports to discuss new features; PRs are welcome as well :)

## Releasing

To release the library to Maven Central:

1. Run a full build: `./gradlew clean build`
2. Edit `build.gradle.kts` and remove `-SNAPSHOT` in the `version=` stanza, e.g. "1.2.13"
3. Run `./gradlew clean build publish closeAndReleaseStagingRepositories`
4. (Optional) watch [Maven Central Publishing Deployments](https://central.sonatype.com/publishing/deployments) as the deployment is published.
5. Commit with the commit message of simply being the version being released, e.g. "1.2.13"
6. git tag the commit with the same tag name as the commit message above, e.g. `1.2.13`
7. `git push`, `git push --tags`
8. Add the `-SNAPSHOT` back to the `version=` while increasing the version to something which will be released in the future,
   e.g. 1.2.14-SNAPSHOT, then commit with the commit message "1.2.14-SNAPSHOT" and push.

