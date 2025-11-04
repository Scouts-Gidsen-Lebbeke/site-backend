package be.sgl.backend.entity

import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.user.BloodGroup
import be.sgl.backend.entity.user.RoleLevel
import be.sgl.backend.entity.user.Sex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EnumTest {

    // BranchStatus tests
    @Test
    fun `BranchStatus should have all expected values`() {
        val values = BranchStatus.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(BranchStatus.ACTIVE))
        assertTrue(values.contains(BranchStatus.MEMBER))
        assertTrue(values.contains(BranchStatus.PASSIVE))
        assertTrue(values.contains(BranchStatus.HIDDEN))
    }

    @Test
    fun `BranchStatus valueOf should return correct status`() {
        assertEquals(BranchStatus.ACTIVE, BranchStatus.valueOf("ACTIVE"))
        assertEquals(BranchStatus.MEMBER, BranchStatus.valueOf("MEMBER"))
        assertEquals(BranchStatus.PASSIVE, BranchStatus.valueOf("PASSIVE"))
        assertEquals(BranchStatus.HIDDEN, BranchStatus.valueOf("HIDDEN"))
    }

    @Test
    fun `BranchStatus ordinal should match declaration order`() {
        assertEquals(0, BranchStatus.ACTIVE.ordinal)
        assertEquals(1, BranchStatus.MEMBER.ordinal)
        assertEquals(2, BranchStatus.PASSIVE.ordinal)
        assertEquals(3, BranchStatus.HIDDEN.ordinal)
    }

    // RoleLevel tests
    @Test
    fun `RoleLevel should have all expected values`() {
        val values = RoleLevel.values()
        assertEquals(5, values.size)
        assertTrue(values.contains(RoleLevel.GUEST))
        assertTrue(values.contains(RoleLevel.SCOUT))
        assertTrue(values.contains(RoleLevel.MEMBER))
        assertTrue(values.contains(RoleLevel.STAFF))
        assertTrue(values.contains(RoleLevel.ADMIN))
    }

    @Test
    fun `RoleLevel valueOf should return correct level`() {
        assertEquals(RoleLevel.GUEST, RoleLevel.valueOf("GUEST"))
        assertEquals(RoleLevel.SCOUT, RoleLevel.valueOf("SCOUT"))
        assertEquals(RoleLevel.MEMBER, RoleLevel.valueOf("MEMBER"))
        assertEquals(RoleLevel.STAFF, RoleLevel.valueOf("STAFF"))
        assertEquals(RoleLevel.ADMIN, RoleLevel.valueOf("ADMIN"))
    }

    @Test
    fun `RoleLevel ordinal should match declaration order`() {
        assertEquals(0, RoleLevel.GUEST.ordinal)
        assertEquals(1, RoleLevel.SCOUT.ordinal)
        assertEquals(2, RoleLevel.MEMBER.ordinal)
        assertEquals(3, RoleLevel.STAFF.ordinal)
        assertEquals(4, RoleLevel.ADMIN.ordinal)
    }

    // Sex tests
    @Test
    fun `Sex should have all expected values`() {
        val values = Sex.values()
        assertEquals(3, values.size)
        assertTrue(values.contains(Sex.MALE))
        assertTrue(values.contains(Sex.FEMALE))
        assertTrue(values.contains(Sex.UNKNOWN))
    }

    @Test
    fun `Sex valueOf should return correct sex`() {
        assertEquals(Sex.MALE, Sex.valueOf("MALE"))
        assertEquals(Sex.FEMALE, Sex.valueOf("FEMALE"))
        assertEquals(Sex.UNKNOWN, Sex.valueOf("UNKNOWN"))
    }

    @Test
    fun `Sex ordinal should match declaration order`() {
        assertEquals(0, Sex.MALE.ordinal)
        assertEquals(1, Sex.FEMALE.ordinal)
        assertEquals(2, Sex.UNKNOWN.ordinal)
    }

    // BloodGroup tests
    @Test
    fun `BloodGroup should have all expected values`() {
        val values = BloodGroup.values()
        assertEquals(9, values.size)
        assertTrue(values.contains(BloodGroup.AP))
        assertTrue(values.contains(BloodGroup.AN))
        assertTrue(values.contains(BloodGroup.BP))
        assertTrue(values.contains(BloodGroup.BN))
        assertTrue(values.contains(BloodGroup.ABP))
        assertTrue(values.contains(BloodGroup.ABN))
        assertTrue(values.contains(BloodGroup.OP))
        assertTrue(values.contains(BloodGroup.ON))
        assertTrue(values.contains(BloodGroup.UNKNOWN))
    }

    @Test
    fun `BloodGroup valueOf should return correct blood group`() {
        assertEquals(BloodGroup.AP, BloodGroup.valueOf("AP"))
        assertEquals(BloodGroup.AN, BloodGroup.valueOf("AN"))
        assertEquals(BloodGroup.BP, BloodGroup.valueOf("BP"))
        assertEquals(BloodGroup.BN, BloodGroup.valueOf("BN"))
        assertEquals(BloodGroup.ABP, BloodGroup.valueOf("ABP"))
        assertEquals(BloodGroup.ABN, BloodGroup.valueOf("ABN"))
        assertEquals(BloodGroup.OP, BloodGroup.valueOf("OP"))
        assertEquals(BloodGroup.ON, BloodGroup.valueOf("ON"))
        assertEquals(BloodGroup.UNKNOWN, BloodGroup.valueOf("UNKNOWN"))
    }

    @Test
    fun `BloodGroup ordinal should match declaration order`() {
        assertEquals(0, BloodGroup.AP.ordinal)
        assertEquals(1, BloodGroup.AN.ordinal)
        assertEquals(2, BloodGroup.BP.ordinal)
        assertEquals(3, BloodGroup.BN.ordinal)
        assertEquals(4, BloodGroup.ABP.ordinal)
        assertEquals(5, BloodGroup.ABN.ordinal)
        assertEquals(6, BloodGroup.OP.ordinal)
        assertEquals(7, BloodGroup.ON.ordinal)
        assertEquals(8, BloodGroup.UNKNOWN.ordinal)
    }
}
