dependencies {
    api(project(":vok-framework"))
    api("com.github.mvysny.vokorm:vok-orm:${properties["vok_orm_version"]}")
    api("com.gitlab.mvysny.jdbiorm:jdbi-orm:2.6")
    testImplementation("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-db", "VoK: A very simple persistence framework built on top of vok-orm")
