# Git Commit Guidelines

## Format

<type>(module): <short description>

---

## Types

- feat → new feature
- fix → bug fix
- refactor → code improvement
- docs → documentation
- test → testing
- chore → maintenance

---

## Examples

feat(inventory): add stock movement tracking
fix(auth): resolve JWT expiration bug
refactor(warehouse): simplify allocation logic
docs(api): update product endpoints

---

## Rules

- No vague commits like "fix bug"
- One feature per commit
- Keep commits atomic


# Code Review Checklist

Before merging:

✓ Architecture respected  
✓ ADR respected  
✓ Tests included  
✓ Security checked  
✓ No business logic in controllers  
✓ Database rules respected  
✓ API contracts respected  
