package dev.lacruzjs.sobreio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row of `account_forecast`: one per live account of the group. */
@Serializable
internal data class ForecastDto(
    @SerialName("account_id") val accountId: String,
    @SerialName("balance_now") val balanceNow: Long = 0,
    @SerialName("scheduled_in") val scheduledIn: Long = 0,
    @SerialName("scheduled_out") val scheduledOut: Long = 0,
    @SerialName("projected_end") val projectedEnd: Long = 0,
    @SerialName("projected_min") val projectedMin: Long = 0,
    val shortfall: Long = 0,
)

/** A row of `account_balance_series`; dates travel as ISO text. */
@Serializable
internal data class BalancePointDto(val day: String, val balance: Long = 0)

/** A row of `account_projection`. */
@Serializable
internal data class ProjectionPointDto(val day: String, val balance: Long = 0, val scheduled: Long = 0)

/** A row of `account_period_flows`. */
@Serializable
internal data class PeriodFlowDto(
    @SerialName("period_id") val periodId: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    val inflow: Long = 0,
    val outflow: Long = 0,
)

/** A row of `upcoming_occurrences`. */
@Serializable
internal data class UpcomingDto(
    @SerialName("commitment_id") val commitmentId: String,
    @SerialName("commitment_name") val name: String,
    val kind: String,
    @SerialName("due_date") val dueDate: String,
    val amount: Long = 0,
    @SerialName("account_id") val accountId: String? = null,
    @SerialName("to_account_id") val toAccountId: String? = null,
)

/** A row of the `movements` view. */
@Serializable
internal data class MovementDto(
    val id: String,
    val kind: String,
    val amount: Long = 0,
    val date: String,
    val notes: String? = null,
    @SerialName("counter_account_id") val counterAccountId: String? = null,
    @SerialName("envelope_id") val envelopeId: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
)

/** The row `compare_with_bank` returns. */
@Serializable
internal data class BankComparisonDto(
    @SerialName("app_balance") val appBalance: Long = 0,
    @SerialName("bank_balance") val bankBalance: Long = 0,
    val difference: Long = 0,
)

/** The row `current_period` returns. */
@Serializable
internal data class PeriodRangeDto(
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
)
