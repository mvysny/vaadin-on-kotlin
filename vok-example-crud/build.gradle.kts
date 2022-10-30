plugins {
    id("application")
    id("com.vaadin")
}

dependencies {
    implementation(project(":vok-framework-vokdb"))
    implementation("com.vaadin:vaadin-core:${properties["vaadin_version"]}")
    implementation("com.github.mvysny.vaadin-boot:vaadin-boot:10.1")

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
    testImplementation("com.github.mvysny.kaributesting:karibu-testing-v23:${properties["kaributesting_version"]}")
    testImplementation(project(":vok-rest-client"))
}

application {
    mainClass.set("example.crudflow.MainKt")
}
