package org.example.paymentservice.payment.domain.entity

import org.example.paymentservice.payment.domain.enums.PaymentMethod
import org.example.paymentservice.payment.domain.enums.PaymentStatus
import org.example.paymentservice.payment.domain.enums.PaymentType
import java.time.LocalDateTime

data class PaymentEvent (
    val id: Long? = null,
    val buyerId: Long,
    val orderName: String,
    val orderId: String,
    val paymentKey: String? = null,
    val paymentType: PaymentType? = null,
    val paymentMethod: PaymentMethod? = null,
    val approvedAt: LocalDateTime? = null,
    val paymentOrders: List<PaymentOrder> = emptyList(),
    private val isPaymentDone: Boolean = false,
) {
    fun totalAmount(): Long {
        return paymentOrders.sumOf { it.amount }.toLong()
    }

    fun isPaymentDone(): Boolean = isPaymentDone

    fun isSuccess(): Boolean {
        return paymentOrders.all { it.paymentStatus == PaymentStatus.SUCCESS }
    }

    fun isFailure(): Boolean {
        return paymentOrders.all { it.paymentStatus == PaymentStatus.FAILURE }
    }

    fun isUnknown(): Boolean {
        return paymentOrders.all { it.paymentStatus == PaymentStatus.UNKNOWN }
    }
}