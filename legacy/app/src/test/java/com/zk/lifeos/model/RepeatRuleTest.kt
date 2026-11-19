package com.zk.lifeos.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * The repeat rules decide when a task comes back, which means a wrong answer here quietly puts a
 * real obligation on the wrong day. All four are pure functions of a date, so they are cheap to pin
 * down exactly — the clamping cases especially, which are the ones nobody tries by hand.
 */
class RepeatRuleTest {

    @Test
    fun `each rule steps by its own period`() {
        val from = LocalDate.of(2026, 8, 14)
        assertEquals(LocalDate.of(2026, 8, 15), RepeatRule.DAILY.next(from))
        assertEquals(LocalDate.of(2026, 8, 21), RepeatRule.WEEKLY.next(from))
        assertEquals(LocalDate.of(2026, 9, 14), RepeatRule.MONTHLY.next(from))
        assertEquals(LocalDate.of(2027, 8, 14), RepeatRule.YEARLY.next(from))
    }

    @Test
    fun `monthly clamps instead of overflowing into the next month`() {
        // The 31st of January is not the 3rd of March. Clamping is what 「每月」 is expected to mean,
        // and it also means a monthly task can never skip a month entirely.
        assertEquals(LocalDate.of(2026, 2, 28), RepeatRule.MONTHLY.next(LocalDate.of(2026, 1, 31)))
        assertEquals(LocalDate.of(2028, 2, 29), RepeatRule.MONTHLY.next(LocalDate.of(2028, 1, 31)))
        assertEquals(LocalDate.of(2026, 4, 30), RepeatRule.MONTHLY.next(LocalDate.of(2026, 3, 31)))
    }

    @Test
    fun `yearly clamps a leap day onto the 28th`() {
        assertEquals(LocalDate.of(2029, 2, 28), RepeatRule.YEARLY.next(LocalDate.of(2028, 2, 29)))
    }

    @Test
    fun `yearly crossing a leap day is still one calendar year`() {
        // 366 days apart, but the same date — which is the thing 车险续费 actually means.
        assertEquals(LocalDate.of(2028, 8, 14), RepeatRule.YEARLY.next(LocalDate.of(2027, 8, 14)))
    }

    @Test
    fun `an unknown stored value degrades to no repeat rather than throwing`() {
        // A backup written by a newer build can be imported into an older one; it must not explode.
        assertNull(RepeatRule.fromStored("FORTNIGHTLY"))
        assertNull(RepeatRule.fromStored(null))
        assertEquals(RepeatRule.YEARLY, RepeatRule.fromStored("YEARLY"))
    }
}
