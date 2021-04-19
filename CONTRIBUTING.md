# Contributing

Thank you so much for making the library better.

The library is in Kotlin, just use the default Kotlin formatter rules.

## Tests

All tests are written using [DynaTest](https://github.com/mvysny/dynatest). The reason
for that is:

- DynaTest provides the easiest way to create a reusable test suite which
  we can then run with various Vaadin versions on the classpath, in order to test
  compatibility with various Vaadin versions.

Usually we test compatibility with three Vaadin versions:
- the latest Vaadin LTS (currently Vaadin 14),
- the latest released version (currently Vaadin 19),
- the latest unreleased version (currently Vaadin 20)

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

1. Edit `build.gradle.kts` and remove `-SNAPSHOT` in the `version=` stanza
2. Commit with the commit message of simply being the version being released, e.g. "1.2.13"
3. git tag the commit with the same tag name as the commit message above, e.g. `1.2.13`
4. `git push`, `git push --tags`
5. Run `./gradlew clean build publish`
6. Continue to the [OSSRH Nexus](https://oss.sonatype.org/#stagingRepositories) and follow the [release procedure](https://central.sonatype.org/pages/releasing-the-deployment.html).
7. Add the `-SNAPSHOT` back to the `version=` while increasing the version to something which will be released in the future,
   e.g. 1.2.14, then commit with the commit message "1.2.14-SNAPSHOT" and push.

