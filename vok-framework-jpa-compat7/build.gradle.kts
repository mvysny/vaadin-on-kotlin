dependencies {
    compile(project(":vok-framework-jpa"))
    compile("com.vaadin:vaadin-compatibility-server:${ext["vaadin8_version"]}")

    compile("com.vaadin.addon:jpacontainer:4.0.0")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-framework-jpa-compat7", "VoK: Vaadin8+JPA integration, Vaadin7-compat support")

