package eu.vaadinonkotlin

import com.github.mvysny.dynatest.DynaTest
import kotlin.test.expect

class MediaTypeTest : DynaTest({
    test("toString()") {
        expect("application/json") { MediaType.json.toString() }
        expect("application/json; charset=utf-8") { MediaType.json.charsetUtf8().toString() }
    }
})
