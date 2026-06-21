# JavaFX Guidelines (Desktop Client)

## Architecture

Use MVVM pattern:

View → ViewModel → Service → API

---

## UI Rules

- No business logic in UI
- UI only displays data
- Use FXML for layout

---

## Performance

- Avoid heavy UI computations on UI thread
- Use background threads for API calls

---

## Design

- Professional enterprise UI
- Dark mode + Light mode support
- Minimal animations

---

## State Management

- Use ViewModel as single source of truth
- Avoid global static state

---

## Networking

- All calls go through API client layer
- Never call backend directly from UI components
