plugins {
    war
}

// don't update Jetty carelessly, it tends to break Atmosphere and Push support!
// test before commit :-)
// see https://github.com/vaadin/framework/issues/8134 for details
val jettyVer = "9.4.2.v20170220"

dependencies {
    compile("com.github.vok.karibudsl:karibu-dsl-v8compat7:${ext["karibudsl_version"]}")

    compile(project(":vok-framework-jpa-compat7"))
    compile("org.jetbrains.kotlin:kotlin-stdlib")
    testCompile("com.github.mvysny.dynatest:dynatest-engine:${ext["dynatest_version"]}")

    // logging
    compile("ch.qos.logback:logback-classic:${ext["logback_version"]}")
    compile("org.slf4j:slf4j-api:${ext["slf4j_version"]}")

    // Vaadin
    compile("com.vaadin:vaadin-compatibility-client-compiled:${ext["vaadin8_version"]}")
    compile("com.vaadin:vaadin-compatibility-server:${ext["vaadin8_version"]}")
    compile("com.vaadin:vaadin-push:${ext["vaadin8_version"]}")
    compile("com.vaadin:vaadin-themes:${ext["vaadin8_version"]}")
    providedCompile("javax.servlet:javax.servlet-api:3.1.0")

    // db
    compile("org.flywaydb:flyway-core:${ext["flyway_version"]}")
    compile("org.hibernate:hibernate-hikaricp:5.2.11.Final") {
        exclude(mapOf("group" to "javax.enterprise"))
    }
    compile("com.zaxxer:HikariCP:${ext["hikaricp_version"]}")
    compile("com.h2database:h2:${ext["h2_version"]}")

    // REST
    compile(project(":vok-rest"))
    
    // easy development with Jetty
    testCompile("org.eclipse.jetty:jetty-webapp:$jettyVer")
    testCompile("org.eclipse.jetty:jetty-annotations:$jettyVer")
    // workaround for https://github.com/Atmosphere/atmosphere/issues/978
    testCompile("org.eclipse.jetty:jetty-continuation:$jettyVer")
}

