package example.crudflow

import eu.vaadinonkotlin.rest.*
import example.crudflow.person.Person
import io.javalin.Javalin
import io.javalin.http.servlet.JavalinServlet
import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

/**
 * Provides access to person list. To test, just run `curl http://localhost:8080/rest/person`
 */
@WebServlet(urlPatterns = ["/rest/*"], name = "JavalinRestServlet", asyncSupported = false)
class JavalinRestServlet : HttpServlet() {
    val javalin: JavalinServlet = Javalin.createStandalone { it.gsonMapper(VokRest.gson) } .apply {
        get("/rest/person/helloworld") { ctx -> ctx.result("Hello World") }
        crud2("/rest/person", Person.getCrudHandler(true))
    }.javalinServlet()

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        javalin.service(req, resp)
    }
}
