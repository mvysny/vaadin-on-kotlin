dependencies {
    api(project(":vok-framework"))
    api(libs.vokorm)

    testImplementation(libs.dynatest)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.h2)
    testImplementation(libs.hikaricp)
    testImplementation(libs.karibu.testing)
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-framework-vokdb", "VOK: Vaadin 10 Flow with VOK-DB persistence")
