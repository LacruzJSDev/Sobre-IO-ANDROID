package dev.lacruzjs.sobreio.ui.envelopeform

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.fake.FakeEnvelopeRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_amount_negative
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnvelopeFormViewModelTest : MainDispatcherTest() {

    private val rent = Envelope("1", "g1", "Rent", EnvelopeKind.Spending, 50_000, 0)
    private val repository = FakeEnvelopeRepository(listOf(rent))

    @Test
    fun createSendsTrimmedNameKindAndTargetAndAsksToLeave() {
        val viewModel = EnvelopeFormViewModel("g1", null, repository)
        viewModel.onEvent(EnvelopeFormEvent.NameChanged("  Gym  "))
        viewModel.onEvent(EnvelopeFormEvent.KindSelected(EnvelopeKind.Saving))
        viewModel.onEvent(EnvelopeFormEvent.MonthlyTargetChanged("12,5"))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertEquals(listOf("createEnvelope(g1, Gym, Saving, 1250)"), repository.calls)
        assertTrue(viewModel.state.value.saved)
    }

    @Test
    fun blankTargetMeansZero() {
        val viewModel = EnvelopeFormViewModel("g1", null, repository)
        viewModel.onEvent(EnvelopeFormEvent.NameChanged("Gym"))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertTrue("createEnvelope(g1, Gym, Spending, 0)" in repository.calls)
    }

    @Test
    fun negativeTargetAndBlankNameAreRejectedWithoutCallingTheRepository() {
        val viewModel = EnvelopeFormViewModel("g1", null, repository)
        viewModel.onEvent(EnvelopeFormEvent.NameChanged("  "))
        viewModel.onEvent(EnvelopeFormEvent.MonthlyTargetChanged("-5"))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertEquals(Res.string.validation_required, viewModel.state.value.nameError)
        assertEquals(Res.string.validation_amount_negative, viewModel.state.value.monthlyTargetError)
        assertTrue(repository.calls.none { it.startsWith("create") })

        viewModel.onEvent(EnvelopeFormEvent.MonthlyTargetChanged("abc"))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)
        assertEquals(Res.string.validation_amount_invalid, viewModel.state.value.monthlyTargetError)
    }

    @Test
    fun failedCreateStaysOnTheFormWithTheError() {
        repository.createResult = Result.failure(AppError.DuplicateName)
        val viewModel = EnvelopeFormViewModel("g1", null, repository)
        viewModel.onEvent(EnvelopeFormEvent.NameChanged("Rent"))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertEquals(AppError.DuplicateName, viewModel.state.value.submitError)
        assertFalse(viewModel.state.value.saved)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun editLoadsTheEnvelopeAndSaves() {
        val viewModel = EnvelopeFormViewModel("g1", "1", repository)
        assertEquals("Rent", viewModel.state.value.name)
        assertEquals("500", viewModel.state.value.monthlyTarget)

        viewModel.onEvent(EnvelopeFormEvent.NameChanged("Housing"))
        viewModel.onEvent(EnvelopeFormEvent.KindSelected(EnvelopeKind.Saving))
        viewModel.onEvent(EnvelopeFormEvent.MonthlyTargetChanged(""))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertTrue("updateEnvelope(1, Housing, Saving, 0)" in repository.calls)
        assertTrue(viewModel.state.value.saved)
    }

    @Test
    fun editOfAMissingEnvelopeShowsNotFound() {
        assertEquals(AppError.NotFound, EnvelopeFormViewModel("g1", "nope", repository).state.value.loadError)
    }
}
