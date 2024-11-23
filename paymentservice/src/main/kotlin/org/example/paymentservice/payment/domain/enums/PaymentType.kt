package org.example.paymentservice.payment.domain.enums

enum class PaymentType(description: String) {
    NORMAL("일반 결제");

    companion object {
        fun get(status: String): PaymentType {
            return PaymentType.entries.find { it.name == status } ?: throw IllegalArgumentException("PaymentType : $status 는 올바르지 않은 결제 타입입니다.")
        }
    }
}