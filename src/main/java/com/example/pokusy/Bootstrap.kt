package com.example.pokusy

import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener

/**
 * @author mvy
 */
@WebListener
class Bootstrap: ServletContextListener {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun contextInitialized(sce: ServletContextEvent?) {
        log.info("Running DB migrations")
        val flyway = Flyway()
        flyway.dataSource = dataSource
        flyway.migrate()
        log.info("Initialization complete")
    }

    override fun contextDestroyed(sce: ServletContextEvent?) {
    }
}
