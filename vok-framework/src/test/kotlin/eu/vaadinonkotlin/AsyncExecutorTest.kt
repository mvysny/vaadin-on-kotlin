package eu.vaadinonkotlin

import com.github.mvysny.dynatest.DynaTest
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.expect
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AsyncExecutorTest : DynaTest({
    beforeEach { VaadinOnKotlin.init() }
    afterEach { VaadinOnKotlin.destroy() }

    group("async") {
        test("basic") {
            var called = false  // happens-before: async execution block is started; guaranteed-by: async()
            expect("hello" ) {
                async { called = true; "hello" } .get(100, TimeUnit.MILLISECONDS)
            }
            expect(true) {
                called // read happens-before async execution block is finished; guaranteed-by: get()
            }
        }
    }

    group("scheduleAtFixedRate") {
        test("schedule immediately") {
            val called = CountDownLatch(1)
            scheduleAtFixedRate(0.milliseconds, 1.seconds) { called.countDown() }
            expect(true) { called.await(100, TimeUnit.MILLISECONDS) }
        }
    }

    group("scheduleAtFixedTime") {
        test("schedule immediately") {
            val called = CountDownLatch(1)
            scheduleAtFixedTime(LocalTime.now().plus(20L, ChronoUnit.MILLIS)) {called.countDown() }
            expect(true) { called.await(100, TimeUnit.MILLISECONDS) }
        }
    }
})
