package be.sgl.backend.entity

import be.sgl.backend.entity.registrable.Registrable
import be.sgl.backend.entity.registrable.RegistrableStatus
import be.sgl.backend.entity.registrable.RegistrableStatus.Companion.getStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class RegistrableStatusTest {

    private class TestRegistrable : Registrable() {
        fun setCancelled(value: Boolean) {
            this.cancelled = value
        }

        fun setOpenTime(time: LocalDateTime) {
            this.open = time
        }

        fun setClosedTime(time: LocalDateTime) {
            this.closed = time
        }

        fun setStartTime(time: LocalDateTime) {
            this.start = time
        }

        fun setEndTime(time: LocalDateTime) {
            this.end = time
        }
    }

    @Test
    fun `RegistrableStatus should have all expected values`() {
        val values = RegistrableStatus.values()
        assertEquals(6, values.size)
        assertTrue(values.contains(RegistrableStatus.NOT_YET_OPEN))
        assertTrue(values.contains(RegistrableStatus.REGISTRATIONS_OPENED))
        assertTrue(values.contains(RegistrableStatus.REGISTRATIONS_COMPLETED))
        assertTrue(values.contains(RegistrableStatus.STARTED))
        assertTrue(values.contains(RegistrableStatus.COMPLETED))
        assertTrue(values.contains(RegistrableStatus.CANCELLED))
    }

    @Test
    fun `getStatus should return CANCELLED when registrable is cancelled`() {
        val registrable = TestRegistrable()
        registrable.setCancelled(true)

        val status = registrable.getStatus()

        assertEquals(RegistrableStatus.CANCELLED, status)
    }

    @Test
    fun `getStatus should return NOT_YET_OPEN when before open time`() {
        val now = LocalDateTime.now()
        val registrable = TestRegistrable().apply {
            setCancelled(false)
            setOpenTime(now.plusDays(1))
            setClosedTime(now.plusDays(2))
            setStartTime(now.plusDays(3))
            setEndTime(now.plusDays(4))
        }

        val status = registrable.getStatus()

        assertEquals(RegistrableStatus.NOT_YET_OPEN, status)
    }

    @Test
    fun `getStatus should return REGISTRATIONS_OPENED when between open and closed`() {
        val now = LocalDateTime.now()
        val registrable = TestRegistrable().apply {
            setCancelled(false)
            setOpenTime(now.minusDays(1))
            setClosedTime(now.plusDays(1))
            setStartTime(now.plusDays(2))
            setEndTime(now.plusDays(3))
        }

        val status = registrable.getStatus()

        assertEquals(RegistrableStatus.REGISTRATIONS_OPENED, status)
    }

    @Test
    fun `getStatus should return REGISTRATIONS_COMPLETED when between closed and start`() {
        val now = LocalDateTime.now()
        val registrable = TestRegistrable().apply {
            setCancelled(false)
            setOpenTime(now.minusDays(3))
            setClosedTime(now.minusDays(2))
            setStartTime(now.plusDays(1))
            setEndTime(now.plusDays(2))
        }

        val status = registrable.getStatus()

        assertEquals(RegistrableStatus.REGISTRATIONS_COMPLETED, status)
    }

    @Test
    fun `getStatus should return STARTED when between start and end`() {
        val now = LocalDateTime.now()
        val registrable = TestRegistrable().apply {
            setCancelled(false)
            setOpenTime(now.minusDays(4))
            setClosedTime(now.minusDays(3))
            setStartTime(now.minusDays(2))
            setEndTime(now.plusDays(1))
        }

        val status = registrable.getStatus()

        assertEquals(RegistrableStatus.STARTED, status)
    }

    @Test
    fun `getStatus should return COMPLETED when after end time`() {
        val now = LocalDateTime.now()
        val registrable = TestRegistrable().apply {
            setCancelled(false)
            setOpenTime(now.minusDays(5))
            setClosedTime(now.minusDays(4))
            setStartTime(now.minusDays(3))
            setEndTime(now.minusDays(2))
        }

        val status = registrable.getStatus()

        assertEquals(RegistrableStatus.COMPLETED, status)
    }

    @Test
    fun `getStatus should prioritize CANCELLED over other statuses`() {
        val now = LocalDateTime.now()
        val registrable = TestRegistrable().apply {
            setCancelled(true)
            setOpenTime(now.minusDays(1))
            setClosedTime(now.plusDays(1))
            setStartTime(now.plusDays(2))
            setEndTime(now.plusDays(3))
        }

        val status = registrable.getStatus()

        assertEquals(RegistrableStatus.CANCELLED, status)
    }

    @Test
    fun `RegistrableStatus valueOf should return correct status`() {
        assertEquals(RegistrableStatus.NOT_YET_OPEN, RegistrableStatus.valueOf("NOT_YET_OPEN"))
        assertEquals(RegistrableStatus.REGISTRATIONS_OPENED, RegistrableStatus.valueOf("REGISTRATIONS_OPENED"))
        assertEquals(RegistrableStatus.REGISTRATIONS_COMPLETED, RegistrableStatus.valueOf("REGISTRATIONS_COMPLETED"))
        assertEquals(RegistrableStatus.STARTED, RegistrableStatus.valueOf("STARTED"))
        assertEquals(RegistrableStatus.COMPLETED, RegistrableStatus.valueOf("COMPLETED"))
        assertEquals(RegistrableStatus.CANCELLED, RegistrableStatus.valueOf("CANCELLED"))
    }
}
