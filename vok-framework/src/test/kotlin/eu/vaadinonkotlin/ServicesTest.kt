package eu.vaadinonkotlin

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.expect

class ServicesTest {
    @BeforeEach fun setupVok() { VaadinOnKotlin.init() }
    @AfterEach fun tearDownVok() { VaadinOnKotlin.destroy() }

    @Test fun api() {
        expect("Foo!") { Services.myStatelessService.foo() }
        expect(1) { Services.mySingletonService.getNewId() }
    }

    @Test fun singleton() {
        expect(1) { Services.mySingletonService.getNewId() }
        expect(2) { Services.mySingletonService.getNewId() }
        expect(3) { Services.mySingletonService.getNewId() }
    }
}

class MyStatelessService internal constructor() {
    fun foo() = "Foo!"
}

class MySingletonService internal constructor() {
    private val idgenerator = AtomicInteger(0)
    fun getNewId() = idgenerator.incrementAndGet()
}

val Services.myStatelessService: MyStatelessService get() = MyStatelessService()
val Services.mySingletonService: MySingletonService get() = singletons.getOrCreate(MySingletonService::class) { MySingletonService() }
