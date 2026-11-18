package dev.lacruzjs.sobreio.ui.envelopeform

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeDraft
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.EnvelopePurpose
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.fake.FakeAccountDetailRepository
import dev.lacruzjs.sobreio.fake.FakeEnvelopeRepository
import dev.lacruzjs.sobreio.fake.FakeMemberRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.envelope_type_has_funded
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_amount_negative
import dev.lacruzjs.sobreio.generated.resources.validation_amount_positive
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnvelopeFormViewModelTest : MainDispatcherTest() {

    private val rent = Envelope(
        "1", "g1", "Rent", EnvelopeKind.Spending, 50_000, 0,
        color = "#A4503F", icon = "home", ownerUserId = "u1", purpose = EnvelopePurpose.Monthly,
    )
    private val repository = FakeEnvelopeRepository(listOf(rent))
    private val juan = Member("u1", "Juan", GroupRole.Owner)
    private val marta = Member("u2", "Marta", GroupRole.Member)
    private val memberRepository = FakeMemberRepository(listOf(juan, marta))

    private val dates = FakeAccountDetailRepository()

    private fun form(envelopeId: String? = null) = EnvelopeFormViewModel("g1", envelopeId, repository, memberRepository, dates)

    @Test
    fun createSendsTrimmedNameKindAndTargetAndAsksToLeave() {
        val viewModel = form()
        viewModel.onEvent(EnvelopeFormEvent.NameChanged("  Gym  "))
        viewModel.onEvent(EnvelopeFormEvent.PurposeSelected(EnvelopePurpose.Savings))
        viewModel.onEvent(EnvelopeFormEvent.MonthlyTargetChanged("12,5"))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertEquals(listOf("createEnvelope(g1, Gym, Savings, 1250)"), repository.calls)
        assertEquals(EnvelopeKind.Saving, repository.createdDraft?.kind)
        assertTrue(viewModel.state.value.saved)
    }

    @Test
    fun blankContributionOfASavingsEnvelopeMeansZero() {
        val viewModel = form()
        viewModel.onEvent(EnvelopeFormEvent.NameChanged("Gym"))
        viewModel.onEvent(EnvelopeFormEvent.PurposeSelected(EnvelopePurpose.Savings))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertTrue("createEnvelope(g1, Gym, Savings, 0)" in repository.calls)
    }

    @Test
    fun theFormStartsAsAMonthlyEnvelope() {
        val state = form().state.value
        assertEquals(EnvelopePurpose.Monthly, state.purpose)
        assertTrue(state.showsTarget)
        assertTrue(state.targetRequired)
    }

    @Test
    fun eachTypeShowsItsOwnFields() {
        val viewModel = form()
        fun fields(purpose: EnvelopePurpose): Pair<Boolean, Boolean> {
            viewModel.onEvent(EnvelopeFormEvent.PurposeSelected(purpose))
            return viewModel.state.value.showsTarget to viewModel.state.value.targetRequired
        }

        assertEquals(true to true, fields(EnvelopePurpose.Monthly))
        assertEquals(false to false, fields(EnvelopePurpose.Bills))
        assertEquals(true to false, fields(EnvelopePurpose.Savings))
        assertEquals(false to false, fields(EnvelopePurpose.Free))
    }

    @Test
    fun aMonthlyEnvelopeNeedsATargetAboveZero() {
        val viewModel = form()
        viewModel.onEvent(EnvelopeFormEvent.NameChanged("Food"))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertEquals(Res.string.validation_amount_positive, viewModel.state.value.monthlyTargetError)
        assertTrue(repository.calls.none { it.startsWith("create") })

        viewModel.onEvent(EnvelopeFormEvent.MonthlyTargetChanged("250"))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)
        assertEquals(25_000L, repository.createdDraft?.monthlyTargetCents)
        assertEquals(EnvelopePurpose.Monthly, repository.createdDraft?.purpose)
    }

    @Test
    fun billsAndFreeSendZeroEvenWithATypedTargetAndSkipItsValidation() {
        listOf(EnvelopePurpose.Bills, EnvelopePurpose.Free).forEach { purpose ->
            val viewModel = form()
            viewModel.onEvent(EnvelopeFormEvent.NameChanged("X ${purpose.name}"))
            viewModel.onEvent(EnvelopeFormEvent.MonthlyTargetChanged("99"))
            viewModel.onEvent(EnvelopeFormEvent.PurposeSelected(purpose))
            viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

            assertEquals(0L, repository.createdDraft?.monthlyTargetCents)
            assertEquals(purpose, repository.createdDraft?.purpose)
            assertEquals(EnvelopeKind.Spending, repository.createdDraft?.kind)
            assertNull(viewModel.state.value.monthlyTargetError)
        }
    }

    @Test
    fun kindFollowsThePurposeOfTheDraft() {
        assertEquals(EnvelopeKind.Saving, EnvelopeDraft("a", EnvelopePurpose.Savings, 0).kind)
        listOf(EnvelopePurpose.Monthly, EnvelopePurpose.Bills, EnvelopePurpose.Free).forEach {
            assertEquals(EnvelopeKind.Spending, EnvelopeDraft("a", it, 0).kind)
        }
    }

    @Test
    fun changingTheTypeClearsItsErrors() {
        val viewModel = form()
        viewModel.onEvent(EnvelopeFormEvent.NameChanged("Food"))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)
        assertEquals(Res.string.validation_amount_positive, viewModel.state.value.monthlyTargetError)

        viewModel.onEvent(EnvelopeFormEvent.PurposeSelected(EnvelopePurpose.Free))
        assertNull(viewModel.state.value.monthlyTargetError)
    }

    @Test
    fun leavingBillsWithFundedCommitmentsExplainsItUnderTheType() {
        repository.envelopes = listOf(rent.copy(purpose = EnvelopePurpose.Bills, monthlyTargetCents = 0))
        repository.updateResult = Result.failure(AppError.BusinessRule("FZ021"))
        val viewModel = form("1")
        assertEquals(EnvelopePurpose.Bills, viewModel.state.value.purpose)

        viewModel.onEvent(EnvelopeFormEvent.PurposeSelected(EnvelopePurpose.Free))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertEquals(Res.string.envelope_type_has_funded, viewModel.state.value.purposeError)
        assertNull(viewModel.state.value.submitError)
        assertFalse(viewModel.state.value.saved)
        assertFalse(viewModel.state.value.isSubmitting)

        // Picking another type clears the message
        viewModel.onEvent(EnvelopeFormEvent.PurposeSelected(EnvelopePurpose.Bills))
        assertNull(viewModel.state.value.purposeError)
    }

    @Test
    fun otherSaveFailuresStayInTheGenericError() {
        repository.createResult = Result.failure(AppError.BusinessRule("FZ016"))
        val viewModel = form()
        viewModel.onEvent(EnvelopeFormEvent.NameChanged("Food"))
        viewModel.onEvent(EnvelopeFormEvent.MonthlyTargetChanged("10"))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertNull(viewModel.state.value.purposeError)
        assertEquals(AppError.BusinessRule("FZ016"), viewModel.state.value.submitError)
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
        viewModel.onEvent(EnvelopeFormEvent.MonthlyTargetChanged("500"))
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

        assertEquals(EnvelopePurpose.Monthly, viewModel.state.value.purpose)
        viewModel.onEvent(EnvelopeFormEvent.NameChanged("Housing"))
        viewModel.onEvent(EnvelopeFormEvent.PurposeSelected(EnvelopePurpose.Savings))
        viewModel.onEvent(EnvelopeFormEvent.MonthlyTargetChanged(""))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertTrue("updateEnvelope(1, Housing, Savings, 0)" in repository.calls)
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
            EnvelopeDraft("Food", EnvelopePurpose.Monthly, 30_000, "u2", "#4F7A5A", "groceries"),
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

        assertEquals(EnvelopeDraft("Rent", EnvelopePurpose.Monthly, 50_000, null, null, null), repository.updatedDraft)
    }

    @Test
    fun theHolderIsOnlyAskedForWhenTheGroupHasSeveralMembers() {
        assertTrue(form().state.value.hasSeveralMembers)

        memberRepository.members = listOf(juan)
        assertFalse(form().state.value.hasSeveralMembers)
    }

    // D-53: from when the target is assigned ------------------------------------------------------------------------

    @Test
    fun aNewEnvelopeCanStartNextPeriod() {
        val viewModel = form()
        assertTrue(viewModel.state.value.canChooseStart)
        assertFalse(viewModel.state.value.startsNextPeriod)

        viewModel.onEvent(EnvelopeFormEvent.NameChanged("Gym"))
        viewModel.onEvent(EnvelopeFormEvent.MonthlyTargetChanged("30"))
        viewModel.onEvent(EnvelopeFormEvent.StartNextPeriodChanged(true))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        // The fake's period ends on 30 November
        assertEquals(LocalDate(2026, 12, 1), repository.createdDraft?.targetsFrom)
    }

    @Test
    fun aNewEnvelopeLeavesTheStartToTheDatabaseByDefault() {
        val viewModel = form()
        viewModel.onEvent(EnvelopeFormEvent.NameChanged("Gym"))
        viewModel.onEvent(EnvelopeFormEvent.MonthlyTargetChanged("30"))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertNull(repository.createdDraft?.targetsFrom)
    }

    @Test
    fun typesWithoutTargetDoNotOfferTheStart() {
        val viewModel = form()
        viewModel.onEvent(EnvelopeFormEvent.PurposeSelected(EnvelopePurpose.Bills))

        assertFalse(viewModel.state.value.canChooseStart)
    }

    @Test
    fun anEnvelopeWhoseTargetAlreadyRunsDoesNotOfferTheStart() {
        repository.envelopes = listOf(rent.copy(targetsFrom = LocalDate(2026, 10, 1)))
        val viewModel = form("1")

        assertFalse(viewModel.state.value.canChooseStart)
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)
        assertNull(repository.updatedDraft?.targetsFrom)
    }

    @Test
    fun anEnvelopeThatStartsLaterShowsItAndTurningItOffStartsToday() {
        repository.envelopes = listOf(rent.copy(targetsFrom = LocalDate(2026, 12, 1)))
        val viewModel = form("1")
        assertTrue(viewModel.state.value.canChooseStart)
        assertTrue(viewModel.state.value.startsNextPeriod)

        viewModel.onEvent(EnvelopeFormEvent.StartNextPeriodChanged(false))
        viewModel.onEvent(EnvelopeFormEvent.SaveClicked)

        assertEquals(dates.today, repository.updatedDraft?.targetsFrom)
    }

    @Test
    fun withoutTheDatesTheStartIsNotOffered() {
        dates.todayResult = Result.failure(AppError.Network)
        assertFalse(form().state.value.canChooseStart)
    }
}
