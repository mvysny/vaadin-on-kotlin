package com.github.vok.rest

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Type
import javax.ws.rs.Consumes
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.ext.MessageBodyReader
import javax.ws.rs.ext.MessageBodyWriter
import javax.ws.rs.ext.Provider

var gsonProvider: ()->Gson = { GsonBuilder().create() }

@Provider
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class GsonMessageBodyHandler : MessageBodyWriter<Any>, MessageBodyReader<Any> {
    private val gson: Gson by lazy { gsonProvider() }

    override fun isReadable(type: Class<*>, genericType: Type,
                            annotations: Array<Annotation>, mediaType: MediaType) = true

    override fun readFrom(type: Class<Any>, genericType: Type, annotations: Array<Annotation>, mediaType: MediaType, httpHeaders: MultivaluedMap<String, String>, entityStream: InputStream): Any {
        return entityStream.reader().use {
            gson.fromJson(it, genericType)
        }
    }

    override fun isWriteable(type: Class<*>, genericType: Type, annotations: Array<Annotation>, mediaType: MediaType) =
            true

    override fun getSize(`object`: Any, type: Class<*>, genericType: Type, annotations: Array<Annotation>, mediaType: MediaType): Long =
            -1

    override fun writeTo(`object`: Any, type: Class<*>, genericType: Type, annotations: Array<Annotation>, mediaType: MediaType, httpHeaders: MultivaluedMap<String, Any>, entityStream: OutputStream) {
        entityStream.writer().use {
            gson.toJson(`object`, genericType, it)
        }
    }
}
