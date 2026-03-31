# AGENTS.md

Instructions for AI coding agents working in this repository.

## Commands

```bash
./gradlew build              # Build + run all tests + ktlint
./gradlew test               # Run tests only
./gradlew ktlintFormat       # Auto-format Kotlin code
./gradlew ktlintCheck        # Check formatting without fixing
```

`ktlintFormat` runs automatically before Kotlin compilation, so `./gradlew build` always
formats first.

## Stack

- **Language:** Kotlin 2.3, JVM 21
- **Web framework:** Ktor 3 (Netty engine)
- **Database:** PostgreSQL + Exposed ORM
- **Messaging:** Kafka via Rapids & Rivers pattern
- **Auth:** Azure AD (service-to-service) and TokenX (user context), JWT validation
- **PDF generation:** FreeMarker templates + OpenHtmlToPDF
- **Build:** Gradle (use the bundled `./gradlew` wrapper)

## Project structure

```
src/main/kotlin/no/nav/dagpenger/soknad/orkestrator/
├── api/              # REST endpoint handlers and auth config
├── barn/             # Child (barn) information handling
├── behov/løsere/     # Kafka "behov" solvers (one per information need)
├── config/           # Application configuration
├── journalføring/    # Archival/journaling
├── land/             # Country/location data
├── metrikker/        # Prometheus metrics
├── opplysning/       # Core opplysning (information) service and barn CRUD
├── personalia/       # Personal data (name, address, bank account)
├── quizOpplysning/   # Quiz-based data collection + DB repositories
├── søknad/           # Søknad (application) lifecycle, PDF, messaging
└── utils/            # Shared utilities
```

### API contracts (auto-generated DTOs)

- `openapi/src/main/resources/soknad-orkestrator-api.yaml` — REST API spec (OpenAPI 3.0)
- `asyncapi/src/main/resources/async-api.yaml` — Kafka message spec (AsyncAPI 3.0)
- DTOs are generated at build time into `openapi/build/generated/`. All generated model
  classes are suffixed with `DTO`. Never edit generated files — edit the YAML spec instead.

### Dependency injection

Wiring happens in `ApplicationBuilder.kt`. There is no DI framework — dependencies are
constructed manually and passed as constructor parameters.

## Key patterns

### Rapids & Rivers (Kafka)

The app uses the Rapids & Rivers pattern for event-driven messaging. Each "behovløser"
(need solver) listens for a specific `@behov` type on Kafka and responds with a `@løsning`.

Behovløsere live in `behov/løsere/` and are registered via `BehovløserFactory`.

#### Adding a new behovløser

1. Create a new class in `behov/løsere/` — follow an existing one (e.g., `KanJobbeHvorSomHelstBehovløser` for booleans)
2. Add an entry to the `Behov` enum in `BehovløserFactory.kt`
3. Register the new behovløser in the factory map in `BehovløserFactory.kt`
4. Update the hardcoded list in `BehovløserFactoryTest.kt` (the `Kan hente ut alle behov` test)
5. Add a test in `behov/løsere/` — use `PacketGenerator` to create test messages

**`beskrivendeId` values** (e.g. `faktum.jobbe-hele-norge`) are defined by the upstream
quiz app — do not invent new ones. The behovløser must use the `beskrivendeId` that matches
the incoming data from the quiz.

### Database (Exposed ORM)

- Tables are defined as Exposed `Table` objects (e.g., `SaksbehandlerBarnTabell`)
- Repositories use Exposed DSL for queries
- Migrations: Flyway-style via Exposed
- Always wrap DB operations in `transaction { }`

### Configuration

Uses `com.natpryce:konfig` with priority: System Properties > Environment Variables > Defaults.
See `Configuration.kt` for all config keys.

## Testing

- **Framework:** JUnit 5 (`useJUnitPlatform()`)
- **Assertions:** Kotest matchers (`shouldBe`, `shouldNotBe`, etc.)
- **Mocking:** MockK (`mockk<T>()`, `every { } returns`, `verify { }`)
- **HTTP tests:** Ktor `testApplication { }` with `TestApplication` helpers
- **DB tests:** Postgres testcontainers
- **Auth tests:** `mock-oauth2-server` with `testAzureADToken` helper

Tests mirror the main source structure under `src/test/kotlin/`.

### Auth in tests

```kotlin
System.setProperty("Grupper.saksbehandler", "saksbehandler")
TestApplication.withMockAuthServerAndTestApplication { }
```

## Code style

- ktlint with default rules, no custom configuration
- Generated code in `openapi/build/generated/` is excluded from linting
- Norwegian naming for domain concepts (søknad, opplysning, behov, løser, barn, etc.)
- English for generic programming constructs

## Boundaries

- **Never edit** files under `openapi/build/generated/` — they are auto-generated from the
  OpenAPI YAML spec
- **Never commit** secrets or credentials
- **Do not modify** `.nais/` deployment configs without understanding the NAIS platform
- **Preserve** the append-only pattern for `saksbehandler_barn` (inserts only, no updates)

## Deployment

- **Platform:** NAIS (NAV's Kubernetes on GCP)
- **Environments:** dev-gcp, prod-gcp
- **CI/CD:** GitHub Actions — merges to `main` auto-deploy to dev, then prod
- **Docker base:** OpenJDK 21, timezone Europe/Oslo

## Local development

```bash
docker compose up -d           # Start Kafka + Zookeeper + PostgreSQL
./gradlew build                # Build and test
```
