package com.github.vok.example.crud

import com.github.vok.framework.VaadinOnKotlin
import com.github.vok.framework.getDataSource
import com.vaadin.annotations.VaadinServletConfiguration
import com.vaadin.server.Constants
import com.vaadin.server.VaadinServlet
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import javax.servlet.ServletConfig
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener
import javax.servlet.annotation.WebServlet
import javax.ws.rs.ApplicationPath
import javax.ws.rs.core.Application

/**
 * Boots the app:
 *
 * * Makes sure that the database is up-to-date, by running migration scripts with Flyway. This will work even in cluster as Flyway
 *   automatically obtains a cluster-wide database lock.
 * * Initializes the VaadinOnKotlin framework.
 * * Maps Vaadin to `/`, maps REST server to `/rest`
 * @author mvy
 */
@WebListener
class Bootstrap: ServletContextListener {
    override fun contextInitialized(sce: ServletContextEvent?) {
        log.info("Starting up")
        log.info("Initializing VaadinOnKotlin")
        VaadinOnKotlin.init()
        log.info("Running DB migrations")
        val flyway = Flyway()
        flyway.dataSource = VaadinOnKotlin.getDataSource()
        flyway.migrate()
        log.info("Initialization complete")
    }

    override fun contextDestroyed(sce: ServletContextEvent?) {
        log.info("Shutting down");
        log.info("Destroying VaadinOnKotlin")
        VaadinOnKotlin.destroy()
        log.info("Shutdown complete")
    }

    companion object {
        private val log = LoggerFactory.getLogger(Bootstrap::class.java)

        init {
            // let java.util.logging log to slf4j
            SLF4JBridgeHandler.removeHandlersForRootLogger()
            SLF4JBridgeHandler.install()
        }
    }
}

@WebServlet(urlPatterns = arrayOf("/*"), name = "MyUIServlet", asyncSupported = true)
@VaadinServletConfiguration(ui = MyUI::class, productionMode = false)
class MyUIServlet : VaadinServlet()

/**
 * RESTEasy configuration. Do not use Jersey, it has a tons of dependencies
 */
@ApplicationPath("/rest")
class ApplicationConfig : Application()
