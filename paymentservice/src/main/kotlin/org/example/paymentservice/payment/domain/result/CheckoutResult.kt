package org.example.paymentservice.payment.domain.result

data class CheckoutResult (
    val amount: Long,
    val orderId: String,
    val orderName: String,
)