package be.sgl.backend.service.user.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class SyncStateTest {

    @ParameterizedTest(name = "{1}")
    @MethodSource("states")
    fun `should expose its name and fixability`(testee: SyncState, name: String, fixable: Boolean) {
        // given

        // when
        val actual = testee.name to testee.fixable

        // then
        assertThat(actual).isEqualTo(name to fixable)
    }

    companion object {
        @JvmStatic
        fun states() = listOf(
            Arguments.of(HasExternalOpenRegistration("r1"), "HAS_EXTERNAL_OPEN_REGISTRATION", true),
            Arguments.of(HasNewExternalMemberId(), "HAS_NEW_EXTERNAL_MEMBER_ID", true),
            Arguments.of(HasExternalMemberIdButNoAccount(), "HAS_EXTERNAL_MEMBER_ID_BUT_NO_ACCOUNT", false),
            Arguments.of(HasNoExternalMemberIdYet(), "HAS_NO_EXTERNAL_MEMBER_ID_YET", false),
            Arguments.of(HasUnmatchedExternalFunctions(listOf("a"), listOf("b")), "HAS_UNMATCHED_EXTERNAL_FUNCTIONS", true),
            Arguments.of(HasNoActiveMembership(), "HAS_NO_ACTIVE_MEMBERSHIP", true),
            Arguments.of(Ok(), "OK", false),
        )
    }
}
