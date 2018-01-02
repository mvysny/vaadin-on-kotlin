package com.vaadin.starter.beveragebuddy

import com.github.vok.framework.VaadinOnKotlin
import com.github.vok.framework.sql2o.dataSource
import com.github.vok.framework.sql2o.dataSourceConfig
import com.vaadin.flow.server.ServiceInitEvent
import com.vaadin.flow.server.VaadinServiceInitListener
import com.vaadin.starter.beveragebuddy.backend.StaticData
import org.flywaydb.core.Flyway
import org.h2.Driver
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener
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
        VaadinOnKotlin.dataSourceConfig.apply {
            driverClassName = Driver::class.java.name
            jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
            username = "sa"
            password = ""
        }
        log.info("Initializing VaadinOnKotlin")
        VaadinOnKotlin.init()
        log.info("Running DB migrations")
        val flyway = Flyway()
        flyway.dataSource = VaadinOnKotlin.dataSource
        flyway.migrate()
        log.info("Populating database with testing data")
        StaticData.createTestingData()
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

class ApplicationServiceInitListener : VaadinServiceInitListener {

    override fun serviceInit(event: ServiceInitEvent) {
        event.addBootstrapListener { response ->
            val document = response.document
            document.head().apply {
                meta("viewport", "width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes")
                meta("apple-mobile-web-app-capable", "yes")
                meta("apple-mobile-web-app-status-bar-style", "black")
            }
        }
    }

    private fun Element.meta(name: String, content: String) {
        val meta = ownerDocument().createElement("meta").apply {
            attr("name", name)
            attr("content", content)
        }
        appendChild(meta)
    }
}

/**
 * RESTEasy configuration. Do not use Jersey, it has a tons of dependencies
 */
@ApplicationPath("/rest")
class ApplicationConfig : Application()
