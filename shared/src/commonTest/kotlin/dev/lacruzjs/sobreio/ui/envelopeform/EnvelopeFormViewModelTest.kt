package dev.lacruzjs.sobreio.ui.envelopeform

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeDraft
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.fake.FakeEnvelopeRepository
import dev.lacruzjs.sobreio.fake.FakeMemberRepository
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

    private val rent = Envelope(
        "1", "g1", "Rent", EnvelopeKind.Spending, 50_000, 0,
        color = "#A4503F", icon = "home", ownerUserId = "u1",
    )
    private val repository = FakeEnvelopeRepository(listOf(rent))
    private val juan = Member("u1", "Juan", GroupRole.Owner)
    private val marta = Member("u2", "Marta", GroupRole.Member)
    private val memberRepository = FakeMemberRepository(listOf(juan, marta))

    private fun form(envelopeId: String? = null) = EnvelopeFormViewModel("g1", envelopeId, repository, memberRepository)

    @Test
    fun createSendsTrimmedNameKindAndTargetAndAsksToLeave() {
        val viewModel = form()
        viewModel.onEvent(EnvelopeFormEvent.NameChanged("  Gym  "))
        viewModel.onEvent(EnvelopeFormEvent.KindSelected(EnvelopeKind.Saving))
        viewModel.onEvent(EnvelopeFormEvent.MonthlyTargetChanged("12,5"))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertEquals(listOf("createEnvelope(g1, Gym, Saving, 1250)"), repository.calls)
        assertTrue(viewModel.state.value.saved)
    }

    @Test
    fun blankTargetMeansZero() {
        val viewModel = form()
        viewModel.onEvent(EnvelopeFormEvent.NameChanged("Gym"))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertTrue("createEnvelope(g1, Gym, Spending, 0)" in repository.calls)
    }

    @Test
    fun negativeTargetAndBlankNameAreRejectedWithoutCallingTheRepository() {
        val viewModel = form()
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
        val viewModel = form()
        viewModel.onEvent(EnvelopeFormEvent.NameChanged("Rent"))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertEquals(AppError.DuplicateName, viewModel.state.value.submitError)
        assertFalse(viewModel.state.value.saved)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun editLoadsTheEnvelopeAndSaves() {
        val viewModel = form("1")
        assertEquals("Rent", viewModel.state.value.name)
        assertEquals("500", viewModel.state.value.monthlyTarget)
        assertEquals("#A4503F", viewModel.state.value.color)
        assertEquals("home", viewModel.state.value.icon)
        assertEquals("u1", viewModel.state.value.ownerUserId)

        viewModel.onEvent(EnvelopeFormEvent.NameChanged("Housing"))
        viewModel.onEvent(EnvelopeFormEvent.KindSelected(EnvelopeKind.Saving))
        viewModel.onEvent(EnvelopeFormEvent.MonthlyTargetChanged(""))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertTrue("updateEnvelope(1, Housing, Saving, 0)" in repository.calls)
        assertTrue(viewModel.state.value.saved)
    }

    @Test
    fun editOfAMissingEnvelopeShowsNotFound() {
        assertEquals(AppError.NotFound, form("nope").state.value.loadError)
    }

    @Test
    fun createSendsEveryField() {
        val viewModel = form()
        viewModel.onEvent(EnvelopeFormEvent.NameChanged("Food"))
        viewModel.onEvent(EnvelopeFormEvent.ColorSelected("#4F7A5A"))
        viewModel.onEvent(EnvelopeFormEvent.IconSelected("groceries"))
        viewModel.onEvent(EnvelopeFormEvent.HolderSelected("u2"))
        viewModel.onEvent(EnvelopeFormEvent.MonthlyTargetChanged("300"))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertEquals(
            EnvelopeDraft("Food", EnvelopeKind.Spending, 30_000, "u2", "#4F7A5A", "groceries"),
            repository.createdDraft,
        )
    }

    @Test
    fun editSendsEveryFieldIncludingClearedOnes() {
        val viewModel = form("1")
        viewModel.onEvent(EnvelopeFormEvent.ColorSelected(null))
        viewModel.onEvent(EnvelopeFormEvent.IconSelected(null))
        viewModel.onEvent(EnvelopeFormEvent.HolderSelected(null))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertEquals(EnvelopeDraft("Rent", EnvelopeKind.Spending, 50_000, null, null, null), repository.updatedDraft)
    }

    @Test
    fun theHolderIsOnlyAskedForWhenTheGroupHasSeveralMembers() {
        assertTrue(form().state.value.hasSeveralMembers)

        memberRepository.members = listOf(juan)
        assertFalse(form().state.value.hasSeveralMembers)
    }
}
