plugins {
    war
    id("org.gretty")
}

gretty {
    contextPath = "/"
    servletContainer = "jetty9.4"
}

dependencies {
    compile(project(":vok-framework-v10-vokdb"))
    compile("com.vaadin:flow-server-compatibility-mode:2.0.16")
    providedCompile("javax.servlet:javax.servlet-api:3.1.0")

    // logging
    // currently we are logging through the SLF4J API to slf4j-simple. See simplelogger.properties file for the logger configuration
    compile("org.slf4j:slf4j-api:${properties["slf4j_version"]}")
    compile("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")

    // db
    compile("org.flywaydb:flyway-core:${properties["flyway_version"]}")
    compile("com.h2database:h2:${properties["h2_version"]}")

    // REST
    compile(project(":vok-rest"))

    // testing
    testCompile("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")
    testCompile("com.github.mvysny.kaributesting:karibu-testing-v10:${properties["kaributesting_version"]}")
    testCompile(project(":vok-rest-client"))
    testCompile("org.eclipse.jetty.websocket:websocket-server:9.4.12.v20180830")
}
