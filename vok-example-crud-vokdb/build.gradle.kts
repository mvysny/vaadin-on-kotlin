plugins {
    war
    id("org.gretty")
}

gretty {
    contextPath = "/"
    servletContainer = "jetty9.4"
}

val staging by configurations.creating

dependencies {
    compile(project(":vok-framework-vokdb"))

    // logging
    // currently we are logging through the SLF4J API to slf4j-simple. See simplelogger.properties file for the logger configuration
    compile("org.slf4j:slf4j-api:${properties["slf4j_version"]}")
    compile("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")
    // this will configure Vaadin to log to SLF4J
    compile("org.slf4j:jul-to-slf4j:${properties["slf4j_version"]}")

    // Vaadin
    compile("com.vaadin:vaadin-client-compiled:${properties["vaadin8_version"]}")
    compile("com.vaadin:vaadin-server:${properties["vaadin8_version"]}")
    compile("com.vaadin:vaadin-push:${properties["vaadin8_version"]}")
    compile("com.vaadin:vaadin-themes:${properties["vaadin8_version"]}")
    providedCompile("javax.servlet:javax.servlet-api:3.1.0")

    // db
    compile("org.flywaydb:flyway-core:${properties["flyway_version"]}")
    compile("com.h2database:h2:${properties["h2_version"]}")
    compile("com.zaxxer:HikariCP:${properties["hikaricp_version"]}")

    // REST
    compile(project(":vok-rest"))

    // testing
    testCompile("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")
    testCompile("com.github.mvysny.kaributesting:karibu-testing-v8:${properties["kaributesting_version"]}")
    testCompile(project(":vok-rest-client"))
    testCompile("org.eclipse.jetty.websocket:websocket-server:9.4.12.v20180830") // because of Javalin

    // heroku app runner
    staging("com.github.jsimone:webapp-runner:9.0.27.1")
}

// Heroku
tasks {
    val copyToLib by registering(Copy::class) {
        into("$buildDir/server")
        from(staging) {
            include("webapp-runner*")
        }
    }
    "stage" {
        dependsOn("build", copyToLib)
    }
}
