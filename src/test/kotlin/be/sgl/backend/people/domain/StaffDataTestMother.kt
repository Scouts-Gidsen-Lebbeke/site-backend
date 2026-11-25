package be.sgl.backend.people.domain

object StaffDataTestMother {

    fun staffData() = StaffDataBuilder()

    class StaffDataBuilder {
        private var totem: String? = null
        private var about: String? = null
        private val nicknames = mutableMapOf<Int, String>()

        fun totem(totem: String) = apply { this.totem = totem }

        fun about(about: String) = apply { this.about = about }

        fun nickname(branchId: Int, nickname: String) = apply { this.nicknames[branchId] = nickname }

        fun build(): StaffData {
            val staffData = StaffData()
            staffData.totem = totem
            staffData.about = about
            staffData.nicknames = nicknames.toMutableMap()
            return staffData
        }
    }
}
