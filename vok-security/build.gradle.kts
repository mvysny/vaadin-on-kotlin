dependencies {
    api(project(":vok-framework"))
    api("com.github.mvysny.vaadin-simple-security:vaadin-simple-security:0.2")
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-security", "VoK: A generic support for simple security")
