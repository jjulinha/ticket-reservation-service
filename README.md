# Ticket Reservation Service (Seat Reservation)

Serviço de controle de estoque e reserva de assentos/tickets. Não expõe API REST — opera 100% via filas SQS, atuando como o "estoque" da saga: reserva assentos no pedido, confirma na aprovação do pagamento e compensa (devolve estoque) em caso de falha.

## Stack

- Java 21 + Spring Boot
- MyBatis + MySQL + Flyway
- Spring Cloud AWS SQS (`@SqsListener`, filas FIFO)
- Spring Boot Actuator + Micrometer/Prometheus

## Responsabilidades

- Registrar o estoque inicial de um evento quando ele é criado
- Reservar assentos/tickets para um pedido (com checagem de idempotência e de disponibilidade)
- Confirmar reserva quando o pagamento é aprovado
- Compensar (liberar) a reserva quando o pagamento falha

## Integração via SQS (sem endpoints REST)

| Fila consumida | Origem | Ação |
|---|---|---|
| `fila-evento-cadastrado.fifo` | Event Service | `registerNewEventStock` — cria estoque inicial (`capacity`, `ticketPrice`) para o evento |
| `fila-reserva-assentos.fifo` | Order Service | `processStockReservation` — tenta reservar os assentos do pedido |
| `fila-confirmar-reserva.fifo` | Payment Service | `paymentSuccess` — confirma definitivamente a reserva |
| `fila-compensar-reserva.fifo` | Payment Service | `compensateReservation` — desfaz a reserva (estorno de estoque) |

| Fila publicada | Destino | Quando |
|---|---|---|
| `fila-processar-pagamento.fifo` | Payment Service | Reserva bem-sucedida (`SUCCESS`) — envia lista de tickets reservados |
| `fila-pedido-cancelado.fifo` | Order Service | Reserva falhou: `OUT_OF_STOCK` (sem assentos) ou `ALREADY_PROCESSED` (idempotência) |

## Fluxo de reserva (`processStockReservation`)

`ReservationResult` pode ser:

- **`SUCCESS`**: assentos reservados → busca tickets do pedido, monta `OrderResponseEvent` (lista de `TicketResultDTO`, método de pagamento, parcelas) e publica em `fila-processar-pagamento.fifo`.
- **`ALREADY_PROCESSED`**: pedido já processado anteriormente (idempotência) → publica `FailureResponseEvent` (`PEDIDO_JA_PROCESSADO`) em `fila-pedido-cancelado.fifo`.
- **`OUT_OF_STOCK`**: sem assentos disponíveis → publica `FailureResponseEvent` (`ASSENTO_INDISPONIVEL`) em `fila-pedido-cancelado.fifo`.

Mensagens usam `eventId` como `MessageGroupId` e `sagaId` como `MessageDeduplicationId`.

## Modelo de domínio

- `EventStock`: estoque por evento (capacidade, preço do ticket)
- `Booking`: reserva associada a um pedido
- `Ticket`: ticket individual (id, tipo, preço, assento)
- `ReservationResult`: enum de resultado da tentativa de reserva (`SUCCESS`, `ALREADY_PROCESSED`, `OUT_OF_STOCK`)

## Configuração (variáveis de ambiente)

| Variável | Descrição |
|---|---|
| `SERVER_PORT` | Porta do serviço (default `8080`) — usada apenas para Actuator |
| `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` | Conexão MySQL |
| `AWS_REGION` | Região AWS (default `us-east-1`) |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_SESSION_TOKEN` | Credenciais AWS para SQS |

## Banco de dados

- Flyway (`db/migration`, `baseline-on-migrate=true`).
- Mappers MyBatis em `classpath:mapper/*.xml`, `map-underscore-to-camel-case=true`.
- `UuidBinaryTypeHandler` mapeia `UUID` ↔ `BINARY(16)`.

## Observabilidade

Actuator: `health`, `info`, `metrics`, `prometheus`, com tag `application=ticket-service`.

## Execução local

```bash
docker build -t ticket-service .
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:mysql://localhost:3306/tickets \
  -e DATABASE_USERNAME=root \
  -e DATABASE_PASSWORD=secret \
  -e AWS_ACCESS_KEY_ID=... \
  -e AWS_SECRET_ACCESS_KEY=... \
  ticket-service
```

## Papel na arquitetura

Guardião do estoque na saga. Não tem API pública — toda interação é assíncrona via SQS, com consistência garantida por idempotência (`ALREADY_PROCESSED`) e compensação (devolução de estoque em falha de pagamento).
