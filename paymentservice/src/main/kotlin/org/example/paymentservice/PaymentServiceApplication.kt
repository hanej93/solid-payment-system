package org.example.paymentservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.messaging.Message
import org.springframework.scheduling.annotation.EnableScheduling
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.function.Function

@EnableScheduling
@SpringBootApplication
class PaymentServiceApplication {

//    @Bean
//    fun consumer(): Function<Flux<Message<String>>, Mono<Void>> {
//        return Function { messages ->
//            messages.map {
//                println(it)
//                it
//            }.then()
//        }
//    }

    @Bean
    fun consumer(): Function<Flux<Message<String>>, Mono<Void>> {
        return Function { messages ->
            messages
                .doOnNext { println("Received message payload: ${it.payload}") }
                .then()
        }
    }
}
fun main(args: Array<String>) {
    runApplication<PaymentServiceApplication>(*args)
}
