package eu.vaadinonkotlin

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.expect
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AsyncExecutorTest {
    @BeforeEach fun setupVok() { VaadinOnKotlin.init() }
    @AfterEach fun tearDownVok() { VaadinOnKotlin.destroy() }

    @Nested inner class AsyncTests {
        @Test fun basic() {
            var called = false  // happens-before: async execution block is started; guaranteed-by: async()
            expect("hello" ) {
                async { called = true; "hello" } .get(100, TimeUnit.MILLISECONDS)
            }
            expect(true) {
                called // read happens-before async execution block is finished; guaranteed-by: get()
            }
        }
    }

    @Nested inner class ScheduleAtFixedRate() {
        @Test fun `schedule immediately`() {
            val called = CountDownLatch(1)
            scheduleAtFixedRate(0.milliseconds, 1.seconds) { called.countDown() }
            expect(true) { called.await(100, TimeUnit.MILLISECONDS) }
        }
    }

    @Nested inner class ScheduleAtFixedTime() {
        @Test fun `schedule immediately`() {
            val called = CountDownLatch(1)
            scheduleAtFixedTime(LocalTime.now().plus(20L, ChronoUnit.MILLIS)) { called.countDown() }
            expect(true) { called.await(100, TimeUnit.MILLISECONDS) }
        }
        @Test fun `schedule next day`() {
            val called = CountDownLatch(1)
            val future = scheduleAtFixedTime(LocalTime.now().minus(1L, ChronoUnit.MILLIS)) { called.countDown() }
            expect(true, "" + future.getDelay(TimeUnit.MILLISECONDS)) { future.getDelay(TimeUnit.HOURS) >= 23 }
            expect(false) { called.await(100, TimeUnit.MILLISECONDS) }
        }
    }
}
