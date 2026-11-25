package be.sgl.backend.people.domain

object SiblingRelationTestMother {

    fun siblingRelation() = SiblingRelationBuilder()

    class SiblingRelationBuilder {
        private var user = UserTestMother.user().id(1).build()
        private var sibling = UserTestMother.user().id(2).firstName("siblingFirstName").build()

        fun user(user: User) = apply { this.user = user }

        fun sibling(sibling: User) = apply { this.sibling = sibling }

        fun build() = SiblingRelation(user, sibling)
    }
}
