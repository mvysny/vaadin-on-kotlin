dependencies {
    api(project(":vok-framework"))
    testImplementation("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-security", "VoK: A generic support for simple security")

