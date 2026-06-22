# Java Style Guide (SmartStock AI)

## Language Version
Java 21+

---

## Naming Conventions

- Classes: PascalCase → `InventoryService`
- Methods: camelCase → `createProduct()`
- Variables: camelCase → `totalQuantity`
- Constants: UPPER_SNAKE_CASE → `MAX_RETRY_COUNT`
- Packages: lowercase → `com.smartstock.inventory`

---

## Class Design Rules

- One class = one responsibility
- Max 300 lines per class (soft rule)
- Prefer composition over inheritance
- Avoid utility classes unless necessary

---

## Method Rules

- Max 30–40 lines per method
- Methods must do ONE thing
- Avoid deep nesting (max 3 levels)

---

## Best Practices

- Use `Optional` instead of null returns
- Use `final` where possible
- Avoid primitive obsession (use Value Objects)
- Prefer immutability

---

## Forbidden

- No `System.out.println` in production
- No business logic in controllers
- No raw SQL in services
