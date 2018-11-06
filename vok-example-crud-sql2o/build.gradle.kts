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
    compile(project(":vok-framework-sql2o"))

    // logging
    // currently we are logging through the SLF4J API to LogBack. See logback.xml file for the logger configuration
    compile("ch.qos.logback:logback-classic:${ext["logback_version"]}")
    compile("org.slf4j:slf4j-api:${ext["slf4j_version"]}")
    // this will configure Vaadin to log to SLF4J
    compile("org.slf4j:jul-to-slf4j:${ext["slf4j_version"]}")

    // Vaadin
    compile("com.vaadin:vaadin-client-compiled:${ext["vaadin8_version"]}")
    compile("com.vaadin:vaadin-server:${ext["vaadin8_version"]}")
    compile("com.vaadin:vaadin-push:${ext["vaadin8_version"]}")
    compile("com.vaadin:vaadin-themes:${ext["vaadin8_version"]}")
    providedCompile("javax.servlet:javax.servlet-api:3.1.0")

    // db
    compile("org.flywaydb:flyway-core:${ext["flyway_version"]}")
    compile("com.h2database:h2:${ext["h2_version"]}")

    // REST
    compile(project(":vok-rest"))

    // testing
    testCompile("com.github.mvysny.dynatest:dynatest-engine:${ext["dynatest_version"]}")
    testCompile("com.github.kaributesting:karibu-testing-v8:${ext["kaributesting_version"]}")
    testCompile("khttp:khttp:0.1.0") {
        exclude(mapOf("group" to "org.json"))
    }
    testCompile("org.eclipse.jetty.websocket:websocket-server:9.4.12.v20180830")

    // heroku app runner
    staging("com.github.jsimone:webapp-runner:9.0.11.0")
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
