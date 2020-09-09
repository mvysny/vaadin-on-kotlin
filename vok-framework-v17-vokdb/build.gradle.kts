dependencies {
    compile(project(":vok-framework-v10-vokdb")) {
        exclude(module = "vaadin-core")
    }
    compile("com.vaadin:vaadin-core:${properties["vaadin17_version"]}")

    testCompile("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")
    testCompile("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")
    testCompile("com.h2database:h2:${properties["h2_version"]}")
    testCompile("com.zaxxer:HikariCP:${properties["hikaricp_version"]}")
    testCompile("com.github.mvysny.kaributesting:karibu-testing-v10:${properties["kaributesting_version"]}")
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-framework-v17-vokdb", "VOK: Vaadin 17 with VOK-DB persistence")
