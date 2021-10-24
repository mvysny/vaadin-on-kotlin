plugins {
    war
    id("org.gretty")
    id("com.vaadin")
}

gretty {
    contextPath = "/"
    servletContainer = "jetty9.4"
}

dependencies {
    implementation(project(":vok-framework-vokdb"))
    implementation("com.vaadin:vaadin-core:${properties["vaadin_version"]}") {
        // Webjars are only needed when running in Vaadin 13 compatibility mode
        listOf("com.vaadin.webjar", "org.webjars.bowergithub.insites",
                "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents")
                .forEach { group -> exclude(group = group) }
    }
    providedCompile("javax.servlet:javax.servlet-api:3.1.0")

    // logging
    // currently we are logging through the SLF4J API to slf4j-simple. See simplelogger.properties file for the logger configuration
    implementation("org.slf4j:slf4j-api:${properties["slf4j_version"]}")
    implementation("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")

    // db
    implementation("org.flywaydb:flyway-core:${properties["flyway_version"]}")
    implementation("com.h2database:h2:${properties["h2_version"]}")
    implementation("com.zaxxer:HikariCP:${properties["hikaricp_version"]}")

    // REST
    implementation(project(":vok-rest"))

    // testing
    testImplementation("com.github.mvysny.dynatest:dynatest:${properties["dynatest_version"]}")
    testImplementation("com.github.mvysny.kaributesting:karibu-testing-v10:${properties["kaributesting_version"]}")
    testImplementation(project(":vok-rest-client"))
    testImplementation("org.eclipse.jetty.websocket:websocket-server:9.4.12.v20180830")
}
