# Kafka Camel Dynamic Routing POC

A Spring Boot application demonstrating **dynamic message routing using Apache Camel with Kafka** - providing RabbitMQ-style topic exchange functionality. Messages from a main `orders` topic are automatically routed to regional subtopics based on message content using Camel's powerful routing capabilities.

## What This Does

This POC shows how to achieve **RabbitMQ-style topic routing** using Apache Camel with Kafka:
- Apache Camel consumes messages from the `orders` topic
- Messages are filtered and routed based on content:
  - Messages containing "us" → `orders.us`
  - Messages containing "eu" → `orders.eu`  
  - All other messages → `orders.other`

## Architecture

```
orders (main topic)
    ↓ [Apache Camel Content-Based Router]
    ├── orders.us    (US region orders)
    ├── orders.eu    (EU region orders) 
    └── orders.other (All other orders)
```

## Technology Stack

- **Spring Boot 3.2.0** - Application framework
- **Apache Camel 4.2.0** - Integration framework for routing logic
- **Spring Kafka** - Kafka integration
- **Apache Kafka** - Message broker

## Getting Started

### 1. Start Kafka with Docker

Create a `docker-compose.yml` file:

```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

Start Kafka:
```bash
docker-compose up -d
```

### 2. Create Kafka Topics

```bash
# Create main orders topic
kafka-topics --create --topic orders --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

# Create regional subtopics  
kafka-topics --create --topic orders.us --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
kafka-topics --create --topic orders.eu --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
kafka-topics --create --topic orders.other --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

# Verify topics created
kafka-topics --list --bootstrap-server localhost:9092
```

### 3. Build and Run the Application

```bash
# Build the application
./mvnw clean package

# Run the application
./mvnw spring-boot:run
```

The application will start and automatically begin consuming from the `orders` topic.

## How the Routing Works

The routing logic is implemented in `OrderRoutingRoute.java` using Camel's Content-Based Router pattern:

```java
from("kafka:orders?brokers=localhost:9092&groupId=order-processor")
    .choice()
    .when(simple("${body} contains 'us'"))
    .to("kafka:orders.us?brokers=localhost:9092")
    .when(simple("${body} contains 'eu'"))
    .to("kafka:orders.eu?brokers=localhost:9092")
    .otherwise()
    .to("kafka:orders.other?brokers=localhost:9092");
```

This Camel route:
- Consumes messages from `orders` topic with consumer group `order-processor`
- Uses Camel's Choice component for conditional routing
- Routes based on message content using Simple language expressions
- Publishes to appropriate regional topics

## Testing the Routing

### Send Test Messages

Open a Kafka producer in your terminal:
```bash
kafka-console-producer --broker-list localhost:9092 --topic orders
```

Send these test messages (one per line):
```
order1-us-12345
order2-eu-67890
order3-india-54321
order4-canada-98765
special-us-offer
eu-customer-order
```

### Verify Routing

In separate terminal windows, check each topic to confirm routing:

**Check US orders:**
```bash
kafka-console-consumer --bootstrap-server localhost:9092 --topic orders.us --from-beginning
```
Expected output:
```
order1-us-12345
special-us-offer
```

**Check EU orders:**
```bash
kafka-console-consumer --bootstrap-server localhost:9092 --topic orders.eu --from-beginning  
```
Expected output:
```
order2-eu-67890
eu-customer-order
```

**Check other orders:**
```bash
kafka-console-consumer --bootstrap-server localhost:9092 --topic orders.other --from-beginning
```
Expected output:
```
order3-india-54321
order4-canada-98765
```