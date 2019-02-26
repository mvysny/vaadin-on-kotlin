dependencies {
    compile(project(":vok-framework"))

    compile("io.javalin:javalin:${properties["javalin_version"]}") {
        exclude(mapOf("group" to "org.eclipse.jetty"))
        exclude(mapOf("group" to "org.eclipse.jetty.websocket"))
    }
    compile("com.google.code.gson:gson:2.8.5")
    compile(project(":vok-db"))

    // testing of the CRUD interface
    testCompile("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")
    testCompile(project(":vok-rest-client"))
    testCompile("org.eclipse.jetty.websocket:websocket-server:9.4.12.v20180830")
    testCompile("org.flywaydb:flyway-core:${properties["flyway_version"]}")
    testCompile("com.h2database:h2:${properties["h2_version"]}")
    testCompile("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-rest", "VoK: The REST support")
