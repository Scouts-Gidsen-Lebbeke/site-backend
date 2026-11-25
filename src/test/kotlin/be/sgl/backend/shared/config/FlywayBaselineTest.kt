package be.sgl.backend.shared.config

import be.sgl.backend.shared.util.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.CoreMigrationType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.ConfigurationPropertySources
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

@IntegrationTest
class FlywayBaselineTest {

    @Autowired
    private lateinit var flyway: Flyway

    @Autowired
    private lateinit var dataSource: DataSource

    @AfterEach
    fun dropBaselineHistory() {
        JdbcTemplate(dataSource).execute("DROP TABLE IF EXISTS $BASELINE_HISTORY")
    }

    @Test
    fun `should run every migration when the database is empty`() {
        // given

        // when
        val info = flyway.info()

        // then
        assertThat(info.applied())
            .singleElement()
            .satisfies({ assertThat(it.type).isEqualTo(CoreMigrationType.SQL) })
            .satisfies({ assertThat(it.version.version).isEqualTo(BASELINE_VERSION) })
        assertThat(info.pending()).isEmpty()
    }

    @Test
    fun `should not baseline an unknown schema when running locally or in tests`() {
        // given

        // when
        val baselineOnMigrate = flyway.configuration.isBaselineOnMigrate

        // then
        assertThat(baselineOnMigrate).isFalse()
    }

    @ParameterizedTest
    @ValueSource(strings = ["dev", "prod"])
    fun `should baseline at version 1 when a deployed profile meets an existing schema`(profile: String) {
        // given
        val binder = profileBinder(profile)

        // when
        val baselineOnMigrate = binder.bind("spring.flyway.baseline-on-migrate", Boolean::class.java).orElse(false)
        val baselineVersion = binder.bind("spring.flyway.baseline-version", String::class.java).orElse(null)

        // then
        assertThat(baselineOnMigrate).isTrue()
        assertThat(baselineVersion).isEqualTo(BASELINE_VERSION)
    }

    @Test
    fun `should baseline without migrating when prod meets a schema without history`() {
        // given
        val prod = profileBinder("prod")
        val testee = Flyway.configure()
            .dataSource(dataSource)
            .table(BASELINE_HISTORY)
            .baselineOnMigrate(prod.bind("spring.flyway.baseline-on-migrate", Boolean::class.java).orElse(false) == true)
            .baselineVersion(prod.bind("spring.flyway.baseline-version", String::class.java).orElse(null) ?: "0")
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

    private fun profileBinder(profile: String): Binder {
        val sources = YamlPropertySourceLoader().load(profile, ClassPathResource("application-$profile.yml"))
        return Binder(ConfigurationPropertySources.from(sources))
    }

    companion object {
        private const val BASELINE_VERSION = "1"
        private const val BASELINE_HISTORY = "baseline_check_history"
    }
}
