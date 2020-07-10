dependencies {
    implementation(project(":vok-framework"))
    implementation(project(":vok-util-vaadin8"))

    testImplementation("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")

    // db
    implementation("org.hibernate:hibernate-core:5.3.0.Final") {
        exclude(mapOf("group" to "javax.enterprise")) // there is no need for CDI-API nor @Inject stuff
    }
    // support for Java 9: https://stackoverflow.com/questions/48986999/classnotfoundexception-for-javax-xml-bind-jaxbexception-with-spring-boot-when-sw
    implementation("javax.xml.bind:jaxb-api:2.3.0")

    testImplementation("org.flywaydb:flyway-core:${properties["flyway_version"]}")
    testImplementation("com.h2database:h2:${properties["h2_version"]}")
    testImplementation("com.github.mvysny.kaributesting:karibu-testing-v8:${properties["kaributesting_version"]}")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-framework-jpa", "VoK: Vaadin8+JPA integration")

