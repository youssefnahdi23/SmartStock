# Database Guidelines (PostgreSQL)

## Ownership Rule

Each microservice owns its database.

NO shared tables.

---

## Design Rules

- Must be in 3NF (minimum)
- Always define primary keys
- Always define foreign keys
- Use indexes for search fields

---

## Naming

- Tables: snake_case → `inventory_movements`
- Columns: snake_case → `created_at`

---

## Required Columns

Every table must include:

- id (UUID preferred)
- created_at
- updated_at
- deleted_at (soft delete if needed)

---

## Query Rules

- No `SELECT *`
- Always paginate large queries
- Avoid N+1 problem

---

## Migrations

- Use Flyway
- Never modify production schema manually
