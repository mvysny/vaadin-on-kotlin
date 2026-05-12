import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.20"
    `maven-publish`
    signing
}

defaultTasks("clean", "build")

allprojects {
    group = "eu.vaadinonkotlin"
    version = "0.19.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    tasks.withType<KotlinCompile> {
        // Vaadin 25 requires JDK 21+ anyway
        compilerOptions.jvmTarget = JvmTarget.JVM_21
    }
}

subprojects {
    apply {
        plugin("maven-publish")
        plugin("kotlin")
        plugin("org.gradle.signing")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            // to see the exception stacktraces of failed tests in the CI console.
            exceptionFormat = TestExceptionFormat.FULL
            showCauses = true
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // creates a reusable function which configures proper deployment to Maven Central
    ext["configureMavenCentral"] = { artifactId: String, description: String ->

        java {
            withJavadocJar()
            withSourcesJar()
        }

        tasks.withType<Javadoc> {
            isFailOnError = false
        }

        publishing {
            repositories {
                maven {
                    setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                    credentials {
                        username = project.properties["ossrhUsername"] as String? ?: "Unknown user"
                        password = project.properties["ossrhPassword"] as String? ?: "Unknown user"
                    }
                }
            }
            publications {
                create("mavenJava", MavenPublication::class.java).apply {
                    groupId = project.group.toString()
                    this.artifactId = artifactId
                    version = project.version.toString()
                    pom {
                        this.description = description
                        name = artifactId
                        url = "https://github.com/mvysny/vaadin-on-kotlin"
                        licenses {
                            license {
                                name = "The MIT License"
                                url = "https://opensource.org/licenses/MIT"
                                distribution = "repo"
                            }
                        }
                        developers {
                            developer {
                                id = "mavi"
                                name = "Martin Vysny"
                                email = "martin@vysny.me"
                            }
                        }
                        scm {
                            url = "https://github.com/mvysny/vaadin-on-kotlin"
                        }
                    }
                    from(components.findByName("java")!!)
                }
            }
        }

        signing {
            sign(publishing.publications["mavenJava"])
        }
    }
}
