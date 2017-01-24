package com.github.vok.example.crud

import org.eclipse.jetty.annotations.AnnotationConfiguration
import org.eclipse.jetty.plus.webapp.EnvConfiguration
import org.eclipse.jetty.plus.webapp.PlusConfiguration
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.*


/**
 * Runs the testing Jetty-based server. Just open http://localhost:8080/ in your browser.
 */
fun main(args: Array<String>) {
    val server = Server(8080)
    val context = WebAppContext("src/main/webapp", "/")
    context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", ".*/classes/.*")
    context.setConfigurations(arrayOf<Configuration>(AnnotationConfiguration(), WebInfConfiguration(), WebXmlConfiguration(), MetaInfConfiguration(), FragmentConfiguration(), EnvConfiguration(), PlusConfiguration(), JettyWebXmlConfiguration()))
    server.handler = context
    server.start()
    server.join()
    // @todo note that when you click the "CRUD Demo" tab then you'll receive an exception: SEVERE:
    // java.lang.IllegalArgumentException: Not an entity: class com.github.vok.example.crud.personeditor.Person
    // I have asked for help at Hibernate forums, we'll see.
}
