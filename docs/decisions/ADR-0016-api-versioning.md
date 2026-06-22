# ADR-0016: API Versioning and Backward Compatibility Strategy

## Status
Accepted

## Context
SmartStock AI is an enterprise platform with multiple clients:
- Desktop application (JavaFX)
- Future web portal
- Future mobile apps
- Third-party integrations

APIs evolve as features change. If API contracts break unexpectedly:
- **Clients Crash**: Old clients can't understand new response format
- **Data Loss**: Clients fail to parse critical fields
- **Outages**: Forced client updates cause downtime
- **User Frustration**: Customers must upgrade all devices

Without versioning strategy:
- Cannot deploy API changes
- Cannot add features safely
- Breaks third-party integrations

The challenge is evolving APIs safely while maintaining backward compatibility.

## Decision
Implement **Semantic Versioning with Multiple API Versions in Production**:

### 1. **Versioning Strategy**

**URL-Based Versioning**
```
/api/v1/products              -- Version 1 (stable, maintained)
/api/v2/products              -- Version 2 (current development)
/api/v3/products              -- Version 3 (future)
```

**Rationale for URL versioning**:
- Clear in URLs (easy to debug)
- Easy to route in API Gateway
- Version visible in logs and monitoring
- Headers can be ambiguous

### 2. **Version Lifecycle**

**Version 1 (Live)**
- Fully supported
- Backward compatible changes only
- Bug fixes only
- Will be maintained for 2 years minimum

**Version 2 (Current)**
- Active development
- Latest features
- Breaking changes allowed (new major version)
- Gradually phase out V1 features

**Version 3+ (Deprecated)**
- Scheduled for sunset
- Clients should migrate
- 6-month notice before removal
- Bug fixes only if critical

### 3. **Breaking Changes (Examples)**

**Breaking Changes** (require new version)
```
❌ Removing a field: { "id": "123", "name": "Widget", "sku": "SKU" }
                     → { "id": "123", "name": "Widget" }
   (clients expect "sku" field)

❌ Changing field type: { "price": "9.99" }
                       → { "price": 9.99 }
   (number vs string; parsers break)

❌ Renaming fields: { "productId": "123" }
                   → { "id": "123" }
   (clients look for "productId")

❌ Changing HTTP method: POST /products → GET /products
   (clients expect POST)

❌ Moving endpoint: /products → /api/products
   (URLs change)
```

**Non-Breaking Changes** (can update existing version)
```
✅ Adding optional field: { "id": "123", "name": "Widget" }
                         → { "id": "123", "name": "Widget", "description": "..." }
   (clients ignore unknown fields)

✅ Adding new endpoint: POST /products/123/reviews
   (new endpoint, doesn't affect existing clients)

✅ Adding request option: POST /products?include=reviews
   (optional parameter, backward compatible)

✅ Making required field optional: { "name": "Widget" } 
                                  → { "name": "Widget" (optional) }
   (clients still send it, works fine)

✅ Adding new HTTP status code: 202 Accepted (in addition to 200)
   (clients handle new status gracefully)
```

### 4. **Implementation Pattern**

**Single Codebase, Multiple Versions**
```
src/
├── main/java/
│   └── com/smartstock/inventory/
│       ├── application/
│       │   └── InventoryService.java
│       ├── domain/
│       ├── infrastructure/
│       └── interfaces/
│           ├── api/v1/
│           │   ├── ProductControllerV1.java
│           │   └── ProductDTOV1.java
│           ├── api/v2/
│           │   ├── ProductControllerV2.java
│           │   └── ProductDTOV2.java
│           └── mapper/
│               ├── ProductMapperV1.java
│               └── ProductMapperV2.java
```

**Example: Breaking Change in V2**
```java
// Version 1: Original API
@RestController
@RequestMapping("/api/v1/products")
public class ProductControllerV1 {
  @GetMapping("/{id}")
  public ProductDTOV1 getProduct(@PathVariable UUID id) {
    Product domain = service.getProduct(id);
    return mapperV1.toDtoV1(domain);
  }
}

public record ProductDTOV1(
  UUID id,
  String name,
  String sku,
  BigDecimal price,
  String category  // String in V1
) {}

// Version 2: Add category as object (breaking change)
@RestController
@RequestMapping("/api/v2/products")
public class ProductControllerV2 {
  @GetMapping("/{id}")
  public ProductDTOV2 getProduct(@PathVariable UUID id) {
    Product domain = service.getProduct(id);
    return mapperV2.toDtoV2(domain);
  }
}

public record ProductDTOV2(
  UUID id,
  String name,
  String sku,
  BigDecimal price,
  CategoryDTO category  // Object in V2 (breaking change!)
) {}

public record CategoryDTO(
  UUID id,
  String name
) {}
```

### 5. **Deprecation Process**

**Phase 1: Announce (Month 1)**
- Documentation: "V1 will be sunset in 12 months (June 2027)"
- Response Header: `Deprecation: true`
- Response Header: `Sunset: June 20, 2027`
- Redirect: Link to migration guide

**Phase 2: Encourage (Months 2-9)**
- Email customers about upgrade
- Provide migration tools/utilities
- Offer workshops/webinars
- Monitor V1 usage; identify stragglers

**Phase 3: Final Warning (Months 10-11)**
- Last notice: "Sunset in 60 days"
- Escalate support for migrations
- Reduce support response time for new version

**Phase 4: Sunset (Month 12)**
- Remove V1 endpoints
- Clients on V1 experience 410 Gone
- Support team handles requests

### 6. **Response Format Consistency**

**All versions use same response envelope**
```json
{
  "data": {
    "id": "prod-123",
    "name": "Widget",
    ...version-specific fields...
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "version": "1.0.0",
    "deprecation": null or "Deprecated, sunset June 20, 2027"
  }
}

{
  "errors": [
    {
      "code": "INVALID_INPUT",
      "message": "Product name required",
      "field": "name"
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "abc123"
  }
}
```

## Alternatives Considered

### Option 1: URL Versioning
```/api/v1/products, /api/v2/products```

**Pros:**
- Explicit in URLs
- Easy to route at API Gateway
- Clear in logs and monitoring

**Cons:**
- Requires managing multiple controllers
- URL duplication

✅ **Selected**

### Option 2: Header-Based Versioning
```Accept: application/vnd.smartstock+json;version=1```

**Pros:**
- Cleaner URLs
- Version inside header

**Cons:**
- Not visible in browser/logs
- Harder to route
- Confusing for debugging

### Option 3: Query Parameter Versioning
```/api/products?version=1```

**Pros:**
- URLs relatively clean

**Cons:**
- Version easily missed
- Not standard practice
- Harder to route

## Consequences

### Positive
- **Backward Compatibility**: Old clients continue working
- **Safe Evolution**: New versions don't break existing clients
- **Gradual Migration**: Clients migrate at their own pace
- **Clear Communication**: Deprecation timeline transparent
- **Rollback Capability**: Can disable new version if issues found
- **Multi-Client Support**: Different clients use different versions

### Negative
- **Operational Burden**: Multiple versions to maintain
- **Code Duplication**: Version-specific DTOs and controllers
- **Storage Overhead**: Multiple versions of data structures
- **Testing Burden**: Must test all supported versions
- **Client Confusion**: Which version should I use?

### Trade-offs
- **Maintenance Burden vs. Stability**: Accept multiple versions for stability
- **Code Duplication vs. Compatibility**: Accept duplication for backward compatibility

## Future Considerations

1. **Facade Approach**: Hide version differences
   - Internal service stays single version
   - Adapters translate between versions
   - Reduce code duplication

2. **Automated Version Management**: Generate versions from API spec
   - Detect breaking changes automatically
   - Generate migration guides
   - Generate client SDK for each version

3. **Client SDK Versioning**: Provide official client libraries
   - SDKs handle version negotiation
   - Automatic version upgrade suggestions

4. **Metrics by Version**: Track API usage per version
   - Monitor adoption of new versions
   - Identify stragglers on old versions
   - Optimize sunset timelines

## Implementation Guidance

- Use separate controllers for each API version
- Use separate DTOs for each version
- Share domain model across versions (mappers handle translation)
- Include version in all request/response headers
- Deprecation header standard: `Deprecation: true, Sunset: <date>`
- Document version differences clearly
- Provide migration guides for breaking changes
- Monitor version usage via metrics
- Plan sunset 12+ months in advance
- Support minimum 2 years per version
