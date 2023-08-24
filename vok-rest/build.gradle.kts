dependencies {
    api(project(":vok-framework"))

    api("io.javalin:javalin:${properties["javalin_version"]}") {
        exclude(group = "org.eclipse.jetty")
        exclude(group = "org.eclipse.jetty.websocket")
        exclude(group = "com.fasterxml.jackson.core")
    }
    api("com.google.code.gson:gson:${properties["gson_version"]}")
    // workaround for https://github.com/google/gson/issues/1059
    implementation("com.fatboyindustrial.gson-javatime-serialisers:gson-javatime-serialisers:1.1.1")
    api(project(":vok-db"))

    // testing of the CRUD interface
    testImplementation("com.github.mvysny.dynatest:dynatest:${properties["dynatest_version"]}")
    testImplementation(project(":vok-rest-client"))
    testImplementation("org.eclipse.jetty.ee10:jetty-ee10-webapp:${properties["jetty_version"]}")
    testImplementation("org.eclipse.jetty.ee10.websocket:jetty-ee10-websocket-jakarta-server:${properties["jetty_version"]}")
    testImplementation("org.flywaydb:flyway-core:${properties["flyway_version"]}")
    testImplementation("com.h2database:h2:${properties["h2_version"]}")
    testImplementation("com.zaxxer:HikariCP:${properties["hikaricp_version"]}")
    testImplementation("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")
    // for testing fulltext search
    testImplementation("org.apache.lucene:lucene-queryparser:8.11.1")
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-rest", "VoK: The REST support")
