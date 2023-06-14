package eu.vaadinonkotlin

import java.io.Serializable

public data class MediaType(
    /**
     * Returns the high-level media type, such as "text", "image", "audio", "video", or "application".
     */
    val type: String,

    /**
     * Returns a specific media subtype, such as "plain" or "png", "mpeg", "mp4" or "xml".
     */
    val subtype: String,

    /**
     * Parameter names with their values, like `["charset" to "utf-8"]`.
     */
    private val parameterNamesAndValues: List<Pair<String, String>> = listOf()
) : Serializable {
    public fun charset(charset: String): MediaType = copy(parameterNamesAndValues = parameterNamesAndValues + listOf("charset" to charset))
    public fun charsetUtf8(): MediaType = charset("utf-8")

    override fun toString(): String = buildString {
        append(type)
        append('/')
        append(subtype)
        if (parameterNamesAndValues.isNotEmpty()) {
            append(" ;")
            parameterNamesAndValues.forEach { append(it.first).append('=').append(it.second) }
        }
    }

    public companion object {
        public val json: MediaType = MediaType("application", "json")
        public val jsonUtf8: MediaType = json.charsetUtf8()
        public val xml: MediaType = MediaType("application", "xml")
        public val html: MediaType = MediaType("text", "html")
    }
}
