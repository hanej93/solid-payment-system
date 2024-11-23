package org.example.paymentservice.payment.adapter.out.persistent.repository

import org.example.paymentservice.payment.domain.entity.PaymentEvent
import reactor.core.publisher.Mono

interface PaymentRepository {

    fun save(paymentEvent: PaymentEvent): Mono<Void>
}