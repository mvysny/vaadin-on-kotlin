package com.example.pokusy

import com.example.pokusy.kotlinee.dataSource
import com.example.pokusy.kotlinee.kotlineeDestroy
import com.example.pokusy.kotlinee.kotlineeInit
import com.vaadin.annotations.VaadinServletConfiguration
import com.vaadin.server.VaadinServlet
import org.flywaydb.core.Flyway
import org.glassfish.jersey.servlet.ServletContainer
import org.slf4j.LoggerFactory
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebInitParam
import javax.servlet.annotation.WebListener
import javax.servlet.annotation.WebServlet

/**
 * Boots the app:
 *
 * * Makes sure that the database is up-to-date, by running migration scripts with Flyway. This will work even in cluster as Flyway
 *   automatically obtains a cluster-wide database lock.
 * * Initializes the KotlinEE framework.
 *
 * @author mvy
 */
@WebListener
class Bootstrap: ServletContextListener {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun contextInitialized(sce: ServletContextEvent?) {
        log.info("Starting up")
        log.info("Running DB migrations")
        val flyway = Flyway()
        flyway.dataSource = dataSource
        flyway.migrate()
        log.info("Initializing KotlinEE")
        kotlineeInit()
        log.info("Initialization complete")
    }

    override fun contextDestroyed(sce: ServletContextEvent?) {
        log.info("Shutting down");
        log.info("Destroying KotlinEE")
        kotlineeDestroy()
        log.info("Shutdown complete")
    }
}

@WebServlet(urlPatterns = arrayOf("/*"), name = "MyUIServlet", asyncSupported = true)
@VaadinServletConfiguration(ui = MyUI::class, productionMode = false)
class MyUIServlet : VaadinServlet() { }

@WebServlet(urlPatterns = arrayOf("/rest/*"), name = "JerseyServlet", asyncSupported = true,
        initParams = arrayOf(WebInitParam(name = "jersey.config.server.provider.packages", value = "com.example.pokusy")))
class JerseyServlet : ServletContainer() { }
