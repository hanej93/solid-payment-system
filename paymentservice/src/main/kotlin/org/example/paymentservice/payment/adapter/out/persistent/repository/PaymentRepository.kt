package org.example.paymentservice.payment.adapter.out.persistent.repository

import org.example.paymentservice.payment.domain.entity.PaymentEvent
import org.example.paymentservice.payment.domain.vo.PendingPaymentEvent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface PaymentRepository {

    fun save(paymentEvent: PaymentEvent): Mono<Void>
    fun getPendingPayments(): Flux<PendingPaymentEvent>
}