# VoK Framework Core

The core module of the Vaadin-on-Kotlin framework, always included in your projects, typically
as a transitive dependency of other modules as they are included in your project.

This module provides the means to bootstrap/teardown the VoK runtime, typically from your
`ServletContextListener` as follows:

```kotlin
@WebListener
class Bootstrap: ServletContextListener {
    override fun contextInitialized(sce: ServletContextEvent?) {
        VaadinOnKotlin.init()
    }

    override fun contextDestroyed(sce: ServletContextEvent?) {
        VaadinOnKotlin.destroy()
    }
}
```

This will initialize all VoK plugins properly. The VoK plugins also tend to add additional fields
to the `VaadinOnKotlin` object. For example the `vok-db` module adds the
`VaadinOnKotlin.dataSourceConfig` property which allows you to specify the JDBC URL, username, password,
JDBC connection pool configuration and other.
