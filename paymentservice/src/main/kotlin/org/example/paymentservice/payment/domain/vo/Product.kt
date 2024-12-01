package org.example.paymentservice.payment.domain.vo

data class Product(
    val id: Long,
    val amount: Long,
    val quantity: Int,
    val name: String,
    val sellerId: Long,
)
