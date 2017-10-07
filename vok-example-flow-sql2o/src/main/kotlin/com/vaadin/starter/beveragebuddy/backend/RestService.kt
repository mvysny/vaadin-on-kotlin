package com.vaadin.starter.beveragebuddy.backend

import com.github.vok.framework.sql2o.findAll
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/**
 * Provides access to the database. To test, just run `curl http://localhost:8080/rest/categories`
 */
@Path("/")
class RestService {

    @GET
    @Path("/categories")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAllCategories(): List<Category> = Category.findAll()

    @GET
    @Path("/reviews")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAllReviews(): List<Review> = Review.findAll()
}
