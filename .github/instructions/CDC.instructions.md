1. Cahier de Charge — SmartStock AI Platform
Author: Youssef Nahdi
This is your official product specification document (what companies actually write before
coding).
1. Project Overview
Project Name: SmartStock AI
Type: Enterprise Inventory & Supply Chain Intelligence Platform
Architecture: Microservices + Event-Driven + Data Platform
Clients: Desktop Application (JavaFX) + REST APIs
Goal: Build a scalable inventory system that evolves into an AI-powered decision platform.
2. Objectives
Primary Objective
Design and implement a modular inventory system capable of:
• Managing products, warehouses, suppliers, customers
• Tracking real-time stock movements
• Generating audit logs for every transaction
• Supporting multi-warehouse operations
• Producing structured, high-quality data for future AI systems
Secondary Objective (Future Phase)
• Demand forecasting
• Stock optimization
• Supplier performance analysis
• Predictive replenishment
• AI-driven logistics recommendations
3. System Architecture
3.1 High-Level Architecture
[ JavaFX Desktop App ]
↓
[ API Gateway ]
↓
┌───────────────────────────────┐
│ Microservices Layer │
│ │
│ Identity Service │
│ Product Service │
│ Inventory Service │
│ Warehouse Service │
│ Supplier Service │
│ Customer Service │
│ Order Service │
│ Audit Service │
│ Notification Service │
│ Reporting Service │
│ Data Export Service │
│ Analytics Service (non-AI) │
└───────────────────────────────┘
↓
┌───────────────────────────────┐
│ Infrastructure Layer │
│ PostgreSQL (per service) │
│ Redis (cache/session) │
│ RabbitMQ / Kafka (events) │
│ Object Storage (MinIO) │
└───────────────────────────────┘
3.2 Communication Model
• Synchronous: REST APIs (Spring Boot)
• Asynchronous: Event-driven messaging
Events examples:
• ProductCreated
• StockUpdated
• StockMoved
• SupplierUpdated
• OrderPlaced
• InventoryAdjusted
4. Data Strategy (MOST IMPORTANT PART)
This is where your AI future is built.
Every action generates structured events:
{
"eventType": "StockIn",
"productId": "P001",
"warehouseId": "W01",
"quantity": 50,
"timestamp": "2026-06-19T10:00:00Z",
"userId": "U123",
"unitCost": 12.5,
"supplierId": "S99"
}
Data Layers:
1. Operational Database
• PostgreSQL per microservice
2. Event Bus
• Kafka / RabbitMQ
3. Data Lake (Future AI layer)
• MinIO / S3 compatible storage
4. Analytics Store
• Aggregated tables for dashboards
5. Functional Requirements
5.1 Inventory Module
• Stock In / Out
• Stock Transfers
• Stock Adjustments
• Damage tracking
• Stock history tracking
5.2 Product Module
• CRUD products
• Barcode generation
• QR code generation
• Product categorization
• Import/Export CSV
5.3 Warehouse Module
• Multi-warehouse support
• Zones → Shelves → Bins hierarchy
• Capacity tracking
• Transfer workflows
5.4 Supplier Module
• Supplier profiles
• Purchase history
• Delivery performance tracking
5.5 Audit Module
• Every action logged
• Immutable logs
• Compliance-ready system
5.6 Reporting Module
• Inventory reports
• Warehouse utilization
• Stock aging
• Export PDF / Excel / CSV
5.7 Notification Module
• Low stock alerts
• Email notifications (Brevo API)
• System alerts
6. Security Requirements
• JWT authentication
• Role-based access control (RBAC)
• BCrypt password hashing
• Rate limiting
• Audit logs
• Secure API Gateway
• HTTPS mandatory
7. Non-Functional Requirements
• Horizontal scalability
• Fault tolerance
• High availability (Enterprise tier)
• Offline desktop caching
• Event-driven resilience
• Low latency API responses (<200ms target)
8. AI Readiness Strategy (NO AI YET)
Instead of building AI now:
You build DATA INFRASTRUCTURE
You must collect:
• Time-series inventory changes
• Supplier reliability data
• Seasonal patterns
• Stock movement patterns
• Warehouse performance data
AI will later consume:
• Feature Store (future)
• Data Lake (MinIO)
• Clean event logs
• Aggregated KPIs
9. Deployment Strategy
Basic
• Docker Compose
• Single server
Professional
• Docker + monitoring
• Prometheus + Grafana
Enterprise
• Kubernetes cluster
• Helm charts
• Load balancing
• Auto-scaling
10. Observability
• Prometheus metrics
• Grafana dashboards
• Centralized logging (Loki / ELK)
• Distributed tracing (OpenTelemetry)
11. Data Export Service (CRITICAL FOR AI FUTURE)
Exports:
• Daily inventory snapshots
• Historical stock movements
• Supplier performance datasets
• Warehouse efficiency reports
Formats:
• CSV
• JSON
• Parquet (for AI later)
2. AI SYSTEM PROMPT (FOR FUTURE AI ENGINE)
This is NOT for now — but this is what your AI model will use later.
SmartStock AI Engine Prompt
You are SmartStock AI, an enterprise inventory intelligence engine.
You analyze structured inventory data from a microservices-based enterprise system.
Your goal is to:
1. Predict future product demand
2. Detect inventory risks
3. Optimize warehouse operations
4. Improve supplier efficiency
5. Recommend reorder quantities and timing
6. Detect anomalies in stock movements
7. Provide explainable business insights
You MUST:
- Use only provided structured data
- Never assume missing data
- Always provide confidence scores
- Always explain reasoning
- Prefer statistical + ML-based reasoning over assumptions
---
INPUT DATA TYPES:
- stock_movements
- product_metadata
- warehouse_data
- supplier_data
- sales_history
- time_series_inventory_levels
---
OUTPUT FORMAT:
{
"prediction_type": "...",
"result": {...},
"confidence": 0.0 - 1.0,
"explanation": "...",
"business_impact": "...",
"recommended_actions": [...]
}
---
IMPORTANT RULES:
- No hallucinations
- No guessing missing values
- Always state uncertainty
- Prefer interpretable models when possible
- Output must be structured JSON
3. Final Engineering Advice (Important)
If you build this correctly:
Phase 1 = Data Generation Machine
Your system becomes a factory of structured business data
Phase 2 = Analytics Layer
You understand the business
Phase 3 = AI Layer
You predict the business