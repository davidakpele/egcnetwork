# EGC Network - Geofencing System

Build complete Geofencing network system

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Architecture](#architecture)
- [API Documentation](#api-documentation)
- [Database](#database)
- [Message Queue](#message-queue)
- [Security](#security)
- [Development](#development)
- [Docker Support](#docker-support)
- [Contributing](#contributing)
- [License](#license)

## 🎯 Overview

**egcnetwork** is a complete geofencing network system built with modern Java technologies. It provides a robust platform for managing geographical zones, monitoring location-based events, and handling real-time geofencing operations through a distributed system architecture.

The system leverages Spring Boot 4.0.6, gRPC, Kafka, PostgreSQL with PostGIS, Redis, and Neo4j to create a scalable, enterprise-grade geofencing solution.

**Repository**: [davidakpele/egcnetwork](https://github.com/davidakpele/egcnetwork)  
**Owner**: davidakpele  
**Created**: 2026-05-08  
**Language**: Java (100%)  
**Status**: Active Development

## ✨ Features

- **Geofencing Management**: Create, update, and manage geographical zones with spatial data
- **Real-time Monitoring**: Track location-based events and boundary crossings
- **gRPC Communication**: High-performance inter-service communication using Protocol Buffers
- **Event Streaming**: Kafka-based event processing and distribution
- **OAuth2 Authorization**: Comprehensive security with OAuth2 and JWT tokens
- **Multi-Database Support**: PostgreSQL with PostGIS for spatial queries, Neo4j for graph relationships, Cassandra for time-series data
- **Session Management**: Redis-backed session storage
- **WebSocket Support**: Real-time bidirectional communication
- **Web Interface**: Thymeleaf-based template engine for UI rendering
- **API Error Handling**: Standardized error responses across all APIs using Zalando Problem Spring Web

## 🛠️ Technology Stack

### Core Framework
- **Spring Boot**: 4.0.6
- **Java**: 21
- **Maven**: Build automation

### Data Access & Storage
- **Spring Data JPA**: ORM and database abstraction
- **Spring Data Neo4j**: Graph database integration
- **Spring Data Cassandra**: NoSQL time-series data
- **PostgreSQL**: Primary relational database with PostGIS extension
- **Redis**: Session management and caching (v7)
- **PostGIS**: Spatial and geographic data support (v16-3.4)

### Communication & Streaming
- **gRPC**: 1.77.1 - High-performance RPC framework
- **Protocol Buffers**: 4.33.4 - Data serialization
- **Spring gRPC**: 1.0.3 - Spring integration for gRPC
- **Apache Kafka**: 7.5.0 - Event streaming and messaging
- **Spring Kafka**: Message producer/consumer support

### Security & Authentication
- **Spring Security**: Core security framework
- **Spring Security OAuth2 Authorization Server**: OAuth2 provider implementation
- **JWT (JJWT)**: 0.12.3 - JSON Web Token handling
- **Spring Security OAuth2 Resource Server**: OAuth2 resource protection

### Web & UI
- **Spring Web MVC**: RESTful API development
- **Thymeleaf**: Server-side template engine
- **WebSocket Support**: Real-time communication
- **Spring Session Data Redis**: Distributed session management

### Development Tools
- **Lombok**: Boilerplate code reduction
- **MapStruct**: 1.4.2 - Object mapping and transformation
- **Zalando Problem Spring Web**: 0.27.0 - Standardized error responses
- **Spring Boot DevTools**: Development efficiency
- **Protobuf Maven Plugin**: 4.0.3 - Protocol Buffer compilation

### Testing
- **Spring Boot Test Suite**: Comprehensive test support
- **Cassandra Test Support**: Embedded Cassandra for testing
- **Neo4j Test Support**: Test database support
- **JPA Test Support**: JPA-specific testing utilities
- **Security Test Support**: Security context testing
- **WebMvc Test Support**: Mock MVC for API testing
- **gRPC Test Support**: gRPC testing utilities

### Spatial Processing
- **JTS Core**: 1.19.0 - Java Topology Suite for geometric operations
- **Spring Boot Validation**: Bean validation with Hibernate Validator

## 📁 Project Structure
