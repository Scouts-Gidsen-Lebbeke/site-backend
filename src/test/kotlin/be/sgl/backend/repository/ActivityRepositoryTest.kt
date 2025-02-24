package be.sgl.backend.repository

import be.sgl.backend.util.IntegrationTest
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager

@Transactional
@AutoConfigureTestEntityManager
@IntegrationTest
class ActivityRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager
    @Autowired
    private lateinit var activityRepository: ActivityRepository
}