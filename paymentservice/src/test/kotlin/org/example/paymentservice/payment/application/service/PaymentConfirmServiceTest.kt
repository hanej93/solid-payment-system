package org.example.paymentservice.payment.application.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.example.paymentservice.payment.application.port.`in`.command.CheckoutCommand
import org.example.paymentservice.payment.application.port.`in`.command.PaymentConfirmCommand
import org.example.paymentservice.payment.application.port.`in`.usecase.CheckoutUseCase
import org.example.paymentservice.payment.application.port.out.PaymentExecutorPort
import org.example.paymentservice.payment.application.port.out.PaymentStatusUpdatePort
import org.example.paymentservice.payment.application.port.out.PaymentValidationPort
import org.example.paymentservice.payment.domain.enums.PSPConfirmationStatus
import org.example.paymentservice.payment.domain.enums.PaymentMethod
import org.example.paymentservice.payment.domain.enums.PaymentStatus
import org.example.paymentservice.payment.domain.enums.PaymentType
import org.example.paymentservice.payment.domain.result.PaymentExecutionFailure
import org.example.paymentservice.payment.domain.result.PaymentExecutionResult
import org.example.paymentservice.payment.domain.result.PaymentExtraDetails
import org.example.paymentservice.test.PaymentDatabaseHelper
import org.example.paymentservice.test.PaymentTestConfiguration
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@SpringBootTest
@Import(PaymentTestConfiguration::class)
class PaymentConfirmServiceTest(
    @Autowired private val checkoutUseCase: CheckoutUseCase,
    @Autowired private val paymentStatusUpdatePort: PaymentStatusUpdatePort,
    @Autowired private val paymentValidationPort: PaymentValidationPort,
    @Autowired private val paymentDatabaseHelper: PaymentDatabaseHelper,
) {
    private val mockPaymentExecutorPort = mockk<PaymentExecutorPort>()

    @BeforeEach
    fun setup() {
        paymentDatabaseHelper.clean().block()
    }

    @Test
    fun `should be marked as SUCCESS if Payment Confirmation success in PSP`() {
        Hooks.onOperatorDebug()

        val orderId = UUID.randomUUID().toString()

        val checkoutCommand = CheckoutCommand(
            cartId = 1,
            buyerId = 1,
            productIds = listOf(1, 2, 3),
            idempotencyKey = orderId
        )

        val checkoutResult = checkoutUseCase.checkout(checkoutCommand).block()!!

        val paymentConfirmCommand = PaymentConfirmCommand(
            paymentKey = UUID.randomUUID().toString(),
            orderId = orderId,
            amount = checkoutResult.amount
        )

        val paymentConfirmService = PaymentConfirmService(
            paymentStatusUpdatePort = paymentStatusUpdatePort,
            paymentValidationPort = paymentValidationPort,
            paymentExecutorPort = mockPaymentExecutorPort,
//   paymentErrorHandler = paymentErrorHandler
        )

        val paymentExecutionResult = PaymentExecutionResult(
            paymentKey = paymentConfirmCommand.paymentKey,
            orderId = paymentConfirmCommand.orderId,
            extraDetails = PaymentExtraDetails(
                type = PaymentType.NORMAL,
                method = PaymentMethod.EASY_PAY,
                totalAmount = paymentConfirmCommand.amount,
                orderName = "test_order_name",
                pspConfirmationStatus = PSPConfirmationStatus.DONE,
                approvedAt = LocalDateTime.now(),
                pspRawData = "{}"
            ),
            isSuccess = true,
            isRetryable = false,
            isUnknown = false,
            isFailure = false
        )

        every { mockPaymentExecutorPort.execute(paymentConfirmCommand) } returns Mono.just(paymentExecutionResult)

        val paymentConfirmationResult = paymentConfirmService.confirm(paymentConfirmCommand).block()!!

        val paymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        assertThat(paymentConfirmationResult.status).isEqualTo(PaymentStatus.SUCCESS)
        assertTrue(paymentEvent.isSuccess())
        assertThat(paymentEvent.paymentType).isEqualTo(paymentExecutionResult.extraDetails!!.type)
        assertThat(paymentEvent.paymentMethod).isEqualTo(paymentExecutionResult.extraDetails!!.method)
        assertThat(paymentEvent.orderName).isEqualTo(paymentExecutionResult.extraDetails!!.orderName)
        assertThat(paymentEvent.approvedAt?.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(
            paymentExecutionResult.extraDetails!!.approvedAt.truncatedTo(
                ChronoUnit.SECONDS
            )
        )
    }

    @Test
    fun `should be marked as FAILURE if Payment Confirmation fails on PSP`() {
        Hooks.onOperatorDebug()

        val orderId = UUID.randomUUID().toString()

        val checkoutCommand = CheckoutCommand(
            cartId = 1,
            buyerId = 1,
            productIds = listOf(1, 2, 3),
            idempotencyKey = orderId
        )

        val checkoutResult = checkoutUseCase.checkout(checkoutCommand).block()!!

        val paymentConfirmCommand = PaymentConfirmCommand(
            paymentKey = UUID.randomUUID().toString(),
            orderId = orderId,
            amount = checkoutResult.amount
        )

        val paymentConfirmService = PaymentConfirmService(
            paymentStatusUpdatePort = paymentStatusUpdatePort,
            paymentValidationPort = paymentValidationPort,
            paymentExecutorPort = mockPaymentExecutorPort,
//   paymentErrorHandler = paymentErrorHandler
        )

        val paymentExecutionResult = PaymentExecutionResult(
            paymentKey = paymentConfirmCommand.paymentKey,
            orderId = paymentConfirmCommand.orderId,
            extraDetails = PaymentExtraDetails(
                type = PaymentType.NORMAL,
                method = PaymentMethod.EASY_PAY,
                totalAmount = paymentConfirmCommand.amount,
                orderName = "test_order_name",
                pspConfirmationStatus = PSPConfirmationStatus.DONE,
                approvedAt = LocalDateTime.now(),
                pspRawData = "{}"
            ),
            failure = PaymentExecutionFailure("ERROR", "Test Error"),
            isSuccess = false,
            isRetryable = false,
            isUnknown = false,
            isFailure = true
        )

        every { mockPaymentExecutorPort.execute(paymentConfirmCommand) } returns Mono.just(paymentExecutionResult)

        val paymentConfirmationResult = paymentConfirmService.confirm(paymentConfirmCommand).block()!!

        val paymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        assertThat(paymentConfirmationResult.status).isEqualTo(PaymentStatus.FAILURE)
        assertTrue(paymentEvent.isFailure())
    }

    @Test
    fun `should be marked as UNKNOWN if Payment Confirmation fails due to an unknown exception`() {
        Hooks.onOperatorDebug()

        val orderId = UUID.randomUUID().toString()

        val checkoutCommand = CheckoutCommand(
            cartId = 1, buyerId = 1, productIds = listOf(1, 2, 3), idempotencyKey = orderId
        )

        val checkoutResult = checkoutUseCase.checkout(checkoutCommand).block()!!

        val paymentConfirmCommand = PaymentConfirmCommand(
            paymentKey = UUID.randomUUID().toString(), orderId = orderId, amount = checkoutResult.amount
        )

        val paymentConfirmService = PaymentConfirmService(
            paymentStatusUpdatePort = paymentStatusUpdatePort,
            paymentValidationPort = paymentValidationPort,
            paymentExecutorPort = mockPaymentExecutorPort,
            //   paymentErrorHandler = paymentErrorHandler
        )

        val paymentExecutionResult = PaymentExecutionResult(
            paymentKey = paymentConfirmCommand.paymentKey,
            orderId = paymentConfirmCommand.orderId,
            extraDetails = PaymentExtraDetails(
                type = PaymentType.NORMAL,
                method = PaymentMethod.EASY_PAY,
                totalAmount = paymentConfirmCommand.amount,
                orderName = "test_order_name",
                pspConfirmationStatus = PSPConfirmationStatus.DONE,
                approvedAt = LocalDateTime.now(),
                pspRawData = "{}"
            ),
            failure = PaymentExecutionFailure("ERROR", "Test Error"),
            isSuccess = false,
            isRetryable = false,
            isUnknown = true,
            isFailure = false,
        )

        every { mockPaymentExecutorPort.execute(paymentConfirmCommand) } returns Mono.just(paymentExecutionResult)

        val paymentConfirmationResult = paymentConfirmService.confirm(paymentConfirmCommand).block()!!

        val paymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        assertThat(paymentConfirmationResult.status).isEqualTo(PaymentStatus.UNKNOWN)
        assertTrue(paymentEvent.isUnknown())
    }
}