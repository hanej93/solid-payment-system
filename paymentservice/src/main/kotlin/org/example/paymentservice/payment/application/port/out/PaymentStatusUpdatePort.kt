package org.example.paymentservice.payment.application.port.out

import org.example.paymentservice.payment.application.port.out.command.PaymentStatusUpdateCommand
import reactor.core.publisher.Mono

interface PaymentStatusUpdatePort {

    fun updatePaymentStatusToExecuting(orderId: String, paymentKey: String): Mono<Boolean>

    fun updatePaymentStatus(command: PaymentStatusUpdateCommand): Mono<Boolean>
}