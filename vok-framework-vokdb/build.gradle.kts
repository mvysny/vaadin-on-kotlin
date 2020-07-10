dependencies {
    implementation(project(":vok-framework"))
    implementation(project(":vok-db"))
    implementation(project(":vok-util-vaadin8"))

    testImplementation("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")

    testImplementation("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")
    testImplementation("com.h2database:h2:${properties["h2_version"]}")
    testImplementation("com.zaxxer:HikariCP:${properties["hikaricp_version"]}")
    testImplementation("com.github.mvysny.kaributesting:karibu-testing-v8:${properties["kaributesting_version"]}")
    testImplementation("org.zeroturnaround:zt-exec:1.10")

    testImplementation("org.postgresql:postgresql:42.2.1")
    testImplementation("mysql:mysql-connector-java:5.1.45")
    testImplementation("org.mariadb.jdbc:mariadb-java-client:2.2.3")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-framework-vokdb", "VoK: provides integration of Vaadin8 and vok-db")

