package eu.vaadinonkotlin

import com.github.mvysny.dynatest.DynaTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.expect

class ServicesTest : DynaTest({
    beforeEach { VaadinOnKotlin.init() }
    afterEach { VaadinOnKotlin.destroy() }

    test("api") {
        expect("Foo!") { Services.myStatelessService.foo() }
        expect(1) { Services.mySingletonService.getNewId() }
    }

    test("singleton") {
        expect(1) { Services.mySingletonService.getNewId() }
        expect(2) { Services.mySingletonService.getNewId() }
        expect(3) { Services.mySingletonService.getNewId() }
    }
})

class MyStatelessService internal constructor() {
    fun foo() = "Foo!"
}

class MySingletonService internal constructor() {
    private val idgenerator = AtomicInteger(0)
    fun getNewId() = idgenerator.incrementAndGet()
}

val Services.myStatelessService: MyStatelessService get() = MyStatelessService()
val Services.mySingletonService: MySingletonService get() = singletons.getOrCreate { MySingletonService() }
