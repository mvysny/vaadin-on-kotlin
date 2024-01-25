dependencies {
    api(project(":vok-framework"))
    api("com.vaadin:flow-data:${properties["vaadin_version"]}")
    api("com.gitlab.mvysny.jdbiorm:jdbi-orm:2.6") // only used for the Condition API

    api("com.google.code.gson:gson:${properties["gson_version"]}")
    api("com.gitlab.mvysny.apache-uribuilder:uribuilder:5.2.1")
    // workaround for https://github.com/google/gson/issues/1059
    implementation("com.fatboyindustrial.gson-javatime-serialisers:gson-javatime-serialisers:1.1.1")

    // this project is actually tested in the vok-rest project, where it is used as a test dependency. For tests please see vok-rest
    testImplementation("com.github.mvysny.dynatest:dynatest:${properties["dynatest_version"]}")
    testImplementation("io.javalin:javalin:${properties["javalin_version"]}") {
        exclude(group = "org.eclipse.jetty")
        exclude(group = "org.eclipse.jetty.websocket")
        exclude(group = "com.fasterxml.jackson.core")
    }
    testImplementation("org.eclipse.jetty.ee10:jetty-ee10-webapp:${properties["jetty_version"]}")
    testImplementation("org.eclipse.jetty.ee10.websocket:jetty-ee10-websocket-jakarta-server:${properties["jetty_version"]}")
    testImplementation("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-rest-client", "VoK: The Client REST support")
