package org.example.paymentservice.payment.application.port.`in`.usecase

import org.example.paymentservice.payment.domain.event.LedgerEventMessage
import org.example.paymentservice.payment.domain.event.WalletEventMessage
import reactor.core.publisher.Mono

interface PaymentCompleteUseCase {

  fun completePayment(walletEventMessage: WalletEventMessage): Mono<Void>

  fun completePayment(ledgerEventMessage: LedgerEventMessage): Mono<Void>
}