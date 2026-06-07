\# Ticket Reservation Service



Microsserviço de reserva de assentos para o sistema distribuído de venda de ingressos online (A3 — Sistemas Distribuídos e Mobile, UNISUL).



\## Responsabilidade



Este serviço é responsável por:



\- Receber pedidos de reserva via fila SQS (vindos do Order Service)

\- Decrementar atomicamente o estoque de assentos por evento

\- Persistir a reserva (booking) e os ingressos (tickets) emitidos

\- Publicar resultado em fila SQS de pagamento (sucesso) ou cancelamento (estoque insuficiente)

\- Compensar reservas em caso de falha de pagamento (devolver estoque, marcar booking como `COMPENSATED`)



\## Stack



\- Java 21

\- Spring Boot 4.0.6

\- MyBatis (acesso a dados, sem JPA)

\- MySQL 8 (BINARY(16) para IDs UUIDv7)

\- Flyway (versionamento de schema)

\- AWS SDK + Spring Cloud AWS (SQS FIFO)

\- Maven



\## Arquitetura



O serviço opera de forma assíncrona via filas SQS FIFO, sem expor endpoints REST públicos.



\### Filas consumidas



\- `fila-reserva-assentos.fifo` — pedidos de reserva (Order Service)

\- `fila-compensar-reserva.fifo` — pedidos de compensação (Payment Service)



\### Filas publicadas



\- `fila-processar-pagamento.fifo` — reserva confirmada, pronta para cobrar

\- `fila-pedido-cancelado.fifo` — reserva falhou (estoque insuficiente)



\### Tabelas



\- `tb\_event\_stock` — estoque atual por evento (`available\_capacity`)

\- `tb\_bookings` — reservas (orderId, userId, eventId, status, total)

\- `tb\_tickets` — ingressos emitidos por reserva (1 booking -> N tickets)



\## Requisitos distribuídos atendidos



\- \*\*Controle de concorrência\*\*: `UPDATE ... WHERE available\_capacity >= quantity` é atômico por linha no MySQL — duas reservas concorrentes pelo mesmo evento não causam overselling. O `CHECK (available\_capacity >= 0)` na tabela garante a invariante mesmo em caso de bug.

\- \*\*Idempotência\*\*: antes de processar, o serviço verifica se o `orderId` já existe no banco. Se sim, retorna `ALREADY\_PROCESSED` sem efeitos colaterais. Atende redentrega de mensagens (SQS é at-least-once). Adicionalmente, `MessageDeduplicationId = sagaId` na publicação para deduplicação na própria fila.

\- \*\*Resiliência\*\*: padrão Saga com compensação. Falha em qualquer ponto downstream (pagamento) dispara `executeCompensation`, que devolve o estoque ao evento e marca a reserva como `COMPENSATED`.

\- \*\*Atomicidade\*\*: todos os métodos críticos anotados com `@Transactional` — rollback automático em caso de exceção.



\## Variáveis de ambiente



DATABASE\_URL=jdbc:mysql://localhost:3306/seat\_reservation

DATABASE\_USERNAME=...

DATABASE\_PASSWORD=...

AWS\_REGION=us-east-1

AWS\_ACCESS\_KEY\_ID=...

AWS\_SECRET\_ACCESS\_KEY=...

AWS\_SESSION\_TOKEN=...



\## Execução



mvn clean install

mvn spring-boot:run



Porta padrão: 8081



\## Observabilidade



Endpoints do Spring Actuator expostos em `/actuator`:



\- `/actuator/health` — health check

\- `/actuator/metrics` — métricas

\- `/actuator/prometheus` — formato Prometheus (consumível pelo Grafana)

