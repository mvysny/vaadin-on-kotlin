package example.crudflow

import eu.vaadinonkotlin.rest.*
import example.crudflow.person.Persons
import io.javalin.Javalin
import jakarta.servlet.Servlet
import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

/**
 * Provides access to person list. To test, just run `curl http://localhost:8080/rest/person`
 */
@WebServlet(urlPatterns = ["/rest/*"], name = "JavalinRestServlet", asyncSupported = false)
class JavalinRestServlet : HttpServlet() {
    val javalin: Servlet = Javalin.create {
        // Javalin only serves through this servlet; its own Jetty never starts.
        it.startup.startupWatcherEnabled = false
        it.gsonMapper(VokRest.gson)
        it.routes.get("/rest/person/helloworld") { ctx -> ctx.result("Hello World") }
        it.routes.crud2("/rest/person", Persons.getCrudHandler(allowModification = true))
    }.javalinServlet()

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        javalin.service(req, resp)
    }
}
