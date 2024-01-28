// workaround for https://github.com/gradle/gradle/issues/9830
pluginManagement {
    val vaadin_version: String by settings
    plugins {
        id("com.vaadin") version vaadin_version
    }
}

include("vok-framework",
        "vok-framework-vokdb",
        "vok-util-vaadin",
        "vok-framework-vokdb",
        "vok-rest",
        "vok-example-crud",
        "vok-security",
        "vok-rest-client"
)
