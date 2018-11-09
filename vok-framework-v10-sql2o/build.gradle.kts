plugins {
    id("io.spring.dependency-management") version "1.0.6.RELEASE"
}

dependencyManagement {
    imports { mavenBom("com.vaadin:vaadin-bom:${ext["vaadin10_version"]}") }
}

dependencies {
    compile(project(":vok-framework"))
    compile(project(":vok-db"))
    compile(project(":vok-util-vaadin10"))

    testCompile("com.github.mvysny.dynatest:dynatest-engine:${ext["dynatest_version"]}")

    testCompile("ch.qos.logback:logback-classic:${ext["logback_version"]}")
    testCompile("com.h2database:h2:${ext["h2_version"]}")
    testCompile("com.github.mvysny.kaributesting:karibu-testing-v10:${ext["kaributesting_version"]}")
    testCompile("org.zeroturnaround:zt-exec:1.10")

    testCompile("org.postgresql:postgresql:42.2.1")
    testCompile("mysql:mysql-connector-java:5.1.45")
    testCompile("org.mariadb.jdbc:mariadb-java-client:2.2.3")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-framework-v10-sql2o", "VOK: Vaadin 10 Flow with VOK-DB persistence")

