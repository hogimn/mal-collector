package test.hogimn.malcollector.circuitbreaker

import com.hogimn.malcollector.circuitbreakersupport.CircuitBreaker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CircuitBreakerTest {
    @Test
    fun testClosed() {
        val circuitBreaker = CircuitBreaker(100)

        val actual = circuitBreaker.withCircuitBreaker(
            { Thread.sleep(10); true }, { false })
        assertTrue(actual)
    }

    @Test
    fun testTimeout() {
        val circuitBreaker = CircuitBreaker(10)

        val actual = circuitBreaker.withCircuitBreaker(
            { Thread.sleep(100); true }, { false })
        assertFalse(actual)
    }

    @Test
    fun testMaxFailures() {
        val circuitBreaker = CircuitBreaker(0, 3)

        for (i in 1..3) {
            val actual = circuitBreaker.withCircuitBreaker(
                { Thread.sleep(100); true }, { false })
            assertFalse(actual)
        }

        val actual = circuitBreaker.withCircuitBreaker(
            { fail(); Thread.sleep(0); true }, { false })
        assertFalse(actual)
    }

    @Test
    fun testReset() {
        val circuitBreaker = CircuitBreaker(10, 3, 300)

        for (i in 1..3) {
            val actual = circuitBreaker.withCircuitBreaker(
                { Thread.sleep(100); true }, { false })
            assertFalse(actual)
        }

        Thread.sleep(400)

        val actual = circuitBreaker.withCircuitBreaker(
            { Thread.sleep(0); true }, { false })
        assertTrue(actual)
    }
}
