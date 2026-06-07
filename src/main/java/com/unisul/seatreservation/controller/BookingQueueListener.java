package com.unisul.seatreservation.controller;

import com.unisul.seatreservation.dto.EventCreatedEvent;
import com.unisul.seatreservation.dto.OrderCreatedEvent;
import com.unisul.seatreservation.dto.OrderResponseEvent;
import com.unisul.seatreservation.service.SeatService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Component;

@Component
public class BookingQueueListener {

    private final SeatService seatService;

    public BookingQueueListener(SeatService seatService) {
        this.seatService = seatService;
    }

    /**
     * Escuta requisições vindas do Order Service para checar e travar assentos.
     */
    @SqsListener("fila-reserva-assentos.fifo")
    public void handleOrderCreated(OrderCreatedEvent event) {
        seatService.processStockReservation(event);
    }

    /**
     * Escuta eventos de compensação disparados pelo serviço de Pagamentos.
     */
    @SqsListener("fila-compensar-reserva.fifo")
    public void handlePaymentFailed(OrderResponseEvent event) {
        seatService.compensateReservation(event);
    }

    /**
     * Escuta o cadastro de novos eventos/shows vindos do Serviço de Eventos
     * para espelhar a capacidade máxima de assentos permitida.
     */
    /*@SqsListener("fila-evento-cadastrado-replicacao")
    public void handleNewEventCreated(EventCreatedEvent event) {
        seatService.registerNewEventStock(event);
    }*/
}