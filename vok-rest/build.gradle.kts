dependencies {
    implementation(project(":vok-framework"))

    implementation("io.javalin:javalin:${properties["javalin_version"]}") {
        exclude(mapOf("group" to "org.eclipse.jetty"))
        exclude(mapOf("group" to "org.eclipse.jetty.websocket"))
    }
    implementation("com.google.code.gson:gson:2.8.5")
    implementation(project(":vok-db"))

    // testing of the CRUD interface
    testImplementation("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")
    testImplementation(project(":vok-rest-client"))
    testImplementation("org.eclipse.jetty.websocket:websocket-server:9.4.20.v20190813")
    testImplementation("org.flywaydb:flyway-core:${properties["flyway_version"]}")
    testImplementation("com.h2database:h2:${properties["h2_version"]}")
    testImplementation("com.zaxxer:HikariCP:${properties["hikaricp_version"]}")
    testImplementation("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")
    // for testing fulltext search
    testImplementation("org.apache.lucene:lucene-queryparser:8.5.2")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-rest", "VoK: The REST support")
