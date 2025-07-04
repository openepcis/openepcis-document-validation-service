# openepcis-document-validation-service

An open-source service to validate EPCIS documents ensuring compliance with EPCIS standards.

## Introduction

The `openepcis-document-validation-service` is designed to validate EPCIS documents to ensure they meet the required standards. EPCIS (Electronic Product Code Information Services) facilitates the exchange of visibility data within and across enterprises.

## Key Features

- Validates EPCIS documents against EPCIS 2.0 standards.
- Supports both XML and JSON/JSON-LD formats.
- Provides detailed validation error messages.
- RESTful API for easy integration.

## Getting Started

### Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- Docker (optional, for containerized deployment)

### Installation

1. Clone the repository:
    ```sh
    git clone https://github.com/openepcis/openepcis-document-validation-service.git
    cd openepcis-document-validation-service
    ```

2. Build the project using Maven:
    ```sh
    mvn clean install
    ```
