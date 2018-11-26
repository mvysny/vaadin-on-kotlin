package example.crud7

import example.crud7.personeditor.Person
import eu.vaadinonkotlin.vaadin8.jpa.db
import eu.vaadinonkotlin.vaadin8.jpa.findAll
import eu.vaadinonkotlin.rest.configureToJavalin
import com.google.gson.GsonBuilder
import io.javalin.EmbeddedJavalin
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
    val javalin = EmbeddedJavalin()
            .configureRest()
            .createServlet()

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        javalin.service(req, resp)
    }
}

fun Javalin.configureRest(): Javalin {
    val gson = GsonBuilder().create()
    gson.configureToJavalin()
    get("/rest/person/helloworld") { ctx -> ctx.result("Hello World") }
    get("/rest/person") { ctx -> ctx.json(db { em.findAll<Person>() }) }
    return this
}
