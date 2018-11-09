package com.github.vok.example.crud

import eu.vaadinonkotlin.VaadinOnKotlin
import eu.vaadinonkotlin.vaadin8.jpa.entityManagerFactory
import org.atmosphere.util.annotation.AnnotationDetector
import org.eclipse.jetty.annotations.AnnotationConfiguration
import org.eclipse.jetty.plus.webapp.EnvConfiguration
import org.eclipse.jetty.plus.webapp.PlusConfiguration
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.*
import org.hibernate.jpa.AvailableSettings
import javax.persistence.Entity
import javax.persistence.Persistence


/**
 * Runs the testing Jetty-based server. Just open http://localhost:8080/ in your browser.
 */
fun main(args: Array<String>) {
    // detect all entities. workaround for Hibernate not able to detect entities outside of jar files
    // https://forum.hibernate.org/viewtopic.php?f=1&t=1043948&e=0
    val entities = mutableListOf<Class<*>>()
    AnnotationDetector(object : AnnotationDetector.TypeReporter {
        override fun reportTypeAnnotation(annotation: Class<out Annotation>?, className: String?) {
            entities.add(Class.forName(className))
        }

        override fun annotations(): Array<out Class<out Annotation>> = arrayOf(Entity::class.java)
    }).detect("com.github")   // added a package name for the detector to be faster; you can just use detect() to scan the whole classpath
    VaadinOnKotlin.entityManagerFactory = Persistence.createEntityManagerFactory("sample", mapOf(AvailableSettings.LOADED_CLASSES to entities))

    val server = Server(8080)
    val context = WebAppContext("src/main/webapp", "/")
    context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", ".*/classes/.*")
    context.setConfigurations(arrayOf<Configuration>(AnnotationConfiguration(), WebInfConfiguration(), WebXmlConfiguration(), MetaInfConfiguration(), FragmentConfiguration(), EnvConfiguration(), PlusConfiguration(), JettyWebXmlConfiguration()))
    server.handler = context
    server.start()
    server.join()
}
