## Request app

A small DDD / hexagonal Spring Boot application that creates a Request and drives it through its
lifecycle.

````
  ● ──create──▶ CREATED ──verify──▶ VERIFIED ──accept──▶ ACCEPTED ──publish──▶ PUBLISHED
                   │                    │                    │
                delete                reject               reject
                   ▼                    ▼                    ▼
                DELETED             REJECTED             REJECTED
````

`DELETED`, `REJECTED` and `PUBLISHED` are terminal. The body can be rewritten while the request is
`CREATED` or `VERIFIED`. Every rule is enforced in the aggregate, so no caller can reach an illegal
state by going around the application service.

### Modules

| module | role |
|---|---|
| `requestapp` | domain and ports (the `Request` aggregate, in/out ports, application service) |
| `requestapp-rest-adapter` | driving adapter, REST API |
| `request-sql-db-adapter` | driven adapter, MyBatis + Liquibase persistence |
| `requestapp-webapp` | Spring Boot application that wires the adapters together |

### Requirements

- Java 21

### Build

````
./gradlew build
````

### Run unit and component tests for all modules

````
./gradlew test
````

Component tests also run against an in-memory H2 database, so they need no external services.

### Start the application

Nothing else has to be running first. The app carries its own in-memory database.

Either run it straight from Gradle:

````
./gradlew requestapp-webapp:bootRun
````

or build the jar once and run that:

````
./gradlew build
java -jar requestapp-webapp/build/libs/requestapp-webapp-1.0.jar
````

It is up when the log says `Started Application`, on port 8080 under the context path `/requestapp`.
Stop it with Ctrl+C.

| where | what |
|---|---|
| http://localhost:8080/requestapp/api/requests | the API, see below |
| http://localhost:8080/requestapp/swagger-ui/index.html | Swagger UI |
| http://localhost:8080/requestapp/h2-console | H2 console, JDBC URL `jdbc:h2:mem:requestdb`, user `sa`, no password |
| http://localhost:8080/requestapp/actuator | actuator endpoints |

Check it answers:

````
curl -X POST http://localhost:8080/requestapp/api/requests \
  -H 'Content-Type: application/json' \
  -d '{"name":"first request","body":"the body of the request"}'
````

That prints the new request's uuid. Read it back with:

````
curl http://localhost:8080/requestapp/api/requests/{uuid}
````

To run on a different port, pass it through:

````
./gradlew requestapp-webapp:bootRun --args='--server.port=9090'
````

The database is an in-memory H2 in MySQL compatibility mode, so it needs no external service. The data
lives as long as the process does, and Liquibase recreates the `request` and `request_event` tables on
every start.

### API

Only a read answers with the request. Creating and publishing answer with the id each of them mints,
as plain text; every other call answers with `200` and no body, or with an error body.

| call | answer |
|---|---|
| `GET /api/requests/{id}` | the request, as JSON |
| `POST /api/requests` | the new request's id |
| `POST /api/requests/{id}/publish?version=` | the published request's id |
| `PUT /api/requests/{id}/body?version=` | `200`, no body |
| `POST /api/requests/{id}/{verify\|accept\|reject\|delete}?version=` | `200`, no body |

A call that failed always has a body: a list of `{ code, fieldName, engMessage }`.

### Audit log

Every change that takes effect is written down. The aggregate raises an event for it (it is the only
place that knows a change was legal and what it moved from), and the application service appends those
events to the `request_event` table **in the same transaction as the change itself**, which is the
outbox shape: a committed change always has its history, and a rolled back one leaves none. A refused
call records nothing, having changed nothing.

| column | |
|---|---|
| `event_type` | `CREATED`, `BODY_UPDATED`, `VERIFIED`, `ACCEPTED`, `REJECTED`, `PUBLISHED`, `DELETED` |
| `from_status` → `to_status` | where the request moved (`from_status` is null for a creation) |
| `reason`, `published_request_uuid` | what that change carried |
| `decided_on_version` | the version the change was decided on |
| `occurred_at`, `id` | when it happened; `id` is the order it was appended in |
| `published_at` | stamped by whatever forwards these one day: null, and unread, until then |

The table is append-only: nothing updates or deletes a row, which is what separates an audit log from a
queue. Rewriting the body is recorded too, even though it moves no status: a history with a body
appearing from nowhere would have a hole in it. There is no API for reading the history yet.

### Optimistic locking

Nothing is locked between reading a request and writing it. Instead every request carries a version,
and a write only lands while the stored request is still at the version the write was decided on:

- reading a request answers with its current `version`;
- every transition requires the `version` the caller acted on, and is refused with `409` if the request
  has moved on since, it's not allowed to ask for a change without saying which state you decided on;
- a transition that lands answers with `200` and no body, so a caller that wants to make another change
  reads the request again and takes the version from there;
- the store checks the version again as it writes, which catches a writer that slipped into the window
  between this read and this write

Reading and creating name no version: one changes nothing, and the other has nothing to be stale about.

A refused write changes nothing: read the request again, decide again, and write against the version it
comes back at.

You can access the Swagger UI here:
- http://localhost:8080/requestapp/swagger-ui/index.html
