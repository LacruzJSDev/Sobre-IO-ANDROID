package dev.lacruzjs.sobreio.data.remote

import dev.lacruzjs.sobreio.data.remote.dto.DeletionPreviewDto
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.DeletionOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountDeletionErrorTest {

    @Test
    fun fz022FromTheFunctionIsTheBlockedRuleNotTheTagMerge() {
        assertEquals(
            AppError.BusinessRule(ErrorMapper.ACCOUNT_BLOCKED_CODE),
            ErrorMapper.fromFunctionBody("""{"code":"FZ022","message":"blocked"}"""),
        )
    }

    @Test
    fun otherFzCodesFromTheFunctionKeepTheirCode() {
        assertEquals(AppError.BusinessRule("FZ009"), ErrorMapper.fromFunctionBody("""{"code": "FZ009"}"""))
    }

    @Test
    fun serverFailureOrUnreadableBodiesAreNotBusinessRules() {
        assertNull(ErrorMapper.fromFunctionBody("""{"code":"auth_delete_failed"}"""))
        assertNull(ErrorMapper.fromFunctionBody("not json"))
        assertNull(ErrorMapper.fromFunctionBody("""{"message":"x"}"""))
        assertNull(ErrorMapper.fromFunctionBody(""))
        assertNull(ErrorMapper.fromFunctionBody(null))
    }

    @Test
    fun previewRowsMapOutcomes() {
        fun outcome(value: String) = DeletionPreviewDto("g", "Casa", "#fff", "home", value).toDomain().outcome
        assertEquals(DeletionOutcome.Delete, outcome("delete"))
        assertEquals(DeletionOutcome.Leave, outcome("leave"))
        assertEquals(DeletionOutcome.Blocked, outcome("blocked"))
        assertEquals(DeletionOutcome.Blocked, outcome("something new"))
    }
}
