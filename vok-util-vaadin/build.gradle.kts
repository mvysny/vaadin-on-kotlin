dependencies {
    api(project(":vok-framework"))

    api("com.github.mvysny.vokdataloader:vok-dataloader:${properties["vok_dataloader_version"]}")

    // Vaadin
    api("com.github.mvysny.karibudsl:karibu-dsl:${properties["karibudsl_version"]}")
    api("com.github.mvysny.karibu-tools:karibu-tools:0.8")
    api("com.vaadin:vaadin-core:${properties["vaadin_version"]}") {
        exclude(module = "fusion-endpoint") // exclude fusion: it brings tons of dependencies (including swagger)
    }
    api("javax.servlet:javax.servlet-api:4.0.1")

    // testing
    testImplementation("com.github.mvysny.dynatest:dynatest:${properties["dynatest_version"]}")
    testImplementation("com.github.mvysny.kaributesting:karibu-testing-v10:${properties["kaributesting_version"]}")
    testImplementation("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-util-vaadin", "VOK: Basic utility classes for Vaadin 10")
