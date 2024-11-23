package be.sgl.backend.service.belcotax

import be.sgl.backend.entity.enum.OrganizationType
import be.sgl.backend.repository.ActivityRegistrationRepository
import be.sgl.backend.repository.OrganizationRepository
import be.sgl.backend.service.MailService
import be.sgl.backend.service.user.UserDataProvider
import generated.Verzendingen
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BelcotaxService {

    @Autowired
    private lateinit var userDataProvider: UserDataProvider
    @Autowired
    private lateinit var organizationRepository: OrganizationRepository
    @Autowired
    private lateinit var activityRegistrationRepository: ActivityRegistrationRepository
    @Autowired
    private lateinit var dispatchService: DispatchService
    @Autowired
    private lateinit var formService: FormService
    @Autowired
    private lateinit var mailService: MailService

    fun getDispatchForFiscalYearAndRate(fiscalYear: Int?, rate: Double?): Verzendingen {
        val owner = organizationRepository.getByType(OrganizationType.OWNER)
        val certifier = organizationRepository.getByType(OrganizationType.CERTIFIER)
        val activities = activityRegistrationRepository.getByUserIdAndStartBeforeOrderByStart()
        TODO("")
    }

    fun getFormForUserFiscalYearAndRate(username: String, fiscalYear: Int?, rate: Double?): ByteArray {
        TODO("")
    }

    fun sendFormForUserFiscalYearAndRate(username: String, fiscalYear: Int?, rate: Double?): Boolean {
        TODO("")
    }
}