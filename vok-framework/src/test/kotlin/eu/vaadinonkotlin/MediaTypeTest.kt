package eu.vaadinonkotlin

import org.junit.jupiter.api.Test
import kotlin.test.expect

class MediaTypeTest {
    @Test fun toStringTest() {
        expect("application/json") { MediaType.json.toString() }
        expect("application/json; charset=utf-8") { MediaType.json.charsetUtf8().toString() }
    }
}
