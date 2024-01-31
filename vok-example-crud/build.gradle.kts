plugins {
    id("application")
    alias(libs.plugins.vaadin)
}

dependencies {
    implementation(project(":vok-framework-vokdb"))
    implementation("com.vaadin:vaadin-core:${properties["vaadin_version"]}") {
        afterEvaluate {
            if (vaadin.productionMode.get()) {
                exclude(module = "vaadin-dev")
            }
        }
    }
    implementation(libs.vaadinboot)

    // logging
    // currently we are logging through the SLF4J API to slf4j-simple. See simplelogger.properties file for the logger configuration
    implementation(libs.slf4j.simple)

    // db
    implementation("org.flywaydb:flyway-core:${properties["flyway_version"]}")
    implementation(libs.h2)
    implementation("com.zaxxer:HikariCP:${properties["hikaricp_version"]}")

    // REST
    implementation(project(":vok-rest"))

    // testing
    testImplementation(libs.dynatest)
    testImplementation(libs.karibu.testing)
    testImplementation(project(":vok-rest-client"))
}

application {
    mainClass.set("example.crudflow.MainKt")
}
