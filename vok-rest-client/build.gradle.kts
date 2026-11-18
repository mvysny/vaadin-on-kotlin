dependencies {
    api(project(":vok-framework"))

    api(libs.bundles.gson)
    api(libs.apache.uribuilder)

    // this project is actually tested in the vok-rest project, where it is used as a test dependency. For tests please see vok-rest
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.javalin) {
        // The app brings its own servlet container.
        exclude(group = "org.eclipse.jetty")
        exclude(group = "org.eclipse.jetty.ee10")
        exclude(group = "org.eclipse.jetty.ee10.websocket")
    }
    testImplementation(libs.bundles.jetty)
    testImplementation(libs.slf4j.simple)
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureMavenCentral"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-rest-client", "VoK: The Client REST support")
