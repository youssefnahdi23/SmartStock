# Warehouse Service API

**Base URL:** `/api/v1/warehouses`  
**Service Port:** 8004  
**Authentication:** JWT Bearer Token (required for all endpoints)  
**Authorization:** RBAC - Warehouse-specific permissions  
**Status:** Core Service

---

## Overview

The Warehouse Service manages warehouse definitions, zones, shelves, bins, capacity tracking, warehouse transfer workflows, and physical locations within the SmartStock AI platform.

---

## Endpoints

### 1. Create Warehouse

**Endpoint:** `POST /api/v1/warehouses`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `warehouse:create`

**Request Body:**
```json
{
  "code": "W01",
  "name": "Main Warehouse",
  "description": "Primary distribution warehouse for East region",
  "type": "PRIMARY",
  "location": {
    "address": "123 Industrial Ave, New York, NY 10001",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "city": "New York",
    "state": "NY",
    "country": "USA",
    "zipCode": "10001"
  },
  "capacity": {
    "maxFloorSpace": 50000.0,
    "floorSpaceUnit": "SQ_FT",
    "maxPallets": 2000,
    "maxWeight": 500000.0,
    "weightUnit": "KG"
  },
  "manager": {
    "userId": "user-123",
    "email": "john.manager@company.com"
  },
  "operatingHours": {
    "mondayToFriday": "06:00-22:00",
    "saturday": "08:00-18:00",
    "sunday": "CLOSED"
  },
  "active": true
}
```

**Response (201 Created):**
```json
{
  "data": {
    "id": "W01",
    "code": "W01",
    "name": "Main Warehouse",
    "description": "Primary distribution warehouse for East region",
    "type": "PRIMARY",
    "location": {
      "address": "123 Industrial Ave, New York, NY 10001",
      "city": "New York",
      "country": "USA"
    },
    "capacity": {
      "maxFloorSpace": 50000.0,
      "usedFloorSpace": 0,
      "availableFloorSpace": 50000.0,
      "maxPallets": 2000,
      "usedPallets": 0,
      "maxWeight": 500000.0,
      "usedWeight": 0
    },
    "manager": {
      "userId": "user-123",
      "name": "John Manager",
      "email": "john.manager@company.com"
    },
    "active": true,
    "createdAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 2. Get Warehouse Details

**Endpoint:** `GET /api/v1/warehouses/{warehouseId}`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `warehouse:read`

**Response (200 OK):**
```json
{
  "data": {
    "id": "W01",
    "code": "W01",
    "name": "Main Warehouse",
    "description": "Primary distribution warehouse for East region",
    "type": "PRIMARY",
    "location": {
      "address": "123 Industrial Ave, New York, NY 10001",
      "latitude": 40.7128,
      "longitude": -74.0060,
      "city": "New York",
      "state": "NY",
      "country": "USA",
      "zipCode": "10001"
    },
    "capacity": {
      "maxFloorSpace": 50000.0,
      "usedFloorSpace": 35000.0,
      "availableFloorSpace": 15000.0,
      "utilizationPercentage": 70.0,
      "maxPallets": 2000,
      "usedPallets": 1400,
      "maxWeight": 500000.0,
      "usedWeight": 450000.0
    },
    "manager": {
      "userId": "user-123",
      "name": "John Manager",
      "email": "john.manager@company.com"
    },
    "staffCount": 25,
    "active": true,
    "zoneCount": 5,
    "totalBins": 2000,
    "occupiedBins": 1850,
    "createdAt": "2026-06-20T12:00:00Z",
    "updatedAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 3. List Warehouses (Paginated)

**Endpoint:** `GET /api/v1/warehouses`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `warehouse:read`

**Query Parameters:**
- `page` (integer, default: 0) - Page number
- `size` (integer, default: 20) - Results per page
- `type` (string) - Filter by warehouse type
- `active` (boolean) - Filter by active status
- `search` (string) - Search warehouse name or code

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "W01",
      "code": "W01",
      "name": "Main Warehouse",
      "type": "PRIMARY",
      "city": "New York",
      "country": "USA",
      "capacity": {
        "utilizationPercentage": 70.0
      },
      "staffCount": 25,
      "active": true
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "page": 0,
    "size": 20,
    "total": 12,
    "totalPages": 1,
    "traceId": "trace-123"
  }
}
```

---

### 4. Update Warehouse

**Endpoint:** `PUT /api/v1/warehouses/{warehouseId}`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `warehouse:write`

**Request Body:**
```json
{
  "name": "Main Warehouse - Updated",
  "description": "Updated description",
  "manager": {
    "userId": "user-124"
  },
  "operatingHours": {
    "mondayToFriday": "06:00-23:00",
    "saturday": "08:00-20:00",
    "sunday": "10:00-18:00"
  }
}
```

**Response (200 OK):**
```json
{
  "data": {
    "id": "W01",
    "code": "W01",
    "name": "Main Warehouse - Updated",
    "description": "Updated description",
    "updatedAt": "2026-06-20T12:05:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:05:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 5. Create Warehouse Zone

**Endpoint:** `POST /api/v1/warehouses/{warehouseId}/zones`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `warehouse:zone:create`

**Request Body:**
```json
{
  "code": "ZONE-A",
  "name": "Cold Storage Zone",
  "description": "Temperature-controlled zone for perishable goods",
  "type": "COLD_STORAGE",
  "floorSpace": 5000.0,
  "maxCapacity": 1000,
  "temperature": {
    "min": 0,
    "max": 5,
    "unit": "CELSIUS"
  }
}
```

**Response (201 Created):**
```json
{
  "data": {
    "id": "zone-123",
    "warehouseId": "W01",
    "code": "ZONE-A",
    "name": "Cold Storage Zone",
    "description": "Temperature-controlled zone for perishable goods",
    "type": "COLD_STORAGE",
    "floorSpace": 5000.0,
    "maxCapacity": 1000,
    "usedCapacity": 0,
    "occupancyPercentage": 0,
    "temperature": {
      "min": 0,
      "max": 5,
      "unit": "CELSIUS"
    },
    "shelves": [],
    "createdAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 6. List Warehouse Zones

**Endpoint:** `GET /api/v1/warehouses/{warehouseId}/zones`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `warehouse:read`

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "zone-123",
      "code": "ZONE-A",
      "name": "Cold Storage Zone",
      "type": "COLD_STORAGE",
      "floorSpace": 5000.0,
      "usedCapacity": 500,
      "occupancyPercentage": 50.0,
      "shelves": 10,
      "bins": 100
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "page": 0,
    "total": 5,
    "traceId": "trace-123"
  }
}
```

---

### 7. Create Shelf in Zone

**Endpoint:** `POST /api/v1/warehouses/{warehouseId}/zones/{zoneId}/shelves`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `warehouse:shelf:create`

**Request Body:**
```json
{
  "code": "SHELF-A-01",
  "name": "Shelf A-01",
  "level": 1,
  "capacity": 100,
  "weight_limit": 1000
}
```

**Response (201 Created):**
```json
{
  "data": {
    "id": "shelf-123",
    "zoneId": "zone-123",
    "code": "SHELF-A-01",
    "name": "Shelf A-01",
    "level": 1,
    "capacity": 100,
    "usedCapacity": 0,
    "weightLimit": 1000,
    "bins": 10,
    "createdAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 8. Create Bin in Shelf

**Endpoint:** `POST /api/v1/warehouses/{warehouseId}/zones/{zoneId}/shelves/{shelfId}/bins`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `warehouse:bin:create`

**Request Body:**
```json
{
  "code": "BIN-A-01-001",
  "name": "Bin A-01-001",
  "position": 1,
  "capacity": 50,
  "type": "STANDARD"
}
```

**Response (201 Created):**
```json
{
  "data": {
    "id": "bin-123",
    "shelfId": "shelf-123",
    "code": "BIN-A-01-001",
    "name": "Bin A-01-001",
    "position": 1,
    "capacity": 50,
    "usedCapacity": 0,
    "type": "STANDARD",
    "currentProduct": null,
    "createdAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 9. Find Available Bin

**Endpoint:** `GET /api/v1/warehouses/{warehouseId}/bins/available`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `warehouse:read`

**Query Parameters:**
- `zoneId` (UUID, optional) - Prefer specific zone
- `quantity` (integer, required) - Number of units to store
- `productId` (UUID, optional) - Find bin compatible with product

**Response (200 OK):**
```json
{
  "data": [
    {
      "binId": "bin-123",
      "code": "BIN-A-01-001",
      "zoneId": "zone-123",
      "zoneName": "Cold Storage Zone",
      "shelfId": "shelf-123",
      "position": "A-01-001",
      "availableCapacity": 50,
      "temperature": 2.5,
      "compatibility": "COMPATIBLE"
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 10. Get Warehouse Capacity Report

**Endpoint:** `GET /api/v1/warehouses/{warehouseId}/capacity-report`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `warehouse:report`

**Response (200 OK):**
```json
{
  "data": {
    "warehouseId": "W01",
    "warehouseName": "Main Warehouse",
    "reportDate": "2026-06-20",
    "floorSpace": {
      "total": 50000.0,
      "used": 35000.0,
      "available": 15000.0,
      "utilizationPercentage": 70.0
    },
    "pallets": {
      "total": 2000,
      "used": 1400,
      "available": 600,
      "utilizationPercentage": 70.0
    },
    "weight": {
      "total": 500000.0,
      "used": 450000.0,
      "available": 50000.0,
      "utilizationPercentage": 90.0
    },
    "zones": [
      {
        "zoneId": "zone-123",
        "zoneName": "Cold Storage Zone",
        "utilizationPercentage": 50.0,
        "shelves": 10,
        "bins": 100,
        "occupiedBins": 50
      }
    ],
    "alerts": [
      {
        "level": "WARNING",
        "type": "CAPACITY_THRESHOLD",
        "message": "Weight capacity at 90%, consider redistribution"
      }
    ]
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 11. Deactivate Warehouse

**Endpoint:** `POST /api/v1/warehouses/{warehouseId}/deactivate`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `warehouse:write`

**Response (200 OK):**
```json
{
  "data": {
    "id": "W01",
    "code": "W01",
    "name": "Main Warehouse",
    "active": false,
    "deactivatedAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

## Data Transfer Objects

### WarehouseDTO
```typescript
{
  id: UUID;
  code: string;
  name: string;
  description: string;
  type: "PRIMARY" | "SECONDARY" | "DISTRIBUTION_CENTER";
  location: {
    address: string;
    city: string;
    state: string;
    country: string;
    zipCode: string;
    latitude: number;
    longitude: number;
  };
  capacity: {
    maxFloorSpace: number;
    usedFloorSpace: number;
    maxPallets: number;
    usedPallets: number;
    maxWeight: number;
    usedWeight: number;
  };
  manager: { userId: UUID; email: string };
  active: boolean;
  createdAt: ISO8601DateTime;
  updatedAt: ISO8601DateTime;
}
```

### ZoneDTO
```typescript
{
  id: UUID;
  code: string;
  name: string;
  type: "STANDARD" | "COLD_STORAGE" | "HAZMAT" | "HIGH_VALUE";
  floorSpace: number;
  usedCapacity: number;
  occupancyPercentage: number;
  temperature?: { min: number; max: number; unit: string };
}
```

---

## Standard Error Codes

| Code | HTTP Status | Description |
|------|------------|-------------|
| WAREHOUSE_NOT_FOUND | 404 | Warehouse does not exist |
| ZONE_NOT_FOUND | 404 | Zone does not exist |
| SHELF_NOT_FOUND | 404 | Shelf does not exist |
| BIN_NOT_FOUND | 404 | Bin does not exist |
| CAPACITY_EXCEEDED | 400 | Warehouse capacity exceeded |
| NO_AVAILABLE_BIN | 400 | No available bin for allocation |
| INSUFFICIENT_PERMISSIONS | 403 | User lacks required permissions |
| VALIDATION_FAILED | 422 | Invalid input |

---

## Events Published

- `WarehouseCreated` - New warehouse created
- `WarehouseUpdated` - Warehouse information updated
- `WarehouseDeactivated` - Warehouse deactivated
- `ZoneCreated` - New zone created
- `ShelfCreated` - New shelf created
- `BinCreated` - New bin created
- `BinAllocated` - Product allocated to bin
- `CapacityAlert` - Capacity threshold exceeded

---

## Implementation Notes

1. Warehouse hierarchy: Warehouse → Zone → Shelf → Bin
2. Bin allocation based on product type and zone compatibility
3. Temperature monitoring for cold storage zones
4. Capacity alerts generated at 80%, 90%, 100%
5. Historical capacity data retained for trend analysis
6. Warehouse transfers use atomic transactions
7. Deactivation prevents new operations but retains historical data
8. All location data geo-indexed for proximity searches
9. Physical space and weight capacity tracked separately
10. Bin search optimized with spatial indexing
