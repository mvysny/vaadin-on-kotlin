dependencies {
    api(project(":vok-framework-vokdb"))

    testImplementation("com.github.mvysny.dynatest:dynatest:${properties["dynatest_version"]}")
    testImplementation("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")
    testImplementation("com.h2database:h2:${properties["h2_version"]}")
    testImplementation("com.zaxxer:HikariCP:${properties["hikaricp_version"]}")
    testImplementation("com.github.mvysny.kaributesting:karibu-testing-v10:${properties["kaributesting_version"]}")
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-framework-v17-vokdb", "VOK: Vaadin 17 with VOK-DB persistence")
