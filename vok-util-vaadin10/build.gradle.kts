dependencies {
    compile(project(":vok-framework"))
    compile(project(":vok-security"))

    compile("com.github.mvysny.vokdataloader:vok-dataloader:${properties["vok_dataloader_version"]}")

    // Vaadin
    compile("com.github.mvysny.karibudsl:karibu-dsl-v10:${properties["karibudsl_version"]}")
    compile(platform("com.vaadin:vaadin-bom:${properties["vaadin10_version"]}"))
    compile("javax.servlet:javax.servlet-api:3.1.0")

    // testing
    testCompile("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")
    testCompile("com.github.mvysny.kaributesting:karibu-testing-v10:${properties["kaributesting_version"]}")
    testCompile("ch.qos.logback:logback-classic:${properties["logback_version"]}")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-util-vaadin10", "VOK: Basic utility classes for Vaadin 10")
