pluginManagement {
    // also download plugins from the vaadin-prereleases repository.
    // this will allow us to use alpha builds of Vaadin Gradle plugin, since those
    // are not published to https://plugins.gradle.org/plugin/com.vaadin
    //
    // also see https://vaadin.com/docs/latest/flow/guide/start/gradle#pre-release
    repositories {
        maven { setUrl("https://maven.vaadin.com/vaadin-prereleases") }
        gradlePluginPortal()
    }
}

rootProject.name = "vaadin-on-kotlin"
include("vok-framework",
        "vok-framework-vokdb",
        "vok-db",
        "vok-util-vaadin",
        "vok-framework-vokdb",
        "vok-rest",
        "vok-example-crud",
        "vok-security",
        "vok-rest-client"
)
