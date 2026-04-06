package be.sgl.backend.service.user.sync

sealed interface SyncState {
    val fixable: Boolean
    val name: String
}

sealed class FixableSyncState: SyncState {
    override val fixable = true
}

sealed class UnfixableSyncState: SyncState {
    override val fixable = false
}

data class HasExternalOpenRegistration(val requestId: String) : FixableSyncState() {
    override val name = "HAS_EXTERNAL_OPEN_REGISTRATION"
}

class HasNewExternalMemberId : FixableSyncState() {
    override val name = "HAS_NEW_EXTERNAL_MEMBER_ID"
}

class HasExternalMemberIdButNoAccount : UnfixableSyncState() {
    override val name = "HAS_EXTERNAL_MEMBER_ID_BUT_NO_ACCOUNT"
}

class HasNoExternalMemberIdYet : UnfixableSyncState() {
    override val name = "HAS_NO_EXTERNAL_MEMBER_ID_YET"
}

data class HasUnmatchedExternalFunctions(val functionsToAssign: List<String>, val functionsToDeassign: List<String>) : FixableSyncState() {
    override val name = "HAS_UNMATCHED_EXTERNAL_FUNCTIONS"
}

class HasNoActiveMembership : FixableSyncState() {
    override val name = "HAS_NO_ACTIVE_MEMBERSHIP"
}

class Ok : UnfixableSyncState() {
    override val name = "OK"
}