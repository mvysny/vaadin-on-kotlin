import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.0"
    id("org.gretty") version "2.2.0"
    id("com.jfrog.bintray") version "1.8.1"
    `maven-publish`
}

defaultTasks("clean", "build")

allprojects {
    group = "com.github.vaadinonkotlin"
    version = "0.5.3-SNAPSHOT"

    repositories {
        jcenter()
        maven { setUrl("https://dl.bintray.com/mvysny/github") }
        maven { setUrl("https://maven.vaadin.com/vaadin-addons") }
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

        publishing {
            publications {
                create("mavenJava", MavenPublication::class.java).apply {
                    groupId = project.group.toString()
                    this.artifactId = artifactId
                    version = project.version.toString()
                    pom.withXml {
                        val root = asNode()
                        root.appendNode("description", description)
                        root.appendNode("name", artifactId)
                        root.appendNode("url", "https://github.com/mvysny/vaadin-on-kotlin")
                    }
                    from(components.findByName("java")!!)
                    artifact(sourceJar) {
                        classifier = "sources"
                    }
                }
            }
        }

        bintray {
            user = local.getProperty("bintray.user")
            key = local.getProperty("bintray.key")
            pkg(closureOf<BintrayExtension.PackageConfig> {
                repo = "github"
                name = "vaadin-on-kotlin"
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
