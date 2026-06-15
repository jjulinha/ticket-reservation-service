package com.unisul.seatreservation.service;

import com.unisul.seatreservation.domain.ReservationResult;
import com.unisul.seatreservation.domain.Ticket;
import com.unisul.seatreservation.dto.*;
import com.unisul.seatreservation.mapper.BookingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import io.awspring.cloud.sqs.operations.SqsTemplate;

import java.util.List;
import java.util.UUID;

@Service
public class SeatService {

    private static final Logger log = LoggerFactory.getLogger(SeatService.class);
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

        log.info("Iniciando processamento da reserva de estoque | orderId: {} | eventId: {} | totalItems: {}",
                orderId, eventId, event.items().size());

        ReservationResult result = seatTransactionService.tryProcessStockReservation(event, orderId, userId, eventId);

        switch (result){
            case SUCCESS:
                log.info("Reserva efetuada com SUCESSO. Preparando envio para pagamento | orderId: {}", orderId);
                List<Ticket> tickets = bookingMapper.findTicketsByOrderId(orderId);
                List<TicketResultDTO> ticketResultDTO = TicketResultDTO.fromList(tickets);

                OrderResponseEvent successPayload = new OrderResponseEvent(event.sagaId(), event.orderId(),
                        ticketResultDTO, event.paymentMethod(), event.installments());
                sendToQueue(FILA_PAGAMENTO, successPayload, eventId);
                break;

            case ALREADY_PROCESSED:
                log.warn("Reserva ignorada por idempotencia. Notificando falha ao OrderService | orderId: {}", orderId);
                FailureResponseEvent alreadyProcessedPayload = new FailureResponseEvent(event.sagaId(), event.orderId(), "PEDIDO_JA_PROCESSADO");
                sendToQueue(FILA_CANCELADO, alreadyProcessedPayload, eventId);
                break;

            case OUT_OF_STOCK:
                log.error("Estoque esgotado! Notificando falha ao OrderService | orderId: {} | eventId: {}", orderId, eventId);
                FailureResponseEvent failurePayload = new FailureResponseEvent(event.sagaId(), event.orderId(), "ASSENTO_INDISPONIVEL");
                sendToQueue(FILA_CANCELADO, failurePayload, eventId);
                break;
        }
    }

    public void paymentSuccess(OrderResponseEvent event){
        UUID orderId = UUID.fromString(event.orderId());
        log.info("Iniciando rotina de confirmação de pagamento (Saga) | orderId: {}", orderId);
        seatTransactionService.executePaymentSuccess(orderId);
    }

    public void compensateReservation(OrderResponseEvent event) {
        UUID orderId = UUID.fromString(event.orderId());
        log.info("Iniciando rotina de compensacao de reserva (Saga) | orderId: {}", orderId);
        seatTransactionService.executeCompensation(orderId);
    }

    public void registerNewEventStock(EventCreatedEvent event) {
        UUID eventId = UUID.fromString(event.eventId());
        log.info("Processando registro de estoque inicial | eventId: {} | capacity: {}", eventId, event.capacity());
        seatTransactionService.executeRegisterNewEventStock(eventId, event.capacity(), event.ticketPrice());
    }

    private void sendToQueue(String queueName, SagaResponseEvent payload, UUID eventId) {
        try {
            sqsTemplate.send(to -> to
                    .queue(queueName)
                    .payload(payload)
                    .header("MessageGroupId", eventId.toString())
                    .header("MessageDeduplicationId", payload.sagaId())
            );
            log.info("Evento da Saga publicado com sucesso | queue: {} | sagaId: {}", queueName, payload.sagaId());
        } catch (Exception e) {
            log.error("Falha critica ao publicar evento na fila SQS | queue: {} | sagaId: {} | errorMessage: {}",
                    queueName, payload.sagaId(), e.getMessage(), e);
        }
    }
}