dependencies {
    api(project(":vok-framework"))
    api(libs.jdbiorm) // only used for the Condition API

    api(libs.bundles.gson)
    api(libs.apache.uribuilder)

    // this project is actually tested in the vok-rest project, where it is used as a test dependency. For tests please see vok-rest
    testImplementation(libs.dynatest)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.javalin) {
        exclude(group = "org.eclipse.jetty")
        exclude(group = "org.eclipse.jetty.websocket")
        exclude(group = "com.fasterxml.jackson.core")
    }
    testImplementation(libs.bundles.jetty)
    testImplementation(libs.slf4j.simple)
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-rest-client", "VoK: The Client REST support")
