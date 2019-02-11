dependencies {
    compile(project(":vok-framework"))
    compile(project(":vok-util-vaadin8"))

    testCompile("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")

    // db
    compile("org.hibernate:hibernate-core:5.3.0.Final") {
        exclude(mapOf("group" to "javax.enterprise")) // there is no need for CDI-API nor @Inject stuff
    }
    // support for Java 9: https://stackoverflow.com/questions/48986999/classnotfoundexception-for-javax-xml-bind-jaxbexception-with-spring-boot-when-sw
    compile("javax.xml.bind:jaxb-api:2.3.0")

    testCompile("org.flywaydb:flyway-core:${properties["flyway_version"]}")
    testCompile("com.h2database:h2:${properties["h2_version"]}")
    testCompile("com.github.mvysny.kaributesting:karibu-testing-v8:${properties["kaributesting_version"]}")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-framework-jpa", "VoK: Vaadin8+JPA integration")

