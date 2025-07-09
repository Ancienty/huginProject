# Hugin Invoice Management System

A Java-based invoice management system with desktop GUI and server-client architecture.

## Features

- **Desktop Application**: Create, manage, and export invoices
- **Server Application**: HTTP and TCP services for invoice upload and query
- **Database**: SQLite-based data storage
- **Export**: XML and JSON export capabilities using Jackson libraries

## Components

- **Client**: Desktop GUI for invoice management
- **Server**: HTTP/TCP server for remote operations
- **Database**: SQLite database with customer, item, and invoice tables

## Technology Stack

- Java 21
- Swing GUI
- SQLite Database
- Jackson XML/JSON libraries
- Maven build system

## Running the Application

1. **Client**: Run `com.ancienty.Main`
2. **Server**: Run `com.ancienty.server.ServerMain`

The system supports both local desktop operations and remote server-client communication for invoice management.
