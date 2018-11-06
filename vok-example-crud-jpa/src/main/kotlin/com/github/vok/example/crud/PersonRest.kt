package com.github.vok.example.crud

import com.github.vok.example.crud.personeditor.Person
import com.github.vok.framework.db
import com.github.vok.framework.findAll
import com.github.vok.rest.configureToJavalin
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
