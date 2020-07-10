dependencies {
    implementation(project(":vok-framework"))
    implementation("com.github.mvysny.vokorm:vok-orm:${properties["vok_orm_version"]}")
    implementation("com.gitlab.mvysny.jdbiorm:jdbi-orm:0.5")
    implementation("com.github.mvysny.vokdataloader:vok-dataloader:${properties["vok_dataloader_version"]}")
    testImplementation("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-db", "VoK: A very simple persistence framework built on top of vok-orm")
