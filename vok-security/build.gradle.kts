dependencies {
    api(project(":vok-framework"))
    api("com.vaadin:vaadin-core:${properties["vaadin_version"]}")
    api("javax.annotation:javax.annotation-api:1.3.2")
    testImplementation("com.github.mvysny.dynatest:dynatest:${properties["dynatest_version"]}")
    testImplementation("com.github.mvysny.kaributesting:karibu-testing-v10:${properties["kaributesting_version"]}")
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-security", "VoK: A generic support for simple security")

