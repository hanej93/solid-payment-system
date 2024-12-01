package org.example.paymentservice.payment.application.service

import org.example.paymentservice.common.UseCase
import org.example.paymentservice.payment.application.port.`in`.command.CheckoutCommand
import org.example.paymentservice.payment.application.port.`in`.usecase.CheckoutUseCase
import org.example.paymentservice.payment.application.port.out.LoadProductPort
import org.example.paymentservice.payment.application.port.out.SavePaymentPort
import org.example.paymentservice.payment.domain.enums.PaymentStatus
import org.example.paymentservice.payment.domain.result.CheckoutResult
import org.example.paymentservice.payment.domain.vo.PaymentEvent
import org.example.paymentservice.payment.domain.vo.PaymentOrder
import org.example.paymentservice.payment.domain.vo.Product
import reactor.core.publisher.Mono

@UseCase
class CheckoutService (
    private val loadProductPort: LoadProductPort,
    private val savePaymentPort: SavePaymentPort,
) : CheckoutUseCase {

    override fun checkout(command: CheckoutCommand): Mono<CheckoutResult> {
        return loadProductPort.getProducts(command.cartId, command.productIds)
            .collectList()
            .map { createPaymentEvent(command, it) }
            .flatMap { savePaymentPort.save(it).thenReturn(it) }
            .map { CheckoutResult(amount = it.totalAmount(), orderId = it.orderId, orderName = it.orderName) }
    }

    private fun createPaymentEvent(command: CheckoutCommand, products: List<Product>): PaymentEvent {
        return PaymentEvent(
            buyerId = command.buyerId,
            orderId = command.idempotencyKey,
            orderName = products.joinToString { it.name },
            paymentOrders = products.map {
                PaymentOrder(
                    sellerId = it.sellerId,
                    orderId = command.idempotencyKey,
                    productId = it.id,
                    amount = it.amount.toLong(),
                    paymentStatus = PaymentStatus.NOT_STARTED,
                )
            }
        )
    }
}