dependencies {
    compile(project(":vok-framework"))
    compile(project(":vok-db"))
    compile(project(":vok-util-vaadin8"))

    testCompile("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")

    testCompile("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")
    testCompile("com.h2database:h2:${properties["h2_version"]}")
    testCompile("com.github.mvysny.kaributesting:karibu-testing-v8:${properties["kaributesting_version"]}")
    testCompile("org.zeroturnaround:zt-exec:1.10")

    testCompile("org.postgresql:postgresql:42.2.1")
    testCompile("mysql:mysql-connector-java:5.1.45")
    testCompile("org.mariadb.jdbc:mariadb-java-client:2.2.3")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-framework-vokdb", "VoK: provides integration of Vaadin8 and vok-db")

