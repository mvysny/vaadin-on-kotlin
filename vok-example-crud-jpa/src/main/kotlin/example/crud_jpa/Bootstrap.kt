package example.crud_jpa

import eu.vaadinonkotlin.VaadinOnKotlin
import eu.vaadinonkotlin.vaadin8.jpa.entityManagerFactory
import eu.vaadinonkotlin.vaadin8.jpa.getDataSource
import com.vaadin.annotations.VaadinServletConfiguration
import com.vaadin.server.VaadinServlet
import org.atmosphere.util.annotation.AnnotationDetector
import org.flywaydb.core.Flyway
import org.hibernate.jpa.AvailableSettings
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import javax.persistence.Entity
import javax.persistence.Persistence
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener
import javax.servlet.annotation.WebServlet

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
        discoverJpaEntities()
        log.info("Initializing VaadinOnKotlin")
        VaadinOnKotlin.init()
        log.info("Running DB migrations")
        val flyway = Flyway.configure()
            .dataSource(VaadinOnKotlin.getDataSource())
            .load()
        flyway.migrate()
        log.info("Initialization complete")
    }

    private fun discoverJpaEntities() {
        // detect all entities. workaround for Hibernate not able to detect entities outside of jar files
        // https://forum.hibernate.org/viewtopic.php?f=1&t=1043948&e=0
        val entities = mutableListOf<Class<*>>()
        AnnotationDetector(object : AnnotationDetector.TypeReporter {
            override fun reportTypeAnnotation(annotation: Class<out Annotation>?, className: String?) {
                entities.add(Class.forName(className))
            }

            override fun annotations(): Array<out Class<out Annotation>> = arrayOf(Entity::class.java)
        }).detect("com.github")   // added a package name for the detector to be faster; you can just use detect() to scan the whole classpath
        println("Auto-detected JPA entities: ${entities.map { it.simpleName }}")
        VaadinOnKotlin.entityManagerFactory = Persistence.createEntityManagerFactory("sample", mapOf(AvailableSettings.LOADED_CLASSES to entities))
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
