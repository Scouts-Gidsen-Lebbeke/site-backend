package be.sgl.backend.entity.branch

import be.sgl.backend.entity.user.Sex

object BranchTestMother {

    fun branch() = BranchBuilder()

    class BranchBuilder {

        private var id: Int? = 1
        private var name = "branchName"
        private var email = "branch@group.com"
        private var minimumAge = 0
        private var maximumAge: Int? = null
        private var sex = Sex.MALE
        private var description = "branchDescription"
        private var law: String? = null
        private var image = "branch.jpg"
        private var status = BranchStatus.ACTIVE
        private var staffTitle: String? = null

        fun id(id: Int) = apply { this.id = id }

        fun name(name: String) = apply { this.name = name }

        fun email(email: String) = apply { this.email = email }

        fun minimumAge(minimumAge: Int) = apply { this.minimumAge = minimumAge }

        fun maximumAge(maximumAge: Int) = apply { this.maximumAge = maximumAge }

        fun sex(sex: Sex) = apply { this.sex = sex }

        fun description(description: String) = apply { this.description = description }

        fun law(law: String) = apply { this.law = law }

        fun image(image: String) = apply { this.image = image }

        fun active() = apply { this.status = BranchStatus.ACTIVE }

        fun member() = apply { this.status = BranchStatus.MEMBER }

        fun passive() = apply { this.status = BranchStatus.PASSIVE }

        fun hidden() = apply { this.status = BranchStatus.HIDDEN }

        fun staffTitle(staffTitle: String) = apply { this.staffTitle = staffTitle }

        fun build(): Branch {
            val branch = Branch()
            branch.id = id
            branch.name = name
            branch.email = email
            branch.minimumAge = minimumAge
            branch.maximumAge = maximumAge
            branch.sex = sex
            branch.description = description
            branch.law = law
            branch.image = image
            branch.status = status
            branch.staffTitle = staffTitle
            return branch
        }
    }
}