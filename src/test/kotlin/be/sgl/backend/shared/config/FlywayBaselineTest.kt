package be.sgl.backend.shared.config

import be.sgl.backend.shared.util.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.CoreMigrationType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import javax.sql.DataSource

@IntegrationTest
class FlywayBaselineTest {

    @Autowired
    private lateinit var flyway: Flyway

    @Autowired
    private lateinit var dataSource: DataSource

    @Test
    fun `should apply all migrations when the database is empty`() {
        // given

        // when
        val info = flyway.info()

        // then
        assertThat(info.current().version.version).isEqualTo(BASELINE_VERSION)
        assertThat(info.pending()).isEmpty()
    }

    @Test
    fun `should baseline without migrating when the schema exists without history`() {
        // given
        val testee = Flyway.configure()
            .dataSource(dataSource)
            .table("baseline_check_history")
            .baselineOnMigrate(true)
            .baselineVersion(BASELINE_VERSION)
            .load()

        // when
        val result = testee.migrate()

        // then
        assertThat(result.migrationsExecuted).isZero()
        assertThat(testee.info().applied())
            .singleElement()
            .satisfies({ assertThat(it.type).isEqualTo(CoreMigrationType.BASELINE) })
            .satisfies({ assertThat(it.version.version).isEqualTo(BASELINE_VERSION) })
    }

    companion object {
        private const val BASELINE_VERSION = "1"
    }
}
