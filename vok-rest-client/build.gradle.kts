dependencies {
    compile(project(":vok-framework"))

    compile("com.google.code.gson:gson:2.8.5")
    compile("com.squareup.retrofit2:converter-scalars:2.4.0")
    compile("com.squareup.retrofit2:converter-gson:2.4.0")
    compile("com.github.mvysny.vokdataloader:vok-dataloader:${ext["vok_dataloader_version"]}")

    // this project is actually tested in the vok-rest project, where it is used as a test dependency. For tests please see vok-rest
    testCompile("com.github.mvysny.dynatest:dynatest-engine:${ext["dynatest_version"]}")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-rest-client", "VoK: The Client REST support")
