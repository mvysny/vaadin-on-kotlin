package com.github.vok.framework.vaadin

import org.junit.Test
import kotlin.test.expect

/**
 * @author mavi
 */
class AutoViewProviderTest {
    @Test
    fun testParseViewName() {
        expect("foo") { autoViewProvider.parseViewName("!foo/25") }
        expect("foo") { autoViewProvider.parseViewName("foo") }
        expect("foo") { autoViewProvider.parseViewName("foo/") }
    }
}