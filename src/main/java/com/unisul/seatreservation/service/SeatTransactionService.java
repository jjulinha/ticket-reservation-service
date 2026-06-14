package com.unisul.seatreservation.service;
import com.fasterxml.uuid.Generators;
import com.unisul.seatreservation.domain.Booking;
import com.unisul.seatreservation.domain.ReservationResult;
import com.unisul.seatreservation.domain.Ticket;
import com.unisul.seatreservation.dto.OrderCreatedEvent;
import com.unisul.seatreservation.mapper.BookingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SeatTransactionService {

    private static final Logger log = LoggerFactory.getLogger(SeatTransactionService.class);
    private final BookingMapper bookingMapper;

    public SeatTransactionService(BookingMapper bookingMapper) {
        this.bookingMapper = bookingMapper;
    }

    @Transactional
    public ReservationResult tryProcessStockReservation(OrderCreatedEvent event, UUID orderId, UUID userId, UUID eventId) {
        int totalItems = event.items().size();

        Optional<Booking> existing = Optional.ofNullable(bookingMapper.findBookingWithTickets(orderId));
        if (existing.isPresent()) {
            log.info("Bloqueio de Idempotencia ativado: Booking ja existente no banco | orderId: {}", orderId);
            return ReservationResult.ALREADY_PROCESSED;
        }

        int rowsAffected = bookingMapper.decrementStock(eventId, totalItems);
        if (rowsAffected == 0) {
            log.warn("Falha ao decrementar estoque: Assentos insuficientes | orderId: {} | eventId: {} | requestedQuantity: {}",
                    orderId, eventId, totalItems);
            return ReservationResult.OUT_OF_STOCK;
        }

        log.info("Estoque decrementado com sucesso. Registrando Booking... | orderId: {} | eventId: {}", orderId, eventId);

        Booking booking = new Booking();
        booking.setOrderId(orderId);
        booking.setUserId(userId);
        booking.setEventId(eventId);
        booking.setBookingStatus("PENDING");

        bookingMapper.insertBooking(booking);

        BigDecimal ticketPrice = bookingMapper.getEventTicketPrice(eventId);

        for (OrderCreatedEvent.ItemEvent item : event.items()) {
            Ticket ticket = new Ticket();
            ticket.setTicketId(Generators.timeBasedEpochGenerator().generate());
            ticket.setOrderId(orderId);
            ticket.setEventId(eventId);
            ticket.setSeatIdentifier(item.seatIdentifier());
            ticket.setTicketType(item.ticketType());
            ticket.setTicketPrice(ticketPrice);

            bookingMapper.insertTicket(ticket);
            log.info("Ticket persistido no banco | orderId: {} | ticketId: {} | seatIdentifier: {}",
                    orderId, ticket.getTicketId(), item.seatIdentifier());
        }

        return ReservationResult.SUCCESS;
    }

    @Transactional
    public void executeCompensation(UUID orderId) {
        List<Ticket> ticketsReservados = bookingMapper.findTicketsByOrderId(orderId);

        if (ticketsReservados != null && !ticketsReservados.isEmpty()) {
            UUID eventId = ticketsReservados.get(0).getEventId();
            int quantidadeParaDevolver = ticketsReservados.size();

            bookingMapper.updateBookingStatus(orderId, "COMPENSATED");
            bookingMapper.incrementStock(eventId, quantidadeParaDevolver);

            log.info("Compensacao concluida: Status atualizado para COMPENSATED e estoque devolvido | orderId: {} | eventId: {} | assentosDevolvidos: {}",
                    orderId, eventId, quantidadeParaDevolver);
        } else {
            log.warn("Tentativa de compensacao ignorada: Nenhum assento encontrado para o pedido | orderId: {}", orderId);
        }
    }

    @Transactional
    public void executeRegisterNewEventStock(UUID eventId, int capacity, BigDecimal ticketPrice) {
        bookingMapper.insertEventStock(eventId, capacity, ticketPrice);
        log.info("Capacidade inicial e preco persistidos no banco de dados | eventId: {} | capacity: {}", eventId, capacity);
    }
}