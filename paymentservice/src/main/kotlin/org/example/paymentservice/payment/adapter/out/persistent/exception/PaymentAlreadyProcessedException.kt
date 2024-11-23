package org.example.paymentservice.payment.adapter.out.persistent.exception

import org.example.paymentservice.payment.domain.enums.PaymentStatus

class PaymentAlreadyProcessedException(
    val status: PaymentStatus,
    message: String
) : RuntimeException(message)