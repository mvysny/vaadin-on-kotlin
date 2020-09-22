plugins {
    war
    id("org.gretty")
}

gretty {
    contextPath = "/"
    servletContainer = "jetty9.4"
}

dependencies {
    compile(project(":vok-framework-jpa"))
    testCompile("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")

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
    compile("org.hibernate:hibernate-hikaricp:5.2.11.Final") {
        exclude(mapOf("group" to "javax.enterprise"))
    }
    compile("com.zaxxer:HikariCP:${properties["hikaricp_version"]}")
    compile("com.h2database:h2:${properties["h2_version"]}")

    // REST
    compile(project(":vok-rest")) {
        exclude(module = "vok-db")
    }
    
    // testing
    testCompile("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")
    testCompile("com.github.mvysny.kaributesting:karibu-testing-v8:${properties["kaributesting_version"]}")
}
