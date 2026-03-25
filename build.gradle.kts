plugins {
    java
    id("io.quarkus") version "3.34.1"
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://packages.confluent.io/maven/")
}

group = "com.github.imcf"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation(platform("io.quarkiverse.mcp:quarkus-mcp-server-bom:1.11.0"))

    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkiverse.mcp:quarkus-mcp-server-stdio")
    implementation("io.quarkiverse.mcp:quarkus-mcp-server-sse")
    implementation("org.apache.kafka:kafka-clients:4.2.0")
    implementation("io.quarkus:quarkus-rest-client-jackson")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("io.confluent:kafka-schema-registry-client:8.2.0")
    implementation("io.confluent:kafka-avro-serializer:8.2.0")
    implementation("io.confluent:kafka-json-schema-serializer:8.2.0")
    implementation("io.confluent:kafka-protobuf-serializer:8.2.0")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("org.testcontainers:kafka:2.0.1")
    testImplementation("org.testcontainers:junit-jupiter:2.0.1")
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}
