package org.example.paymentservice.payment.adapter.out.web.toss.executor

import io.netty.handler.timeout.TimeoutException
import org.example.paymentservice.payment.adapter.out.web.toss.response.TossFailureResponse
import org.example.paymentservice.payment.adapter.out.web.toss.response.TossPaymentConfirmationResponse
import org.example.paymentservice.payment.application.port.`in`.command.PaymentConfirmCommand
import org.example.paymentservice.payment.domain.enums.PSPConfirmationStatus
import org.example.paymentservice.payment.domain.enums.PaymentMethod
import org.example.paymentservice.payment.domain.enums.PaymentType
import org.example.paymentservice.payment.domain.result.PaymentExecutionResult
import org.example.paymentservice.payment.domain.result.PaymentExtraDetails
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class TossPaymentExecutor (
    private val tossPaymentWebClient: WebClient,
    private val uri: String = "/v1/payments/confirm"
) : PaymentExecutor {

    override fun execute(command: PaymentConfirmCommand): Mono<PaymentExecutionResult> {
        return tossPaymentWebClient.post()
                    .uri(uri)
                    .header("Idempotency-Key", command.orderId)
                    .bodyValue("""
                {
                  "paymentKey": "${command.paymentKey}",
                  "orderId": "${command.orderId}", 
                  "amount": ${command.amount}
                }
              """.trimIndent())
            .retrieve()
            .bodyToMono(TossPaymentConfirmationResponse::class.java)
            .map {
                PaymentExecutionResult(
                    paymentKey = command.paymentKey,
                    orderId = command.orderId,
                    extraDetails = PaymentExtraDetails(
                        type = PaymentType.get(it.type),
                        method = PaymentMethod.get(it.method),
                        approvedAt = LocalDateTime.parse(it.approvedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        pspRawData = it.toString(),
                        orderName = it.orderName,
                        pspConfirmationStatus = PSPConfirmationStatus.get(it.status),
                        totalAmount = it.totalAmount.toLong()
                    ),
                    isSuccess = true,
                    isFailure = false,
                    isUnknown = false,
                    isRetryable = false
                )
            }
    }

}