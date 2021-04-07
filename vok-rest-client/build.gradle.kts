dependencies {
    api(project(":vok-framework"))

    api("com.google.code.gson:gson:2.8.5")
    api("com.squareup.moshi:moshi:1.11.0")
    api("com.squareup.okhttp3:okhttp:4.0.0")
    api("com.github.mvysny.vokdataloader:vok-dataloader:${properties["vok_dataloader_version"]}")

    // this project is actually tested in the vok-rest project, where it is used as a test dependency. For tests please see vok-rest
    testImplementation("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")
    testImplementation("io.javalin:javalin:${properties["javalin_version"]}")
    testImplementation("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")

    // temporary: add kotlin-reflect until https://github.com/mvysny/vaadin-on-kotlin/issues/60 is sorted out.
    testImplementation("com.squareup.moshi:moshi-kotlin:1.11.0")
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-rest-client", "VoK: The Client REST support")
