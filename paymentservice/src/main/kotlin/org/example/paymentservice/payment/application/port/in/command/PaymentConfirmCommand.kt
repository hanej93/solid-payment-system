package org.example.paymentservice.payment.application.port.`in`.command

data class PaymentConfirmCommand (
    val paymentKey: String,
    val orderId: String,
    val amount: Long
)
