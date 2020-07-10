dependencies {
    implementation(project(":vok-framework"))
    implementation(project(":vok-security"))

    implementation("com.github.mvysny.vokdataloader:vok-dataloader:${properties["vok_dataloader_version"]}")

    // Vaadin
    implementation("com.github.mvysny.karibudsl:karibu-dsl-v8:${properties["karibudsl_version"]}")
    implementation("com.vaadin:vaadin-server:${properties["vaadin8_version"]}")
    implementation("javax.servlet:javax.servlet-api:3.1.0")

    // IDEA language injections
    implementation("com.intellij:annotations:12.0")

    // testing
    testImplementation("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")
    testImplementation("com.github.mvysny.kaributesting:karibu-testing-v8:${properties["kaributesting_version"]}")
    testImplementation("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-util-vaadin8", "VOK: Basic utility classes for Vaadin 8")

