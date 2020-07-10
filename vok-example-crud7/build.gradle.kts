plugins {
    war
}

// don't update Jetty carelessly, it tends to break Atmosphere and Push support!
// test before commit :-)
// see https://github.com/vaadin/framework/issues/8134 for details
val jettyVer = "9.4.2.v20170220"

dependencies {
    implementation("com.github.mvysny.karibudsl:karibu-dsl-v8compat7:${properties["karibudsl_version"]}")

    implementation(project(":vok-framework-jpa-compat7"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation("com.github.mvysny.dynatest:dynatest-engine:${properties["dynatest_version"]}")

    // logging
    implementation("org.slf4j:slf4j-api:${properties["slf4j_version"]}")
    implementation("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")

    // Vaadin
    implementation("com.vaadin:vaadin-compatibility-client-compiled:${properties["vaadin8_version"]}")
    implementation("com.vaadin:vaadin-compatibility-server:${properties["vaadin8_version"]}")
    implementation("com.vaadin:vaadin-push:${properties["vaadin8_version"]}")
    implementation("com.vaadin:vaadin-themes:${properties["vaadin8_version"]}")
    providedCompile("javax.servlet:javax.servlet-api:3.1.0")

    // db
    implementation("org.flywaydb:flyway-core:${properties["flyway_version"]}")
    implementation("org.hibernate:hibernate-hikaricp:5.2.11.Final") {
        exclude(mapOf("group" to "javax.enterprise"))
    }
    implementation("com.zaxxer:HikariCP:${properties["hikaricp_version"]}")
    implementation("com.h2database:h2:${properties["h2_version"]}")

    // REST
    implementation(project(":vok-rest"))
    
    // easy development with Jetty
    testImplementation("org.eclipse.jetty:jetty-webapp:$jettyVer")
    testImplementation("org.eclipse.jetty:jetty-annotations:$jettyVer")
    // workaround for https://github.com/Atmosphere/atmosphere/issues/978
    testImplementation("org.eclipse.jetty:jetty-continuation:$jettyVer")
}

