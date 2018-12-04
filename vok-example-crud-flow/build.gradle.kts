plugins {
    war
    id("org.gretty")
}

gretty {
    contextPath = "/"
    servletContainer = "jetty9.4"
}

dependencies {
    compile(project(":vok-framework-v10-sql2o"))
    providedCompile("javax.servlet:javax.servlet-api:3.1.0")

    // logging
    // currently we are logging through the SLF4J API to LogBack. See logback.xml file for the logger configuration
    compile("ch.qos.logback:logback-classic:${ext["logback_version"]}")
    compile("org.slf4j:slf4j-api:${ext["slf4j_version"]}")

    // db
    compile("org.flywaydb:flyway-core:${ext["flyway_version"]}")
    compile("com.h2database:h2:${ext["h2_version"]}")

    // REST
    compile(project(":vok-rest"))

    // testing
    testCompile("com.github.mvysny.dynatest:dynatest-engine:${ext["dynatest_version"]}")
    testCompile("com.github.mvysny.kaributesting:karibu-testing-v10:${ext["kaributesting_version"]}")
    testCompile(project(":vok-rest-client"))
    testCompile("org.eclipse.jetty.websocket:websocket-server:9.4.12.v20180830")
}
