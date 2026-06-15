package com.unisul.seatreservation.controller;

import com.unisul.seatreservation.dto.EventCreatedEvent;
import com.unisul.seatreservation.dto.OrderCreatedEvent;
import com.unisul.seatreservation.dto.OrderResponseEvent;
import com.unisul.seatreservation.service.SeatService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BookingQueueListener {

    private static final Logger log = LoggerFactory.getLogger(BookingQueueListener.class);
    private final SeatService seatService;

    public BookingQueueListener(SeatService seatService) {
        this.seatService = seatService;
    }

    @SqsListener("fila-reserva-assentos.fifo")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Recebida solicitacao para reservar assentos | orderId: {} | sagaId: {}", event.orderId(), event.sagaId());
        try {
            seatService.processStockReservation(event);
        } catch (Exception e) {
            log.error("Falha inesperada ao processar reserva de assentos | orderId: {} | sagaId: {} | errorMessage: {}",
                    event.orderId(), event.sagaId(), e.getMessage(), e);
        }
    }

    @SqsListener("fila-confirmar-reserva.fifo")
    public void handlePaymentSuccess(OrderResponseEvent event){
        log.info("Recebida confirmação de pagamento | orderId: {} | sagaId: {}", event.orderId(), event.sagaId());
        try {
            seatService.paymentSuccess(event);
        } catch (Exception e) {
            log.error("Falha inesperada ao confirmar pagamento | orderId: {} | sagaId: {} | errorMessage: {}",
                    event.orderId(), event.sagaId(), e.getMessage(), e);
        }
    }

    @SqsListener("fila-compensar-reserva.fifo")
    public void handlePaymentFailed(OrderResponseEvent event) {
        log.info("Recebido comando de COMPENSACAO (Estorno de Assentos) | orderId: {} | sagaId: {}", event.orderId(), event.sagaId());
        try {
            seatService.compensateReservation(event);
        } catch (Exception e) {
            log.error("Falha inesperada ao compensar reserva | orderId: {} | sagaId: {} | errorMessage: {}",
                    event.orderId(), event.sagaId(), e.getMessage(), e);
        }
    }

    @SqsListener("fila-evento-cadastrado.fifo")
    public void handleNewEventCreated(EventCreatedEvent event) {
        log.info("Recebido cadastro de novo evento para controle de estoque | eventId: {}", event.eventId());
        try {
            seatService.registerNewEventStock(event);
        } catch (Exception e) {
            log.error("Falha inesperada ao registrar estoque do novo evento | eventId: {} | errorMessage: {}",
                    event.eventId(), e.getMessage(), e);
        }
    }
}