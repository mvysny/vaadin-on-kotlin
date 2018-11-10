dependencies {
    compile(project(":vok-framework"))
    compile("com.github.vokorm:vok-orm:0.13")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-db", "VoK: A very simple persistence framework built on top of vok-orm/Sql2o")
