dependencies {
    api(project(":vok-framework"))

    api("com.google.code.gson:gson:${properties["gson_version"]}")
    api("com.squareup.moshi:moshi:1.13.0")
    api("com.squareup.okhttp3:okhttp:4.9.3")
    api("com.github.mvysny.vokdataloader:vok-dataloader:${properties["vok_dataloader_version"]}")
    // workaround for https://github.com/google/gson/issues/1059
    implementation("com.fatboyindustrial.gson-javatime-serialisers:gson-javatime-serialisers:1.1.1")

    // this project is actually tested in the vok-rest project, where it is used as a test dependency. For tests please see vok-rest
    testImplementation("com.github.mvysny.dynatest:dynatest:${properties["dynatest_version"]}")
    testImplementation("io.javalin:javalin:${properties["javalin_version"]}") {
        exclude(group = "org.eclipse.jetty")
        exclude(group = "org.eclipse.jetty.websocket")
        exclude(group = "com.fasterxml.jackson.core")
    }
    testImplementation("org.eclipse.jetty:jetty-webapp:${properties["jetty_version"]}")
    testImplementation("org.eclipse.jetty.websocket:websocket-jakarta-server:${properties["jetty_version"]}")
    testImplementation("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")

    // temporary: add kotlin-reflect until https://github.com/mvysny/vaadin-on-kotlin/issues/60 is sorted out.
    testImplementation("com.squareup.moshi:moshi-kotlin:1.13.0")
    testImplementation(kotlin("reflect")) // make sure to use the same kotlin-reflect version as the kotlin language itself.
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-rest-client", "VoK: The Client REST support")
