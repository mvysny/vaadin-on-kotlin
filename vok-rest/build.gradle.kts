dependencies {
    compile(project(":vok-framework"))

    compile("org.jboss.resteasy:resteasy-servlet-initializer:3.6.1.Final")
    compile("com.google.code.gson:gson:2.8.5")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-rest", "VoK: The REST support")

