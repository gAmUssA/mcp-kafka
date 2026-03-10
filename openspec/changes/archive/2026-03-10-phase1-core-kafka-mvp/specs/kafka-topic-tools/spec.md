## ADDED Requirements

### Requirement: list-topics tool
The server SHALL provide a `list-topics` MCP tool that lists all topics in the Kafka cluster. It SHALL return topic names as a CSV string (e.g., `"topic1,topic2,topic3"`).

#### Scenario: List topics on a cluster with topics
- **WHEN** the `list-topics` tool is called on a cluster with topics "orders", "users", "events"
- **THEN** the tool returns a CSV string containing all three topic names

#### Scenario: List topics on an empty cluster
- **WHEN** the `list-topics` tool is called on a cluster with no user topics
- **THEN** the tool returns an empty string or a string with only internal topics excluded

### Requirement: create-topics tool
The server SHALL provide a `create-topics` MCP tool that creates one or more Kafka topics. It SHALL accept `topicNames` (string array, required), `numPartitions` (number, optional, default 1), and `replicationFactor` (number, optional, default cluster default).

#### Scenario: Create a single topic
- **WHEN** `create-topics` is called with `topicNames: ["my-topic"]`
- **THEN** the topic "my-topic" is created with 1 partition and the tool returns a success message

#### Scenario: Create multiple topics with custom partitions
- **WHEN** `create-topics` is called with `topicNames: ["topic-a", "topic-b"], numPartitions: 3`
- **THEN** both topics are created with 3 partitions each

#### Scenario: Create topic that already exists
- **WHEN** `create-topics` is called with a topic name that already exists
- **THEN** the tool returns an error response indicating the topic already exists

### Requirement: delete-topics tool
The server SHALL provide a `delete-topics` MCP tool that deletes topics by name. It SHALL accept `topicNames` (string array, required).

#### Scenario: Delete existing topics
- **WHEN** `delete-topics` is called with `topicNames: ["my-topic"]`
- **THEN** the topic is deleted and the tool returns a confirmation message

#### Scenario: Delete non-existent topic
- **WHEN** `delete-topics` is called with a topic name that does not exist
- **THEN** the tool returns an error response

### Requirement: produce-message tool
The server SHALL provide a `produce-message` MCP tool that produces records to a Kafka topic. It SHALL accept `topicName` (string, required), `value` (object with `message` field, required), `key` (object, optional), `headers` (object, optional), and `partition` (number, optional).

#### Scenario: Produce a simple string message
- **WHEN** `produce-message` is called with `topicName: "test-topic", value: { message: "hello" }`
- **THEN** the message is produced and the tool returns a delivery report with topic, partition, and offset

#### Scenario: Produce a message with key and headers
- **WHEN** `produce-message` is called with a key, headers, and value
- **THEN** the message is produced with the specified key and headers

### Requirement: consume-messages tool
The server SHALL provide a `consume-messages` MCP tool that consumes messages from one or more Kafka topics. It SHALL accept `topicNames` (string array, required), `maxMessages` (number, optional, default 10), `timeoutMs` (number, optional, default 10000), and `fromBeginning` (boolean, optional, default true).

#### Scenario: Consume messages from a topic
- **WHEN** `consume-messages` is called with `topicNames: ["test-topic"]` on a topic with 5 messages
- **THEN** the tool returns a JSON array of up to 5 message objects with key, value, partition, offset, and timestamp

#### Scenario: Consume with timeout on empty topic
- **WHEN** `consume-messages` is called on an empty topic with `timeoutMs: 1000`
- **THEN** the tool returns an empty array after the timeout expires

### Requirement: get-topic-config tool
The server SHALL provide a `get-topic-config` MCP tool that retrieves configuration for a specific topic. It SHALL accept `topicName` (string, required).

#### Scenario: Get config for existing topic
- **WHEN** `get-topic-config` is called with `topicName: "my-topic"`
- **THEN** the tool returns topic configuration parameters with their values, sources, and read-only/sensitive flags

### Requirement: alter-topic-config tool
The server SHALL provide an `alter-topic-config` MCP tool that modifies topic configuration. It SHALL accept `topicName` (string, required), `topicConfigs` (array of objects with `name`, `value`, `operation` fields, required), and `validateOnly` (boolean, optional, default false).

#### Scenario: Set retention.ms on a topic
- **WHEN** `alter-topic-config` is called with `topicName: "my-topic", topicConfigs: [{ name: "retention.ms", value: "86400000", operation: "SET" }]`
- **THEN** the topic's retention.ms is updated to 86400000

#### Scenario: Validate-only mode
- **WHEN** `alter-topic-config` is called with `validateOnly: true`
- **THEN** the configuration change is validated but not applied

#### Scenario: Delete a config override
- **WHEN** `alter-topic-config` is called with `operation: "DELETE"` for a config key
- **THEN** the config key reverts to the cluster default
