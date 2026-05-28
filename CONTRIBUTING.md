# Contributing to Generic Sys Admin

Thank you for your interest in contributing to Generic Sys Admin. This document provides guidelines and instructions for contributing.

## Getting Started

### Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| JDK | 17+ | Backend runtime |
| Maven | 3.8+ | Backend build |
| Node.js | 20+ | Frontend runtime |
| MySQL | 8.0+ | Database |
| Docker | Latest | Containerized deployment (optional) |

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd generic-sys-admin
   ```

2. **Initialize the database**
   ```bash
   mysql -u root -p < sql/init.sql
   ```

3. **Start the backend**
   ```bash
   cd backend
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

4. **Start the frontend**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

5. **Access the application**
   - Frontend: http://localhost:5173
   - API Docs: http://localhost:8081/doc.html
   - Default credentials: admin / 123456

## Development Workflow

### Branch Naming

- `feature/<description>` - New features
- `fix/<description>` - Bug fixes
- `refactor/<description>` - Code refactoring
- `docs/<description>` - Documentation updates

### Commit Messages

Follow conventional commits format:

```
<type>(<scope>): <description>

[optional body]
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

Examples:
```
feat(equipment): add batch import via Excel
fix(auth): resolve JWT token refresh race condition
docs(api): update Swagger annotations for ConsumableController
```

### Code Style

#### Backend (Java)

- Follow standard Java naming conventions
- Use Lombok annotations to reduce boilerplate
- Add Javadoc for public classes and methods
- Use `@PreAuthorize` for endpoint security
- Log operations via the `@Log` AOP annotation

#### Frontend (Vue/TypeScript)

- Use Composition API with `<script setup>`
- Type all props and emits
- Use Pinia stores for state management
- Follow Element Plus component patterns

## Testing

### Backend Tests

```bash
cd backend

# Run all tests
mvn test

# Run smoke tests only
mvn test -Dtest=SmokeTest

# Run specific test class
mvn test -Dtest=JwtUtilTest
```

### Frontend Tests

```bash
cd frontend

# Type checking
npx vue-tsc --noEmit

# Build verification
npm run build
```

### Writing Tests

- Place unit tests alongside the class they test in `src/test/java`
- Use `@SpringBootTest` with H2 for integration tests
- Use `@DisplayName` for readable test names
- Mock external services (MinIO, Redis) in tests

## Pull Request Process

1. Create a feature branch from `main`
2. Make your changes with appropriate tests
3. Ensure all tests pass: `mvn test` (backend) and `npm run build` (frontend)
4. Update documentation if needed
5. Submit a pull request with a clear description
6. Wait for CI checks to pass

### PR Checklist

- [ ] Code compiles without errors
- [ ] Existing tests pass
- [ ] New tests added for new functionality
- [ ] API documentation updated (Swagger annotations)
- [ ] No hardcoded credentials or secrets
- [ ] Database migrations included if schema changed

## Database Changes

When modifying the database schema:

1. Create a new migration script in `sql/` with version prefix (e.g., `v1.2__add_field.sql`)
2. Update entity classes in `model/entity/`
3. Update the `init.sql` file for fresh installations
4. Test with both fresh install and migration scenarios

## Reporting Issues

When reporting bugs, include:

- Steps to reproduce
- Expected behavior
- Actual behavior
- Environment details (OS, JDK version, browser)
- Relevant logs or screenshots

## Architecture Overview

```
backend/
  controller/    - REST API endpoints
  service/       - Business logic
  mapper/        - MyBatis-Plus data access
  model/         - Entity, DTO, VO classes
  security/      - JWT authentication
  config/        - Application configuration
  aspect/        - AOP (audit logging)

frontend/
  api/           - Axios HTTP clients
  views/         - Page components
  router/        - Vue Router config
  store/         - Pinia state management
  components/    - Reusable UI components
```

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
