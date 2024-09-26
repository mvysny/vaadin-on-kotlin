dependencies {
    api(project(":vok-framework-vokdb"))

    api(libs.javalin) {
        exclude(group = "org.eclipse.jetty")
        exclude(group = "org.eclipse.jetty.websocket")
        exclude(group = "com.fasterxml.jackson.core")
    }
    api(libs.gson.gson)
    // workaround for https://github.com/google/gson/issues/1059
    implementation(libs.gson.javatime)

    // testing of the CRUD interface
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(project(":vok-rest-client"))
    testImplementation(libs.bundles.jetty)
    testImplementation(libs.flyway)
    testImplementation(libs.h2)
    testImplementation(libs.hikaricp)
    testImplementation(libs.slf4j.simple)
    // for testing fulltext search
    testImplementation(libs.lucene.queryparser)
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-rest", "VoK: The REST support")
