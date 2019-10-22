package example.crud

import example.crud.personeditor.Person
import eu.vaadinonkotlin.rest.configureToJavalin
import eu.vaadinonkotlin.rest.crud2
import eu.vaadinonkotlin.rest.getCrudHandler
import com.google.gson.GsonBuilder
import io.javalin.Javalin
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Provides access to person list. To test, just run `curl http://localhost:8080/rest/person`
 */
@WebServlet(urlPatterns = ["/rest/*"], name = "JavalinRestServlet", asyncSupported = false)
class JavalinRestServlet : HttpServlet() {
    val javalin = Javalin.createStandalone()
            .configureRest()
            .servlet()

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        javalin.service(req, resp)
    }
}

fun Javalin.configureRest(): Javalin {
    val gson = GsonBuilder().create()
    gson.configureToJavalin()
    get("/rest/person/helloworld") { ctx -> ctx.result("Hello World") }
    crud2("/rest/person", Person.getCrudHandler(true))
    return this
}
