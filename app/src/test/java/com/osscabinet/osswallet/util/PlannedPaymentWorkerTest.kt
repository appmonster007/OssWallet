package com.osscabinet.ossWallet.util

import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.service.management.ConfirmResult
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentConfirmer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

class PlannedPaymentWorkerTest {

    private val repo: PlannedPaymentRepository = mockk()
    private val confirmer: PlannedPaymentConfirmer = mockk()
    private val profileId = 1

    private fun epochOf(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun makePayment(
        autoConfirm: Boolean,
        recurrenceType: RecurrenceType = RecurrenceType.MONTHLY,
        status: PlannedPaymentStatus = PlannedPaymentStatus.ACTIVE,
        nextDueDate: Long = epochOf(2024, 1, 15)
    ) = PlannedPayment(
        id = 1L,
        name = "Rent",
        categoryId = 1001L,
        amount = BigDecimal("100"),
        entryMode = EntryMode.EXPENSE,
        currency = "USD",
        schedule = PaymentSchedule(
            recurrenceRule = RecurrenceRule(type = recurrenceType),
            startDate = epochOf(2024, 1, 1),
            nextDueDate = nextDueDate
        ),
        autoConfirm = autoConfirm,
        status = status
    )

    @Test
    fun `autoConfirm payment delegates to PlannedPaymentConfirmer instead of building its own request`() = runTest {
        val payment = makePayment(autoConfirm = true)
        coEvery { repo.getDue(profileId, any()) } returns listOf(payment)
        coEvery {
            confirmer.confirm(payment, payment.schedule.nextDueDate, profileId)
        } returns ConfirmResult.Confirmed(transactionId = 42L, paymentName = payment.name)

        processDuePlannedPayments(repo, confirmer, profileId, now = epochOf(2024, 1, 20))

        coVerify(exactly = 1) {
            confirmer.confirm(payment, payment.schedule.nextDueDate, profileId)
        }
        // The confirmer owns advancing nextDueDate/occurrence atomically — the worker must
        // not also update the payment itself for this item.
        coVerify(exactly = 0) { repo.update(any()) }
    }

    @Test
    fun `autoConfirm payment needing review is left due instead of being silently advanced`() = runTest {
        val payment = makePayment(autoConfirm = true)
        coEvery { repo.getDue(profileId, any()) } returns listOf(payment)
        coEvery {
            confirmer.confirm(payment, payment.schedule.nextDueDate, profileId)
        } returns ConfirmResult.NeedsReview(payment.name)

        processDuePlannedPayments(repo, confirmer, profileId, now = epochOf(2024, 1, 20))

        coVerify(exactly = 0) { repo.update(any()) }
    }

    @Test
    fun `non-autoConfirm recurring payment advances nextDueDate without calling the confirmer`() = runTest {
        val payment = makePayment(autoConfirm = false, recurrenceType = RecurrenceType.MONTHLY)
        coEvery { repo.getDue(profileId, any()) } returns listOf(payment)
        coEvery { repo.update(any()) } returns Unit

        processDuePlannedPayments(repo, confirmer, profileId, now = epochOf(2024, 1, 20))

        coVerify(exactly = 0) { confirmer.confirm(any<PlannedPayment>(), any(), any()) }
        coVerify(exactly = 1) {
            repo.update(
                payment.copy(
                    schedule = payment.schedule.copy(
                        nextDueDate = epochOf(2024, 2, 15),
                        lastTriggeredDate = epochOf(2024, 1, 15)
                    )
                )
            )
        }
    }

    @Test
    fun `non-autoConfirm one-time payment is marked COMPLETED instead of rescheduled`() = runTest {
        val payment = makePayment(autoConfirm = false, recurrenceType = RecurrenceType.ONE_TIME)
        coEvery { repo.getDue(profileId, any()) } returns listOf(payment)
        coEvery { repo.update(any()) } returns Unit

        processDuePlannedPayments(repo, confirmer, profileId, now = epochOf(2024, 1, 20))

        coVerify(exactly = 0) { confirmer.confirm(any<PlannedPayment>(), any(), any()) }
        coVerify(exactly = 1) { repo.update(payment.copy(status = PlannedPaymentStatus.COMPLETED)) }
    }

    @Test
    fun `payments not ACTIVE are skipped entirely`() = runTest {
        val paused = makePayment(autoConfirm = true, status = PlannedPaymentStatus.PAUSED)
        coEvery { repo.getDue(profileId, any()) } returns listOf(paused)

        processDuePlannedPayments(repo, confirmer, profileId, now = epochOf(2024, 1, 20))

        coVerify(exactly = 0) { confirmer.confirm(any<PlannedPayment>(), any(), any()) }
        coVerify(exactly = 0) { repo.update(any()) }
    }
}
