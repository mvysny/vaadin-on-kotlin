plugins {
    war
    id("org.gretty")
}

// don't update Jetty carelessly, it tends to break Atmosphere and Push support!
// test before commit :-)
// see https://github.com/vaadin/framework/issues/8134 for details
val jettyVer = "9.4.2.v20170220"

gretty {
    contextPath = "/"
    servletContainer = "jetty9.4"
}

dependencies {
    compile(project(":vok-framework-jpa"))
    testCompile("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")

    // logging
    // currently we are logging through the SLF4J API to LogBack. See logback.xml file for the logger configuration
    compile("ch.qos.logback:logback-classic:${properties["logback_version"]}")
    compile("org.slf4j:slf4j-api:${properties["slf4j_version"]}")
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
    compile("org.hibernate:hibernate-hikaricp:5.2.11.Final") {
        exclude(mapOf("group" to "javax.enterprise"))
    }
    compile("com.zaxxer:HikariCP:${properties["hikaricp_version"]}")
    compile("com.h2database:h2:${properties["h2_version"]}")

    // REST
    compile(project(":vok-rest")) {
        exclude(module = "vok-db")
    }
    
    // easy development with Jetty
    testCompile("org.eclipse.jetty:jetty-webapp:$jettyVer")
    testCompile("org.eclipse.jetty:jetty-annotations:$jettyVer")
    // workaround for https://github.com/Atmosphere/atmosphere/issues/978
    testCompile("org.eclipse.jetty:jetty-continuation:$jettyVer")
    // make sure that JSR356 is on classpath, otherwise Atmosphere will use native Jetty Websockets which will result
    // in ClassNotFoundException: org.eclipse.jetty.websocket.WebSocketFactory$Acceptor
    // since the class is no longer there in Jetty 9.4
    testCompile("org.eclipse.jetty.websocket:javax-websocket-server-impl:$jettyVer")

    // Embedded Undertow is currently unsupported since it has no servlet/listener/... autodiscovery capabilities:
    // http://stackoverflow.com/questions/22307748/deploying-servlets-webapp-in-embedded-undertow

    // Embedded Tomcat is currently unsupported since it always starts its own class loader which is only known on Tomcat start time
    // and we can't thus discover and preload JPA entities.

    // testing
    testCompile("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")
    testCompile("com.github.mvysny.kaributesting:karibu-testing-v8:${properties["kaributesting_version"]}")
}

