package org.example.paymentservice.payment.application.port.out

import org.example.paymentservice.payment.domain.vo.PaymentEventMessage


interface DispatchEventMessagePort {

  fun dispatch(paymentEventMessage: PaymentEventMessage)
}