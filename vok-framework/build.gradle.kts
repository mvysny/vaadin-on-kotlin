dependencies {
    implementation(libs.slf4j.api)
    api(libs.jdbiorm)
    api(libs.jdbiormvaadin)

    // Vaadin
    api(libs.karibu.dsl)
    api(libs.vaadin.core)
    api(libs.jakarta.servlet)

    // testing
    testImplementation(libs.junit)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.karibu.testing)
    testImplementation(libs.vokorm)
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureMavenCentral"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-framework", "VoK: The Framework")
