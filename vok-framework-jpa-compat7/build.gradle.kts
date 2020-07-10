dependencies {
    implementation(project(":vok-framework-jpa"))
    implementation("com.vaadin:vaadin-compatibility-server:${properties["vaadin8_version"]}")

    implementation("com.vaadin.addon:jpacontainer:4.0.0")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-framework-jpa-compat7", "VoK: Vaadin8+JPA integration, Vaadin7-compat support")

