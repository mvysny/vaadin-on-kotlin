package example.crudflow

import eu.vaadinonkotlin.rest.*
import example.crudflow.person.Person
import io.javalin.Javalin
import io.javalin.http.JavalinServlet
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Provides access to person list. To test, just run `curl http://localhost:8080/rest/person`
 */
@WebServlet(urlPatterns = ["/rest/*"], name = "JavalinRestServlet", asyncSupported = false)
class JavalinRestServlet : HttpServlet() {
    val javalin: JavalinServlet = Javalin.createStandalone()
            .configureRest()
            .javalinServlet()

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        javalin.service(req, resp)
    }
}

fun Javalin.configureRest(): Javalin {
    gsonMapper(VokRest.gson)
    get("/rest/person/helloworld") { ctx -> ctx.result("Hello World") }
    crud2("/rest/person", Person.getCrudHandler(true))
    return this
}
