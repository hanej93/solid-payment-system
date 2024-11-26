package org.example.paymentservice.payment.application.service

import org.example.paymentservice.payment.adapter.out.persistent.repository.PaymentOutboxRepository
import org.example.paymentservice.payment.application.port.out.DispatchEventMessagePort
import org.example.paymentservice.payment.application.port.out.LoadPendingPaymentEventMessagePort
import org.example.paymentservice.payment.application.port.out.command.PaymentStatusUpdateCommand
import org.example.paymentservice.payment.domain.enums.PSPConfirmationStatus
import org.example.paymentservice.payment.domain.enums.PaymentMethod
import org.example.paymentservice.payment.domain.enums.PaymentType
import org.example.paymentservice.payment.domain.result.PaymentExecutionResult
import org.example.paymentservice.payment.domain.result.PaymentExtraDetails
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Hooks
import java.time.LocalDateTime
import java.util.*

@SpringBootTest
@Tag("ExternalIntegration")
class PaymentEventMessageRelayServiceTest(
    @Autowired private val paymentOutboxRepository: PaymentOutboxRepository,
    @Autowired private val loadPendingPaymentEventMessagePort: LoadPendingPaymentEventMessagePort,
    @Autowired private val dispatchEventMessagePort: DispatchEventMessagePort,
) {
    @Test
    fun `should dispatch external message system`() {
        Hooks.onOperatorDebug()

        val paymentEventMessageRelayUseCase =
            PaymentEventMessageRelayService(loadPendingPaymentEventMessagePort, dispatchEventMessagePort)

        val command = PaymentStatusUpdateCommand(
            paymentExecutionResult = PaymentExecutionResult(
                paymentKey = UUID.randomUUID().toString(),
                orderId = UUID.randomUUID().toString(),
                extraDetails = PaymentExtraDetails(
                    type = PaymentType.NORMAL,
                    method = PaymentMethod.EASY_PAY,
                    approvedAt = LocalDateTime.now(),
                    orderName = "test_order_name",
                    pspConfirmationStatus = PSPConfirmationStatus.DONE,
                    totalAmount = 50000L,
                    pspRawData = "{}"
                ),
                isSuccess = true,
                isFailure = false,
                isUnknown = false,
                isRetryable = false
            )
        )

        paymentOutboxRepository.insertOutbox(command).block()

        paymentEventMessageRelayUseCase.relay()

        Thread.sleep(10000)
    }
}