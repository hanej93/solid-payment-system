package org.example.paymentservice.payment.domain.vo

import org.example.paymentservice.payment.domain.enums.PaymentStatus

data class PendingPaymentOrder (
  val paymentOrderId: Long,
  val status: PaymentStatus,
  val amount: Long,
  val failedCount: Byte,
  val threshold: Byte
)
