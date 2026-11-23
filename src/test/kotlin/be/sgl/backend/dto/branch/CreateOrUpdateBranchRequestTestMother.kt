package be.sgl.backend.dto.branch

import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.user.Sex

object CreateOrUpdateBranchRequestTestMother {

    fun branchRequest() = CreateOrUpdateBranchRequestBuilder()

    class CreateOrUpdateBranchRequestBuilder {
        private var name = "requestBranchName"
        private var image = "requestBranch.jpg"
        private var email = "request@group.com"
        private var minimumAge = 6
        private var maximumAge: Int? = 8
        private var sex: Sex? = null
        private var description: String? = "requestDescription"
        private var law: String? = "requestLaw"
        private var status = BranchStatus.ACTIVE
        private var staffTitle: String? = "requestStaffTitle"

        fun name(name: String) = apply { this.name = name }

        fun image(image: String) = apply { this.image = image }

        fun email(email: String) = apply { this.email = email }

        fun minimumAge(minimumAge: Int) = apply { this.minimumAge = minimumAge }

        fun maximumAge(maximumAge: Int?) = apply { this.maximumAge = maximumAge }

        fun sex(sex: Sex) = apply { this.sex = sex }

        fun hidden() = apply { this.status = BranchStatus.HIDDEN }

        fun build() = CreateOrUpdateBranchRequest(name, image, email, minimumAge, maximumAge, sex, description, law,
            status, staffTitle)
    }
}
