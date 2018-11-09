plugins {
    id("io.spring.dependency-management") version "1.0.6.RELEASE"
}

dependencyManagement {
    imports { mavenBom("com.vaadin:vaadin-bom:${ext["vaadin10_version"]}") }
}

dependencies {
    compile(project(":vok-framework"))
    compile(project(":vok-security"))

    // Vaadin
    compile("com.github.mvysny.karibudsl:karibu-dsl-v10:${ext["karibudsl_version"]}")
    compile("javax.servlet:javax.servlet-api:3.1.0")

    // testing
    testCompile("com.github.mvysny.dynatest:dynatest-engine:${ext["dynatest_version"]}")
    testCompile("com.github.mvysny.kaributesting:karibu-testing-v10:${ext["kaributesting_version"]}")
    testCompile("ch.qos.logback:logback-classic:${ext["logback_version"]}")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-util-vaadin10", "VOK: Basic utility classes for Vaadin 10")
