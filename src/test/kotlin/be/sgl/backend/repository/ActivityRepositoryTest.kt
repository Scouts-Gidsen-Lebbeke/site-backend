package be.sgl.backend.repository

import be.sgl.backend.repository.activity.ActivityRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager

@DataJpaTest
class ActivityRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager
    @Autowired
    private lateinit var activityRepository: ActivityRepository
}