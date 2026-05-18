# Kafka Architecture - API EAI Gateway

## Ringkasan Arsitektur

API EAI Gateway menggunakan Apache Kafka 3.7.0 sebagai message broker untuk event-driven communication antar microservices. Arsitektur ini memastikan loose coupling dan high scalability dalam sistem distribusi.

---

## Komponen Utama

### 1. Kafka Broker
- **Version**: 3.7.0
- **Node ID**: 1
- **Process Roles**: broker, controller
- **Bootstrap Server**: `localhost:9092` (untuk aplikasi external)

#### Port Configuration
| Port | Use Case | Protokol |
|------|----------|----------|
| 9092 | External connections (aplikasi) | EXTERNAL:PLAINTEXT |
| 29092 | Internal broker communication | INTERNAL:PLAINTEXT |
| 9093 | Controller communication | CONTROLLER:PLAINTEXT |

#### Konfigurasi Broker
- **Replication Factor**: 1 (single broker)
- **Min ISR** (In-Sync Replicas): 1
- **Offsets Topic Replication**: 1
- **Transaction State Log Replication**: 1
- **Group Rebalance Delay**: 0ms

### 2. Monitoring Tool
- **Kafka UI**: `localhost:8080`
- **Image**: provectuslabs/kafka-ui:latest
- **Purpose**: Visual monitoring dan management topics/consumer groups

---

## Kafka Topics

### Topic 1: `order-topic`

**Purpose**: Event notification untuk order lifecycle (created, cancelled)

| Property | Value |
|----------|-------|
| **Producer** | Order Service |
| **Consumer** | Inventory Service |
| **Consumer Group** | `inventory-group` |
| **Serializer** | JsonSerializer |
| **Message Format** | `OrderMessage` (Java Object) |

**Events**:
- `OrderCreated`: Trigger stock reservation di Inventory Service
- `OrderCancelled`: Trigger stock release di Inventory Service

**Message Structure**:
```json
{
  "orderId": 123,
  "orderNumber": "ORD-2024-001",
  "status": "PENDING",
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ]
}
```

### Topic 2: `payment-topic`

**Purpose**: Event notification untuk payment status

| Property | Value |
|----------|-------|
| **Producer** | Payment Service |
| **Consumer** | Order Service |
| **Consumer Group** | `order-group` |
| **Serializer** | StringSerializer |
| **Message Format** | `orderId:status` |

**Events**:
- `SUCCESS`: Order automatically transitioned to PAID, trigger shipping
- `FAILED`: Order remains PENDING, allow retry

**Message Format**:
```
123:SUCCESS
123:FAILED
```

---

## Event Flow

### Flow 1: Order Creation
```
1. User creates order via Order Service API
2. Order Service produces event: OrderCreated → order-topic
3. Inventory Service consumes: OrderCreated
4. Inventory Service: Reserve stock untuk setiap item
5. Status berubah menjadi stock reserved (acknowledged)
```

### Flow 2: Payment Processing
```
1. User initiates payment
2. Order Service → HTTP call → Payment Service
3. Payment Service memproses pembayaran
4. Payment Service produces: orderId:SUCCESS/FAILED → payment-topic
5. Order Service consumes: Payment Status
6. Order Service updates order status to PAID (success) atau PENDING (failed)
7. Jika SUCCESS: Trigger shipping process
```

### Flow 3: Order Cancellation
```
1. User cancels order
2. Order Service produces: OrderCancelled → order-topic
3. Inventory Service consumes: OrderCancelled
4. Inventory Service: Release/revert stock reservation
5. Stock inventory kembali normal
```

---

## Microservices Integration

### Order Service (Port: 8084)
- **Kafka Role**: 
  - **Producer**: Mengirim order events ke `order-topic`
  - **Consumer**: Mendengarkan payment status dari `payment-topic`
- **Configuration**:
  ```properties
  spring.kafka.bootstrap-servers=localhost:9092
  spring.kafka.producer.key-serializer=StringSerializer
  spring.kafka.producer.value-serializer=JsonSerializer
  ```

### Inventory Service (Port: 8085)
- **Kafka Role**: 
  - **Consumer**: Mendengarkan order events dari `order-topic`
- **Configuration**:
  ```properties
  spring.kafka.bootstrap-servers=localhost:9092
  spring.kafka.consumer.group-id=inventory-group
  spring.kafka.consumer.key-deserializer=StringDeserializer
  spring.kafka.consumer.value-deserializer=JsonDeserializer
  spring.kafka.consumer.properties.spring.json.value.default.type=OrderMessage
  ```

### Payment Service (Port: 8087)
- **Kafka Role**: 
  - **Producer**: Mengirim payment status ke `payment-topic`
- **Configuration**:
  ```properties
  spring.kafka.bootstrap-servers=localhost:9092
  spring.kafka.producer.key-serializer=StringSerializer
  spring.kafka.producer.value-serializer=StringSerializer
  ```

---

## Consumer Groups

| Group ID | Service | Topics | Purpose |
|----------|---------|--------|---------|
| `inventory-group` | Inventory Service | `order-topic` | Track order lifecycle & manage stock |
| `order-group` | Order Service | `payment-topic` | Track payment status & update order |

---

## Docker Compose Setup

```yaml
kafka:
  image: apache/kafka:3.7.0
  environment:
    KAFKA_NODE_ID: 1
    KAFKA_PROCESS_ROLES: broker,controller
    KAFKA_LISTENERS: INTERNAL://:29092,EXTERNAL://:9092,CONTROLLER://:9093
    KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka:29092,EXTERNAL://localhost:9092
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT
    KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
    KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
    KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093

kafka-ui:
  image: provectuslabs/kafka-ui:latest
  environment:
    KAFKA_CLUSTERS_0_NAME: local
    KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
```

---

## Key Design Patterns

### 1. **Event Sourcing**
- Setiap state change di-publish sebagai event
- Events persisted di Kafka topics
- Multiple consumers bisa subscribe to same topic

### 2. **Asynchronous Communication**
- Order Service tidak wait untuk Inventory Service confirm
- Lepas event ke Kafka, immediately return to client
- Inventory Service process async

### 3. **Service Decoupling**
- Services tidak perlu tahu detail HTTP endpoints satu sama lain
- Only need to know topic names
- Memudahkan horizontal scaling

### 4. **Error Handling**
- Payment FAILED: Order tetap PENDING (user bisa retry)
- Automatic acknowledgment setelah consumer process sukses
- Failed messages bisa di-retry atau move to dead letter queue

---

## Monitoring & Troubleshooting

### Access Kafka UI
```
http://localhost:8080
```

### Key Metrics to Monitor
1. **Lag per consumer group**: Apakah consumer tertinggal?
2. **Message throughput**: Berapa message per second?
3. **Broker health**: Broker online dan partition leaders healthy?
4. **Topic partitions**: Default 1 partition per topic

### Check Topics via CLI
```bash
# List all topics
kafka-topics.sh --bootstrap-server localhost:9092 --list

# Describe order-topic
kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic order-topic

# Check consumer group lag
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group inventory-group --describe
```

---

## Scalability Considerations

### Current Setup (Development)
- 1 Kafka broker
- 1 partition per topic
- Single replica

### Production Recommendations
1. **Multi-Broker Cluster**: Minimum 3 brokers untuk HA
2. **Replication Factor**: Minimum 3 untuk production
3. **Partitions**: Scale based on throughput needs
4. **Consumer Instances**: 1 consumer per partition untuk max parallelism
5. **Schema Registry**: Use Avro/Protobuf instead of JSON
6. **Security**: Enable TLS/SSL, SASL authentication
7. **Retention Policy**: Define message retention based on business needs

---

## Versioning

- **Kafka Version**: 3.7.0
- **KRaft Mode**: Yes (Zookeeper-less)
- **Last Updated**: May 2024

---

## Additional Resources

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Kafka UI GitHub](https://github.com/provectus/kafka-ui)
