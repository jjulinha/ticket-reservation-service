package com.unisul.seatreservation.service;

import com.unisul.seatreservation.domain.ReservationResult;
import com.unisul.seatreservation.dto.EventCreatedEvent;
import com.unisul.seatreservation.dto.OrderCreatedEvent;
import com.unisul.seatreservation.dto.OrderResponseEvent;
import org.springframework.stereotype.Service;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.UUID;

@Service
public class SeatService {

    private final SqsTemplate sqsTemplate;
    private final SeatTransactionService seatTransactionService;

    private static final String FILA_CANCELADO = "fila-pedido-cancelado.fifo";
    private static final String FILA_PAGAMENTO = "fila-processar-pagamento.fifo";

    public SeatService(SqsTemplate sqsTemplate, SeatTransactionService seatTransactionService) {
        this.sqsTemplate = sqsTemplate;
        this.seatTransactionService = seatTransactionService;
    }

    public void processStockReservation(OrderCreatedEvent event) {
        UUID orderId = UUID.fromString(event.orderId());
        UUID userId = UUID.fromString(event.userId());
        UUID eventId = UUID.fromString(event.eventId());

        ReservationResult result = seatTransactionService.tryProcessStockReservation(event, orderId, userId, eventId);

        switch (result){
            case SUCCESS:
                OrderResponseEvent successPayload = new OrderResponseEvent(event.sagaId(), event.orderId(), "RESERVA_CONFIRMADA");
                sendToQueue(FILA_PAGAMENTO, successPayload, eventId);
                break;

            case ALREADY_PROCESSED:
                //log
                break;

            case OUT_OF_STOCK:
                OrderResponseEvent failurePayload = new OrderResponseEvent(event.sagaId(), event.orderId(), "ASSENTO_INDISPONIVEL");
                sendToQueue(FILA_CANCELADO, failurePayload, eventId);
                break;
        }
    }

    public void compensateReservation(OrderResponseEvent event) {
        UUID orderId = UUID.fromString(event.orderId());
        seatTransactionService.executeCompensation(orderId);
    }

    public void registerNewEventStock(EventCreatedEvent event) {
        UUID eventId = UUID.fromString(event.eventId());
        seatTransactionService.executeRegisterNewEventStock(eventId, event.capacity());
    }

    private void sendToQueue(String queueName, OrderResponseEvent payload, UUID eventId) {
        sqsTemplate.send(to -> to
                .queue(queueName)
                .payload(payload)
                .header("MessageGroupId", eventId.toString()) // Garante ordenação por evento/show nas filas FIFO
                .header("MessageDeduplicationId",