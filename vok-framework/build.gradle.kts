dependencies {
    implementation(libs.slf4j.api)

    // Vaadin
    api(libs.karibu.dsl)
    api(libs.vaadin.core)
    api(libs.jakarta.servlet)

    // testing
    testImplementation(libs.junit)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.karibu.testing)
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureMavenCentral"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-framework", "VoK: The Framework")
