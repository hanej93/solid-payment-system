package org.example.paymentservice.payment.application.port.`in`.usecase

import org.example.paymentservice.payment.application.port.`in`.command.CheckoutCommand
import org.example.paymentservice.payment.domain.result.CheckoutResult
import reactor.core.publisher.Mono

interface CheckoutUseCase {

    fun checkout(command: CheckoutCommand): Mono<CheckoutResult>
}