dependencies {
    api(project(":vok-framework"))
    api("com.gitlab.mvysny.jdbiorm:jdbi-orm:${properties["jdbi_orm_version"]}")
    api("com.gitlab.mvysny.jdbiormvaadin:jdbi-orm-vaadin:${properties["jdbi_orm_vaadin_version"]}")

    // Vaadin
    api("com.github.mvysny.karibudsl:karibu-dsl-v23:${properties["karibudsl_version"]}")
    api("com.vaadin:vaadin-core:${properties["vaadin_version"]}")
    api("jakarta.servlet:jakarta.servlet-api:5.0.0")

    // testing
    testImplementation("com.github.mvysny.dynatest:dynatest:${properties["dynatest_version"]}")
    testImplementation("com.github.mvysny.kaributesting:karibu-testing-v23:${properties["kaributesting_version"]}")
    testImplementation("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")
    testImplementation("com.github.mvysny.vokorm:vok-orm:${properties["vok_orm_version"]}")
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-util-vaadin", "VOK: Basic utility classes for Vaadin 10")
