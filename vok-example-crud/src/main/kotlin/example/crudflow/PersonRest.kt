package example.crudflow

import example.crudflow.person.Person
import eu.vaadinonkotlin.rest.configureToJavalin
import eu.vaadinonkotlin.rest.crud2
import eu.vaadinonkotlin.rest.getCrudHandler
import eu.vaadinonkotlin.rest.VokRest
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
            .servlet()

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        javalin.service(req, resp)
    }
}

fun Javalin.configureRest(): Javalin {
    VokRest.gson.configureToJavalin()
    get("/rest/person/helloworld") { ctx -> ctx.result("Hello World") }
    crud2("/rest/person", Person.getCrudHandler(true))
    return this
}
