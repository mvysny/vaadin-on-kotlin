dependencies {
    api(project(":vok-framework"))

    api("com.github.mvysny.vokdataloader:vok-dataloader:${properties["vok_dataloader_version"]}")

    // Vaadin
    api("com.github.mvysny.karibudsl:karibu-dsl:${properties["karibudsl_version"]}")
    api("com.vaadin:vaadin-core:${properties["vaadin10_version"]}") {
        // Webjars are only needed when running in Vaadin 13 compatibility mode
        listOf("com.vaadin.webjar", "org.webjars.bowergithub.insites",
                "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents")
                .forEach { group -> exclude(group = group) }
    }
    api("javax.servlet:javax.servlet-api:3.1.0")

    // testing
    testImplementation("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")
    testImplementation("com.github.mvysny.kaributesting:karibu-testing-v10:${properties["kaributesting_version"]}")
    testImplementation("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-util-vaadin10", "VOK: Basic utility classes for Vaadin 10")
