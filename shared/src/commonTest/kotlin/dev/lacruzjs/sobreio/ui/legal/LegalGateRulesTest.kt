package dev.lacruzjs.sobreio.ui.legal

import dev.lacruzjs.sobreio.fake.FakeLegalRepository
import dev.lacruzjs.sobreio.fake.FakeLegalRepository.Companion.PRIVACY
import dev.lacruzjs.sobreio.fake.FakeLegalRepository.Companion.TERMS
import dev.lacruzjs.sobreio.fake.FakeLegalRepository.Companion.status
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LegalGateRulesTest {

    private val older = LocalDate(2026, 1, 1)

    @Test
    fun nothingPublishedOrEverythingAcceptedLetsTheUserIn() {
        assertEquals(LegalGateUiState.Clear, legalGateStateOf(emptyList()))
        assertEquals(
            LegalGateUiState.Clear,
            legalGateStateOf(listOf(status(TERMS, FakeLegalRepository.VERSION, false), status(PRIVACY, FakeLegalRepository.VERSION, false))),
        )
    }

    @Test
    fun aNewUserIsWelcomedToBothDocuments() {
        val state = assertIs<LegalGateUiState.Required>(legalGateStateOf(listOf(status(TERMS, null, true), status(PRIVACY, null, true))))
        assertEquals(TERMS, state.terms)
        assertEquals(PRIVACY, state.privacy)
        assertEquals(false, state.isUpdate)
        assertEquals(false, state.canContinue)
    }

    @Test
    fun someoneWhoAcceptedAnOlderVersionIsToldTheyChanged() {
        val state = legalGateStateOf(listOf(status(TERMS, FakeLegalRepository.VERSION, false), status(PRIVACY, older, true)))
        assertEquals(true, assertIs<LegalGateUiState.Required>(state).isUpdate)
    }

    @Test
    fun withOnlyOneDocumentInForceThereIsNothingTheDatabaseWouldAccept() {
        assertEquals(LegalGateUiState.Clear, legalGateStateOf(listOf(status(TERMS, null, true))))
    }

    @Test
    fun continueNeedsTheSwitchAndNoRequestRunning() {
        val required = assertIs<LegalGateUiState.Required>(legalGateStateOf(listOf(status(TERMS, null, true), status(PRIVACY, null, true))))
        assertEquals(true, required.copy(accepted = true).canContinue)
        assertEquals(false, required.copy(accepted = true, isAccepting = true).canContinue)
        assertEquals(false, required.copy(accepted = true, isSigningOut = true).canContinue)
    }
}
