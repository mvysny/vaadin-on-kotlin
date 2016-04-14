package com.example.pokusy

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/**
 * Root resource (exposed at "myresource" path). To test, just run `wget http://localhost:8080/rest/myresource`
 */
@Path("myresource")
class MyResourceRest {

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun getIt() = "Got it!"
}
