dependencies {
    compile(project(":vok-framework"))

    compile("com.google.code.gson:gson:2.8.5")
    compile("com.squareup.okhttp3:okhttp:3.13.1")
    compile("com.github.mvysny.vokdataloader:vok-dataloader:${properties["vok_dataloader_version"]}")

    // this project is actually tested in the vok-rest project, where it is used as a test dependency. For tests please see vok-rest
    testCompile("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-rest-client", "VoK: The Client REST support")
