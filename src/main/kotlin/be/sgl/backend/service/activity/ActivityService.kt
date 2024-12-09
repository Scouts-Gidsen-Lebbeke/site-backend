package be.sgl.backend.service.activity

import be.sgl.backend.dto.ActivityDTO
import be.sgl.backend.dto.ActivityRegistrationDTO
import be.sgl.backend.repository.ActivityRegistrationRepository
import be.sgl.backend.repository.ActivityRepository
import be.sgl.backend.service.mapper.ActivityMapper
import be.sgl.backend.service.mapper.ActivityRegistrationMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ActivityService {

    @Autowired
    private lateinit var activityRepository: ActivityRepository
    @Autowired
    private lateinit var registrationRepository: ActivityRegistrationRepository
    @Autowired
    private lateinit var activityMapper: ActivityMapper
    @Autowired
    private lateinit var registrationMapper: ActivityRegistrationMapper

    fun getAllActivities(): List<ActivityDTO> {
        return activityRepository.findAll().map(activityMapper::toDto)
    }

    fun getVisibleActivities(): List<ActivityDTO> {
        TODO("Not yet implemented")
    }

    fun getActivityDTOById(id: Int): ActivityDTO? {
        TODO("Not yet implemented")
    }

    fun saveActivityDTO(branch: ActivityDTO): ActivityDTO {
        TODO("Not yet implemented")
    }

    fun mergeActivityDTOChanges(id: Int, activity: ActivityDTO): ActivityDTO {
        TODO("Not yet implemented")
    }

    fun deleteActivity(id: Int) {
        TODO("Not yet implemented")
    }

    fun getAllRegistrationsForActivity(id: Int): List<ActivityRegistrationDTO> {
        TODO("Not yet implemented")
    }
}