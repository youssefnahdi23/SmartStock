# REST API Guidelines

## Design Style

Use REST principles strictly.

---

## URL Structure

- /products
- /inventory
- /warehouses
- /suppliers

NO verbs in URLs.

---

## HTTP Methods

- GET → fetch
- POST → create
- PUT → full update
- PATCH → partial update
- DELETE → remove

---

## Response Format

Always return:

```json
{
  "timestamp": "",
  "status": 200,
  "data": {},
  "message": "success"
}

##Error Format
{
  "timestamp": "",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed"
}

Pagination

Always use:

page
size
Versioning
/api/v1/...
