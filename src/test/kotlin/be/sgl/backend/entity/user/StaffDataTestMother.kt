package be.sgl.backend.entity.user

import be.sgl.backend.organization.domain.Branch

object StaffDataTestMother {

    fun staffData() = StaffDataBuilder()

    class StaffDataBuilder {
        private var totem: String? = null
        private var about: String? = null
        private val nicknames = mutableMapOf<Branch, String>()

        fun totem(totem: String) = apply { this.totem = totem }

        fun about(about: String) = apply { this.about = about }

        fun nickname(branch: Branch, nickname: String) = apply { this.nicknames[branch] = nickname }

        fun build(): StaffData {
            val staffData = StaffData()
            staffData.totem = totem
            staffData.about = about
            staffData.nicknames = nicknames.toMutableMap()
            return staffData
        }
    }
}
