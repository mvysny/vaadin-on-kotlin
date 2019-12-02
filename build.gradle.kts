import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    kotlin("jvm") version "1.3.61"
    id("org.gretty") version "2.3.1"
    id("com.jfrog.bintray") version "1.8.3"
    `maven-publish`
    id("org.jetbrains.dokka") version "0.9.17"
}

defaultTasks("clean", "build")

allprojects {
    group = "eu.vaadinonkotlin"
    version = "0.8.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven { setUrl("https://maven.vaadin.com/vaadin-addons") }  // because of JPA Container
        maven { setUrl("https://maven.vaadin.com/vaadin-prereleases/") }
    }

    tasks {
        // Heroku
        val stage by registering {
            dependsOn("build")
        }
    }
}

subprojects {
    apply {
        plugin("maven-publish")
        plugin("kotlin")
        plugin("com.jfrog.bintray")
        plugin("org.jetbrains.dokka")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            // to see the exceptions of failed tests in Travis-CI console.
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    // creates a reusable function which configures proper deployment to Bintray
    ext["configureBintray"] = { artifactId: String, description: String ->

        val local = Properties()
        val localProperties: File = rootProject.file("local.properties")
        if (localProperties.exists()) {
            localProperties.inputStream().use { local.load(it) }
        }

        val java: JavaPluginConvention = convention.getPluginByName("java")

        val sourceJar = task("sourceJar", Jar::class) {
            dependsOn(tasks.findByName("classes"))
            classifier = "sources"
            from(java.sourceSets["main"].allSource)
        }

        val javadocJar = task("javadocJar", Jar::class) {
            val javadoc = tasks.findByName("dokka") as DokkaTask
            javadoc.outputFormat = "javadoc"
            javadoc.outputDirectory = "$buildDir/javadoc"
            dependsOn(javadoc)
            classifier = "javadoc"
            from(javadoc.outputDirectory)
        }

        publishing {
            publications {
                create("mavenJava", MavenPublication::class.java).apply {
                    groupId = project.group.toString()
                    this.artifactId = artifactId
                    version = project.version.toString()
                    pom {
                        this.description.set(description)
                        name.set(artifactId)
                        url.set("https://github.com/mvysny/vaadin-on-kotlin")
                        licenses {
                            license {
                                name.set("The MIT License")
                                url.set("https://opensource.org/licenses/MIT")
                                distribution.set("repo")
                            }
                        }
                        developers {
                            developer {
                                id.set("mavi")
                                name.set("Martin Vysny")
                                email.set("martin@vysny.me")
                            }
                        }
                        scm {
                            url.set("https://github.com/mvysny/vaadin-on-kotlin")
                        }
                    }
                    from(components.findByName("java")!!)
                    artifact(sourceJar)
                    artifact(javadocJar)
                }
            }
        }

        bintray {
            user = local.getProperty("bintray.user")
            key = local.getProperty("bintray.key")
            pkg(closureOf<BintrayExtension.PackageConfig> {
                repo = "github"
                name = "eu.vaadinonkotlin"
                setLicenses("MIT")
                vcsUrl = "https://github.com/mvysny/vaadin-on-kotlin"
                publish = true
                setPublications("mavenJava")
                version(closureOf<BintrayExtension.VersionConfig> {
                    this.name = project.version.toString()
                    released = Date().toString()
                })
            })
        }
    }
}
