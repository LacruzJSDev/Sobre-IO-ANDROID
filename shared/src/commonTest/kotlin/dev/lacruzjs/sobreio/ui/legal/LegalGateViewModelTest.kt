package dev.lacruzjs.sobreio.ui.legal

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.AuthState
import dev.lacruzjs.sobreio.fake.FakeAuthRepository
import dev.lacruzjs.sobreio.fake.FakeLegalRepository
import dev.lacruzjs.sobreio.fake.FakeLegalRepository.Companion.PRIVACY
import dev.lacruzjs.sobreio.fake.FakeLegalRepository.Companion.TERMS
import dev.lacruzjs.sobreio.fake.FakeLegalRepository.Companion.VERSION
import dev.lacruzjs.sobreio.fake.FakeLegalRepository.Companion.status
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LegalGateViewModelTest : MainDispatcherTest() {

    private val pending = Result.success(listOf(status(TERMS, null, true), status(PRIVACY, null, true)))
    private val legal = FakeLegalRepository(status = pending)
    private val auth = FakeAuthRepository(AuthState.Loading)
    private val viewModel by lazy { LegalGateViewModel(legal, auth) }

    @Test
    fun aFailedReadNeverLocksTheUserOut() {
        legal.status = Result.failure(AppError.Network)
        assertEquals(LegalGateUiState.Clear, viewModel.state.value)
    }

    @Test
    fun acceptingRecordsTheVersionsShownAndOpensTheApp() {
        viewModel.onEvent(LegalGateEvent.ContinueClicked)
        assertEquals(emptyList(), legal.accepted, "nothing is accepted before the switch is on")

        viewModel.onEvent(LegalGateEvent.AcceptedChanged(true))
        viewModel.onEvent(LegalGateEvent.ContinueClicked)

        assertEquals(listOf("$VERSION/$VERSION"), legal.accepted)
        assertEquals(LegalGateUiState.Clear, viewModel.state.value)
    }

    @Test
    fun aVersionThatChangedMeanwhileShowsTheNewOneUnticked() {
        val newer = LocalDate(2027, 1, 1)
        legal.acceptResult = Result.failure(AppError.BusinessRule("FZ025"))
        viewModel.onEvent(LegalGateEvent.AcceptedChanged(true))
        legal.status = Result.success(listOf(status(TERMS.copy(version = newer), null, true), status(PRIVACY, null, true)))

        viewModel.onEvent(LegalGateEvent.ContinueClicked)

        val state = assertIs<LegalGateUiState.Required>(viewModel.state.value)
        assertEquals(newer, state.terms.version)
        assertEquals(false, state.accepted)
        assertEquals(AppError.BusinessRule("FZ025"), state.error)
    }

    @Test
    fun anotherFailureKeepsTheScreenWithTheError() {
        legal.acceptResult = Result.failure(AppError.Network)
        viewModel.onEvent(LegalGateEvent.AcceptedChanged(true))
        viewModel.onEvent(LegalGateEvent.ContinueClicked)

        val state = assertIs<LegalGateUiState.Required>(viewModel.state.value)
        assertEquals(AppError.Network, state.error)
        assertEquals(true, state.accepted)
        assertEquals(false, state.isAccepting)
    }

    @Test
    fun signingOutAsksTheSession() {
        viewModel.onEvent(LegalGateEvent.SignOutClicked)
        assertEquals(listOf("signOut()"), auth.calls)
    }
}
