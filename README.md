# Kafka Streams Dynamic Routing POC

A simple Spring Boot application demonstrating **dynamic message routing using Kafka Streams** - similar to RabbitMQ topic exchanges. Messages from a main `orders` topic are automatically routed to regional subtopics based on message content.

## What This Does

This POC shows how to achieve **RabbitMQ-style topic routing** using Kafka Streams:
- Messages sent to `orders` topic are automatically filtered and routed
- Messages containing "us" → `orders.us`
- Messages containing "eu" → `orders.eu`  
- All other messages → `orders.other`

## Architecture

```
orders (main topic)
    ↓ [Kafka Streams Processing]
    ├── orders.us    (US region orders)
    ├── orders.eu    (EU region orders) 
    └── orders.other (All other orders)
```

### 1. Start Kafka with Docker

```yaml
# docker-compose.yml
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

### 3. Run the Application

## Testing the Routing

### Send Test Messages

Open a producer in your terminal:
```bash
kafka-console-producer --broker-list localhost:9092 --topic orders
```

Send these test messages (one per line):
```
order1-us
order2-eu
order3-in
order4-canada
```

### Verify Routing

In separate terminal windows, check each topic to confirm routing:

**Check US orders:**
```bash
kafka-console-consumer --bootstrap-server localhost:9092 --topic orders.us --from-beginning
```
Should show: `order1-us`

**Check EU orders:**
```bash
kafka-console-consumer --bootstrap-server localhost:9092 --topic orders.eu --from-beginning  
```
Should show: `order2-eu`

**Check other orders:**
```bash
kafka-console-consumer --bootstrap-server localhost:9092 --topic orders.other --from-beginning
```
Should show: `order3-in` and `order4-canada`
