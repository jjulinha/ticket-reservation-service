package com.unisul.seatreservation.service;

import com.unisul.seatreservation.domain.ReservationResult;
import com.unisul.seatreservation.domain.Ticket;
import com.unisul.seatreservation.dto.*;
import com.unisul.seatreservation.mapper.BookingMapper;
import org.springframework.stereotype.Service;
import io.awspring.cloud.sqs.operations.SqsTemplate;

import java.util.List;
import java.util.UUID;

@Service
public class SeatService {

    private final SqsTemplate sqsTemplate;
    private final SeatTransactionService seatTransactionService;

    private static final String FILA_CANCELADO = "fila-pedido-cancelado.fifo";
    private static final String FILA_PAGAMENTO = "fila-processar-pagamento.fifo";
    private final BookingMapper bookingMapper;

    public SeatService(SqsTemplate sqsTemplate, SeatTransactionService seatTransactionService, BookingMapper bookingMapper) {
        this.sqsTemplate = sqsTemplate;
        this.seatTransactionService = seatTransactionService;
        this.bookingMapper = bookingMapper;
    }

    public void processStockReservation(OrderCreatedEvent event) {
        UUID orderId = UUID.fromString(event.orderId());
        UUID userId = UUID.fromString(event.userId());
        UUID eventId = UUID.fromString(event.eventId());

        ReservationResult result = seatTransactionService.tryProcessStockReservation(event, orderId, userId, eventId);

        switch (result){
            case SUCCESS:
                List<Ticket> tickets = bookingMapper.findTicketsByOrderId(orderId);
                List<TicketResultDTO> ticketResultDTO = TicketResultDTO.fromList(tickets);

                OrderResponseEvent successPayload = new OrderResponseEvent(event.sagaId(), event.orderId(),
                        ticketResultDTO, event.paymentMethod(), event.installments());
                sendToQueue(FILA_PAGAMENTO, successPayload, eventId);
                break;

            case ALREADY_PROCESSED:
                FailureResponseEvent alreadyProcessedPayload = new FailureResponseEvent(event.sagaId(), event.orderId(), "PEDIDO_JA_PROCESSADO");
                sendToQueue(FILA_CANCELADO, alreadyProcessedPayload, eventId);
                break;

            case OUT_OF_STOCK:
                FailureResponseEvent failurePayload = new FailureResponseEvent(event.sagaId(), event.orderId(), "ASSENTO_INDISPONIVEL");
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
        seatTransactionService.executeRegisterNewEventStock(eventId, event.capacity(), event.ticketPrice());
    }

    private void sendToQueue(String queueName, SagaResponseEvent payload, UUID eventId) {
        sqsTemplate.send(to -> to
                .queue(queueName)
                .payload(payload)
                .header("MessageGroupId", eventId.toString()) // Garante ordenação por evento/show nas filas FIFO
                .header("MessageDeduplicationId", payload.sagaId())
        );
    }
}