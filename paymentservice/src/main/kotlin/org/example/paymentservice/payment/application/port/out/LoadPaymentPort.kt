package org.example.paymentservice.payment.application.port.out

import org.example.paymentservice.payment.domain.vo.PaymentEvent
import reactor.core.publisher.Mono

interface LoadPaymentPort {

  fun getPayment(orderId: String): Mono<PaymentEvent>
}