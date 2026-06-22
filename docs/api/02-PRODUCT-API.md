# Product Service API

**Base URL:** `/api/v1/products`  
**Service Port:** 8002  
**Authentication:** JWT Bearer Token (required for all endpoints)  
**Authorization:** RBAC - Product-specific permissions  
**Status:** Core Service

---

## Overview

The Product Service manages product definitions, SKUs, categories, barcodes, QR codes, and product metadata across the SmartStock AI platform.

---

## Endpoints

### 1. Create Product

**Endpoint:** `POST /api/v1/products`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `product:create`

**Request Body:**
```json
{
  "name": "Premium Widget",
  "sku": "WDG-001-PREM",
  "description": "High-quality premium widget for industrial use",
  "categoryId": "cat-123",
  "unitPrice": 99.99,
  "unitCost": 45.00,
  "unit": "PIECE",
  "weight": 2.5,
  "weightUnit": "KG",
  "dimensions": {
    "length": 10.0,
    "width": 8.0,
    "height": 5.0,
    "unit": "CM"
  },
  "reorderPoint": 50,
  "reorderQuantity": 100,
  "maxStock": 500,
  "supplierIds": ["supplier-123", "supplier-456"],
  "attributes": {
    "color": "Red",
    "material": "Aluminum",
    "warranty": "2 years"
  },
  "active": true
}
```

**Response (201 Created):**
```json
{
  "data": {
    "id": "prod-001",
    "name": "Premium Widget",
    "sku": "WDG-001-PREM",
    "description": "High-quality premium widget for industrial use",
    "categoryId": "cat-123",
    "unitPrice": 99.99,
    "unitCost": 45.00,
    "unit": "PIECE",
    "weight": 2.5,
    "weightUnit": "KG",
    "dimensions": {
      "length": 10.0,
      "width": 8.0,
      "height": 5.0,
      "unit": "CM"
    },
    "reorderPoint": 50,
    "reorderQuantity": 100,
    "maxStock": 500,
    "supplierIds": ["supplier-123", "supplier-456"],
    "barcode": "9876543210128",
    "qrCode": "QR-PROD-001",
    "attributes": {
      "color": "Red",
      "material": "Aluminum",
      "warranty": "2 years"
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

**Errors:**
- `400 SKU_ALREADY_EXISTS` - SKU already in use
- `404 CATEGORY_NOT_FOUND` - Category does not exist
- `422 VALIDATION_FAILED` - Invalid input (negative price, invalid unit, etc.)

---

### 2. Get Product Details

**Endpoint:** `GET /api/v1/products/{productId}`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `product:read`

**Response (200 OK):**
```json
{
  "data": {
    "id": "prod-001",
    "name": "Premium Widget",
    "sku": "WDG-001-PREM",
    "description": "High-quality premium widget for industrial use",
    "categoryId": "cat-123",
    "categoryName": "Industrial Widgets",
    "unitPrice": 99.99,
    "unitCost": 45.00,
    "unit": "PIECE",
    "weight": 2.5,
    "weightUnit": "KG",
    "dimensions": {
      "length": 10.0,
      "width": 8.0,
      "height": 5.0,
      "unit": "CM"
    },
    "reorderPoint": 50,
    "reorderQuantity": 100,
    "maxStock": 500,
    "supplierIds": ["supplier-123", "supplier-456"],
    "barcode": "9876543210128",
    "qrCode": "QR-PROD-001",
    "attributes": {
      "color": "Red",
      "material": "Aluminum",
      "warranty": "2 years"
    },
    "active": true,
    "currentStockLevel": 250,
    "currentStockValue": 24999.75,
    "createdAt": "2026-06-20T12:00:00Z",
    "updatedAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

**Errors:**
- `404 PRODUCT_NOT_FOUND` - Product does not exist

---

### 3. Update Product

**Endpoint:** `PUT /api/v1/products/{productId}`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `product:write`

**Request Body:**
```json
{
  "name": "Premium Widget Plus",
  "description": "Enhanced premium widget",
  "unitPrice": 109.99,
  "unitCost": 50.00,
  "reorderPoint": 75,
  "reorderQuantity": 150,
  "attributes": {
    "color": "Blue",
    "material": "Aluminum",
    "warranty": "3 years"
  }
}
```

**Response (200 OK):**
```json
{
  "data": {
    "id": "prod-001",
    "name": "Premium Widget Plus",
    "sku": "WDG-001-PREM",
    "description": "Enhanced premium widget",
    "unitPrice": 109.99,
    "unitCost": 50.00,
    "reorderPoint": 75,
    "reorderQuantity": 150,
    "attributes": {
      "color": "Blue",
      "material": "Aluminum",
      "warranty": "3 years"
    },
    "updatedAt": "2026-06-20T12:05:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:05:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 4. List Products (Paginated)

**Endpoint:** `GET /api/v1/products`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `product:read`

**Query Parameters:**
- `page` (integer, default: 0) - Page number
- `size` (integer, default: 20, max: 100) - Results per page
- `sort` (string) - Field and direction (e.g., "name,asc")
- `categoryId` (UUID) - Filter by category
- `active` (boolean) - Filter by active status
- `search` (string) - Search product name or SKU
- `supplierId` (UUID) - Filter by supplier
- `lowStock` (boolean) - Show only low stock products

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "prod-001",
      "name": "Premium Widget",
      "sku": "WDG-001-PREM",
      "categoryId": "cat-123",
      "categoryName": "Industrial Widgets",
      "unitPrice": 99.99,
      "unitCost": 45.00,
      "unit": "PIECE",
      "reorderPoint": 50,
      "currentStockLevel": 250,
      "active": true,
      "createdAt": "2026-06-20T12:00:00Z"
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "page": 0,
    "size": 20,
    "total": 1234,
    "totalPages": 62,
    "traceId": "trace-123"
  }
}
```

---

### 5. Deactivate Product

**Endpoint:** `POST /api/v1/products/{productId}/deactivate`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `product:write`

**Response (200 OK):**
```json
{
  "data": {
    "id": "prod-001",
    "name": "Premium Widget",
    "sku": "WDG-001-PREM",
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

### 6. Reactivate Product

**Endpoint:** `POST /api/v1/products/{productId}/reactivate`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `product:write`

**Response (200 OK):**
```json
{
  "data": {
    "id": "prod-001",
    "name": "Premium Widget",
    "sku": "WDG-001-PREM",
    "active": true,
    "reactivatedAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 7. Get Product By SKU

**Endpoint:** `GET /api/v1/products/sku/{sku}`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `product:read`

**Response (200 OK):** Same as Get Product Details

---

### 8. Generate Barcode

**Endpoint:** `POST /api/v1/products/{productId}/barcode`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `product:write`

**Request Body:**
```json
{
  "barcodeFormat": "EAN13",
  "regenerate": false
}
```

**Response (200 OK):**
```json
{
  "data": {
    "productId": "prod-001",
    "barcode": "9876543210128",
    "barcodeFormat": "EAN13",
    "barcodeImage": "data:image/png;base64,iVBORw0KGgoAAAANS...",
    "generatedAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 9. Generate QR Code

**Endpoint:** `POST /api/v1/products/{productId}/qrcode`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `product:write`

**Request Body:**
```json
{
  "size": 300,
  "regenerate": false
}
```

**Response (200 OK):**
```json
{
  "data": {
    "productId": "prod-001",
    "qrCode": "QR-PROD-001",
    "qrCodeUrl": "https://smartstock.local/product/prod-001",
    "qrCodeImage": "data:image/png;base64,iVBORw0KGgoAAAANS...",
    "size": 300,
    "generatedAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 10. Create Product Category

**Endpoint:** `POST /api/v1/products/categories`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `product:category:create`

**Request Body:**
```json
{
  "name": "Industrial Widgets",
  "description": "High-performance widgets for industrial applications",
  "parentCategoryId": null,
  "icon": "widget-icon.png"
}
```

**Response (201 Created):**
```json
{
  "data": {
    "id": "cat-123",
    "name": "Industrial Widgets",
    "description": "High-performance widgets for industrial applications",
    "parentCategoryId": null,
    "icon": "widget-icon.png",
    "productCount": 0,
    "createdAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 11. List Product Categories

**Endpoint:** `GET /api/v1/products/categories`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `product:read`

**Query Parameters:**
- `page` (integer, default: 0) - Page number
- `size` (integer, default: 50) - Results per page
- `parentCategoryId` (UUID, optional) - Filter by parent category

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "cat-123",
      "name": "Industrial Widgets",
      "description": "High-performance widgets for industrial applications",
      "parentCategoryId": null,
      "productCount": 250,
      "createdAt": "2026-06-20T12:00:00Z"
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "page": 0,
    "size": 50,
    "total": 25,
    "totalPages": 1,
    "traceId": "trace-123"
  }
}
```

---

### 12. Import Products (CSV/Excel)

**Endpoint:** `POST /api/v1/products/import`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `product:import`

**Request (multipart/form-data):**
- `file` - CSV or Excel file with product data
- `categoryId` (UUID, optional) - Apply category to all imported products
- `dryRun` (boolean, default: false) - Validate without saving

**Response (200 OK):**
```json
{
  "data": {
    "importId": "import-123",
    "fileName": "products.csv",
    "rowsProcessed": 1000,
    "rowsSuccessful": 995,
    "rowsFailed": 5,
    "errors": [
      {
        "row": 5,
        "message": "SKU already exists: WDG-002-PREM"
      },
      {
        "row": 10,
        "message": "Invalid category ID: cat-invalid"
      }
    ],
    "dryRun": false,
    "startedAt": "2026-06-20T12:00:00Z",
    "completedAt": "2026-06-20T12:05:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:05:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 13. Export Products (CSV)

**Endpoint:** `GET /api/v1/products/export`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `product:export`

**Query Parameters:**
- `categoryId` (UUID, optional) - Export specific category only
- `active` (boolean, optional) - Filter by active status
- `format` (string, default: "csv") - Export format (csv, excel)
- `includePrice` (boolean, default: true) - Include pricing data
- `includeStock` (boolean, default: false) - Include current stock levels

**Response (200 OK):**
- Content-Type: `text/csv` or `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Content-Disposition: `attachment; filename="products-2026-06-20.csv"`

**CSV Format:**
```
productId,name,sku,categoryId,categoryName,unitPrice,unitCost,unit,description,active
prod-001,Premium Widget,WDG-001-PREM,cat-123,Industrial Widgets,99.99,45.00,PIECE,High-quality widget,true
```

---

## Data Transfer Objects

### ProductDTO
```typescript
{
  id: UUID;
  name: string;
  sku: string;
  description: string;
  categoryId: UUID;
  unitPrice: BigDecimal;
  unitCost: BigDecimal;
  unit: "PIECE" | "KG" | "L" | "M" | "BOX";
  weight: number;
  weightUnit: "KG" | "G" | "LB";
  dimensions: {
    length: number;
    width: number;
    height: number;
    unit: "CM" | "M" | "IN";
  };
  reorderPoint: number;
  reorderQuantity: number;
  maxStock: number;
  supplierIds: UUID[];
  barcode: string;
  qrCode: string;
  attributes: Record<string, string>;
  active: boolean;
  createdAt: ISO8601DateTime;
  updatedAt: ISO8601DateTime;
}
```

### CategoryDTO
```typescript
{
  id: UUID;
  name: string;
  description: string;
  parentCategoryId?: UUID;
  icon?: string;
  productCount: number;
  createdAt: ISO8601DateTime;
  updatedAt: ISO8601DateTime;
}
```

---

## Standard Error Codes

| Code | HTTP Status | Description |
|------|------------|-------------|
| PRODUCT_NOT_FOUND | 404 | Product does not exist |
| SKU_ALREADY_EXISTS | 400 | SKU already in use |
| CATEGORY_NOT_FOUND | 404 | Category does not exist |
| VALIDATION_FAILED | 422 | Invalid input (negative price, invalid unit, etc.) |
| INSUFFICIENT_PERMISSIONS | 403 | User lacks required permissions |
| UNAUTHORIZED | 401 | Invalid or expired token |
| FILE_IMPORT_FAILED | 400 | Error importing file |
| BARCODE_GENERATION_FAILED | 500 | Error generating barcode |
| QRCODE_GENERATION_FAILED | 500 | Error generating QR code |

---

## Events Published

- `ProductCreated` - New product created
- `ProductUpdated` - Product information updated
- `ProductDeactivated` - Product deactivated
- `ProductReactivated` - Product reactivated
- `ProductImported` - Products bulk imported
- `BarcodeGenerated` - Barcode generated for product
- `QRCodeGenerated` - QR code generated for product
- `ProductPriceChanged` - Product price updated
- `ProductDeleted` - Product marked for deletion

---

## Implementation Notes

1. All endpoints require HTTPS/TLS 1.3+
2. SKU must be globally unique
3. Barcode format: EAN-13 (default), also support EAN-8, Code128, Code39
4. QR code links to product detail endpoint in desktop app
5. Product exports exclude sensitive supplier terms
6. File imports limited to 50MB per file
7. Bulk operations use async processing for large datasets
8. All product changes logged for audit trail
9. Deactivated products retain historical data for reporting
10. Category hierarchy limited to 5 levels
