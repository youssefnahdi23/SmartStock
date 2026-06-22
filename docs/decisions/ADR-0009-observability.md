# ADR-0009: Observability Strategy - Structured Logging, Metrics, and Distributed Tracing

## Status
Accepted

## Context
SmartStock AI is a distributed microservices system with 12+ services, Kafka messaging, and complex data flows. When issues occur, the system must be observable to:
- **Diagnose Issues**: Track requests across multiple services
- **Monitor Health**: Know when services are degraded before users complain
- **Understand Performance**: Identify bottlenecks and optimization opportunities
- **Support Compliance**: Audit trails for regulatory requirements
- **Debug Operations**: Root cause analysis for production incidents

The three pillars of observability are:
1. **Structured Logging**: Immutable records of events with full context
2. **Metrics**: Time-series measurements of system behavior
3. **Distributed Tracing**: Trace requests across service boundaries

Each pillar addresses different questions:
- **Logs**: "What happened? Why did it fail?"
- **Metrics**: "Is the system healthy? What's the throughput?"
- **Traces**: "How long did the request take? Which service was slow?"

Without observability strategy, debugging production issues is nearly impossible in microservices.

## Decision
Implement **Three-Pillar Observability** architecture:

### 1. **Structured Logging (Pillar 1)**

**Strategy**: Send all logs to centralized logging system
- Tool: ELK Stack (Elasticsearch, Logstash, Kibana) or Grafana Loki
- Format: JSON for easy parsing and searching
- Retention: 30 days for application logs, 90 days for security logs

**Log Contents**
Every log entry includes:
```json
{
  "timestamp": "2026-06-20T12:00:00.123Z",
  "level": "INFO|WARN|ERROR|DEBUG",
  "service": "inventory-service",
  "correlationId": "req-abc123",
  "userId": "user-456",
  "requestId": "request-789xyz",
  "traceId": "trace-abc123def456",
  "message": "Stock in received from supplier S99",
  "context": {
    "productId": "P001",
    "warehouseId": "W01",
    "quantity": 100,
    "supplierId": "S99"
  },
  "error": null,
  "stackTrace": null
}
```

**Logging Best Practices**
- Never log passwords, tokens, API keys
- PII (personally identifiable info) masked or redacted
- Correlation ID passed through all service calls
- Every error includes stack trace and user context
- Structured context fields for easy searching
- Log at entry/exit of important operations

**Log Levels**
- **ERROR**: Application errors, failures, exceptions (requires action)
- **WARN**: Potentially problematic situations, degraded performance
- **INFO**: Important business events (stock movements, user actions)
- **DEBUG**: Detailed diagnostic information (method parameters, state changes)
- **TRACE**: Very detailed debugging (not enabled in production)

### 2. **Metrics (Pillar 2)**

**Strategy**: Expose metrics in Prometheus format; visualize in Grafana
- Tool: Prometheus for metrics collection
- Scrape Interval: 30 seconds
- Retention: 15 days (data automatically aged out)

**Key Metrics to Track**

**System Health**
- Service up/down status
- JVM memory usage (heap, non-heap)
- JVM thread count
- GC pause duration
- Database connection pool availability

**Application Metrics**
- HTTP requests per second (by endpoint, method, status code)
- Request latency (p50, p95, p99 percentiles)
- Request error rate (by error type)
- Active user sessions
- Event processing lag (Kafka consumer offset lag)

**Business Metrics**
- Stock in transactions per second
- Stock out transactions per second
- Order fulfillment rate
- Inventory count accuracy
- Data export success rate

**Metric Export Format**
```
# HELP smartstock_http_requests_total Total HTTP requests
# TYPE smartstock_http_requests_total counter
smartstock_http_requests_total{service="inventory", method="POST", endpoint="/adjust", status="200"} 1523

# HELP smartstock_http_request_duration_seconds HTTP request duration
# TYPE smartstock_http_request_duration_seconds histogram
smartstock_http_request_duration_seconds_bucket{service="inventory", endpoint="/adjust", le="0.1"} 1200
smartstock_http_request_duration_seconds_bucket{service="inventory", endpoint="/adjust", le="0.5"} 1500
smartstock_http_request_duration_seconds_bucket{service="inventory", endpoint="/adjust", le="1.0"} 1520
smartstock_http_request_duration_seconds_sum{service="inventory", endpoint="/adjust"} 425.5
smartstock_http_request_duration_seconds_count{service="inventory", endpoint="/adjust"} 1523
```

### 3. **Distributed Tracing (Pillar 3)**

**Strategy**: Trace requests across services using OpenTelemetry
- Tool: Jaeger or Zipkin for visualization
- Sampling Rate: 10% of requests (balance cost vs. visibility)
- Retention: 48 hours (enough to diagnose issues)

**Trace Structure**
```
Trace ID: abc123def456
├─ Span: API Gateway (0ms - 250ms)
│  ├─ Span: Inventory Service (5ms - 100ms)
│  │  ├─ Span: Database Query (10ms - 45ms)
│  │  ├─ Span: Kafka Event Publish (50ms - 65ms)
│  │  └─ Span: Cache Update (70ms - 95ms)
│  ├─ Span: Warehouse Service (105ms - 180ms)
│  │  ├─ Span: Database Query (110ms - 140ms)
│  │  └─ Span: Kafka Event Publish (145ms - 175ms)
│  └─ Span: Notification Service (185ms - 240ms)
│     └─ Span: Email Service (190ms - 235ms)
└─ Total: 250ms
```

**Trace Context Propagation**
Every HTTP request includes trace headers:
```
X-Trace-ID: abc123def456        -- Unique trace identifier
X-Span-ID: inventory-span-001   -- Current span
X-Parent-Span-ID: gateway-span  -- Parent span
X-Sampled: 1                    -- Should be sampled (1) or not (0)
```

### 4. **Alert Rules**

**Critical Alerts** (Page on-call engineer)
- Any service down (health check failing)
- Error rate > 5% for 5 minutes
- Response latency p99 > 1 second for 10 minutes
- Kafka consumer lag > 30 seconds
- Database CPU > 85% for 10 minutes

**Warning Alerts** (Slack notification)
- Error rate > 2% for 5 minutes
- Response latency p95 > 500ms
- Kafka consumer lag > 10 seconds
- Memory usage > 75%
- Disk space < 10% free

### 5. **Dashboards**

**Operations Dashboard** (for operations team)
- Service health status (all services green/yellow/red)
- Requests per second (total and by service)
- Error rate (total and by service)
- Response latency distribution
- Kafka consumer lag
- Database resource usage

**Business Dashboard** (for business/warehouse managers)
- Transactions today (stock in, stock out, adjustments)
- Inventory accuracy (physical vs. system)
- Top products (most moved)
- Warehouse utilization rates
- Export success rate

**Platform Dashboard** (for DevOps/SREs)
- All infrastructure metrics
- Service restart count
- Network throughput
- Storage usage trends
- Cost tracking (if cloud-based)

## Alternatives Considered

### Option 1: No Centralized Logging
Each service logs to local files; no aggregation

**Pros:**
- Simpler initially
- No additional infrastructure

**Cons:**
- Impossible to search logs across services
- Difficult to trace requests across service boundaries
- No historical logs after service restart
- High operational burden (SSH into each server)
- Cannot correlate events across services

### Option 2: Commercial SaaS (Datadog, New Relic, Splunk)
Outsource observability to cloud provider

**Pros:**
- Fully managed (no operational burden)
- Excellent UI and advanced features
- Enterprise support

**Cons:**
- High cost at scale
- Data privacy concerns (logs sent to cloud)
- Vendor lock-in
- Overkill for current phase

### Option 3: Minimal Observability (Logs Only)
Implement structured logging but no metrics or tracing

**Pros:**
- Simpler to implement
- Cheaper than full observability

**Cons:**
- Cannot detect issues proactively (only reactive)
- Difficult to diagnose performance issues
- No visibility into system health
- Cannot track trends over time

## Consequences

### Positive
- **Rapid Incident Response**: Trace root cause within minutes
- **Proactive Alerting**: Know about issues before users complain
- **Performance Optimization**: Identify bottlenecks via metrics and traces
- **Audit Trail**: Complete history of all operations for compliance
- **Developer Productivity**: Quick debugging with full context
- **System Visibility**: Understand system behavior and dependencies
- **Cost Optimization**: Identify wasteful resources and over-provisioned services
- **SLA Compliance**: Track SLAs and demonstrate compliance to customers

### Negative
- **Infrastructure Overhead**: Logging, metrics, and tracing require CPU/memory
- **Storage Costs**: Log retention requires disk space and backup
- **Network Overhead**: Sending logs and metrics consumes bandwidth
- **Operational Complexity**: Multiple systems to learn and maintain
- **Privacy Concerns**: Centralized logs contain sensitive data
- **Latency**: Tracing adds latency to critical path (mitigated by sampling)
- **Debugging Overhead**: So much data available; requires skill to interpret

### Trade-offs
- **Visibility vs. Overhead**: Accept logging overhead for complete visibility
- **Cost vs. Reliability**: Invest in observability infrastructure for operational reliability
- **Privacy vs. Debuggability**: Accept data privacy trade-off for production debugging capability

## Future Considerations

1. **Machine Learning Anomaly Detection**: Automatic detection of anomalies
   - Learn normal behavior patterns
   - Alert on deviation (potential issues)
   - Reduce false positives

2. **Correlation Analysis**: Identify patterns across logs, metrics, traces
   - Which logs correlate with performance degradation?
   - Which services always fail together?
   - Impact analysis of changes

3. **Cost Attribution**: Track costs by service, team, customer
   - Cloud costs allocated to services
   - Identify expensive operations
   - Chargeback to business units

4. **Advanced Sampling**: Smart sampling based on request characteristics
   - Sample errors at 100%, normal requests at 10%
   - Adjust sampling dynamically based on load
   - Always collect traces for slow requests

5. **Custom Metrics**: Business-specific metrics
   - Inventory accuracy metrics
   - Supplier performance scores
   - Warehouse efficiency ratings

6. **Continuous Profiling**: Deep performance analysis
   - CPU flame graphs
   - Memory allocation patterns
   - Lock contention detection

## Implementation Guidance

- Use Spring Cloud Sleuth for trace context propagation
- Use Micrometer for metrics abstraction
- Use Slf4j with structured logging libraries (Logback, Log4j2)
- All services expose `/actuator/health` and `/actuator/prometheus` endpoints
- Logs shipped to Elasticsearch via Logstash or Fluentd
- Alerts configured in Prometheus AlertManager
- On-call rotation uses PagerDuty for critical alerts
- Weekly review of alert effectiveness; disable noisy alerts
- Dashboards documented and version-controlled
- Observability requirements included in service definition
