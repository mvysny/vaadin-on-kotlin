dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compile("org.slf4j:slf4j-api:${ext["slf4j_version"]}")
    testCompile("com.github.mvysny.dynatest:dynatest-engine:${ext["dynatest_version"]}")
    testCompile("ch.qos.logback:logback-classic:${ext["logback_version"]}")
}

val configureBintray = ext["configureBintray"] as (artifactId: String, description: String) -> Unit
configureBintray("vok-framework", "VoK: The Core classes")

