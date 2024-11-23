package org.example.paymentservice.payment.domain.result

data class PaymentFailure(
    val errorCode: String,
    val message: String,
)
