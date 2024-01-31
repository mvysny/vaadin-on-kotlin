import com.vaadin.gradle.getBooleanProperty

plugins {
    id("application")
    alias(libs.plugins.vaadin)
}

dependencies {
    implementation(project(":vok-framework-vokdb"))
    implementation(libs.vaadin.core) {
        // https://github.com/vaadin/flow/issues/18572
        if (vaadin.productionMode.map { v -> getBooleanProperty("vaadin.productionMode") ?: v }.get()) {
            exclude(module = "vaadin-dev")
        }
    }
    implementation(libs.vaadinboot)

    // logging
    // currently we are logging through the SLF4J API to slf4j-simple. See simplelogger.properties file for the logger configuration
    implementation(libs.slf4j.simple)

    // db
    implementation(libs.flyway)
    implementation(libs.h2)
    implementation(libs.hikaricp)

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
