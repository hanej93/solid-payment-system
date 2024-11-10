package org.example.paymentservice.test

import org.example.paymentservice.payment.domain.PaymentEvent
import reactor.core.publisher.Mono

interface PaymentDatabaseHelper {

    fun getPayments(orderId: String): PaymentEvent?

    fun clean(): Mono<Void>
}
