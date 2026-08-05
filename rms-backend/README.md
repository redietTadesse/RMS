# RMS Backend (Spring Boot + MySQL)

## Prerequisites
- Java 17+ (`java -version`)
- Maven 3.8+ (`mvn -version`) — or use an IDE like IntelliJ that bundles it
- MySQL 8 running locally on port 3306

## Setup
1. Open `src/main/resources/application.properties` and set your MySQL password
   (`spring.datasource.password=CHANGE_ME`).
2. You do NOT need to create the database manually — `createDatabaseIfNotExist=true`
   creates `rms_db` on first run, and `ddl-auto=update` creates the tables.

## Run
```bash
mvn spring-boot:run
```
API runs at http://localhost:8080. CORS is already configured for the Vite
dev server at http://localhost:5173.

## Endpoints

### Spaces
| Method | URL | Notes |
|--------|-----|-------|
| GET | /api/spaces | all spaces |
| GET | /api/spaces?status=VACANT | filter by status (VACANT / OCCUPIED) |
| GET | /api/spaces/{id} | one space |
| POST | /api/spaces | create |
| PUT | /api/spaces/{id} | update |
| DELETE | /api/spaces/{id} | delete |

### Tenants
| Method | URL | Notes |
|--------|-----|-------|
| GET | /api/tenants | all tenants |
| GET | /api/tenants?search=abebe | search by name, email, or TIN |
| GET | /api/tenants/{id} | one tenant |
| POST | /api/tenants | create |
| PUT | /api/tenants/{id} | update |
| DELETE | /api/tenants/{id} | delete |

### Tenant documents
| Method | URL | Notes |
|--------|-----|-------|
| POST | /api/tenants/{id}/documents | multipart: `file` + `type` (TIN_CERTIFICATE / ID_CARD / BUSINESS_LICENSE) |
| GET | /api/tenants/{id}/documents | list a tenant's documents |
| GET | /api/tenants/{tid}/documents/{docId}/download | download a file |

Files are stored under `uploads/tenant-{id}/`. Uploading an ID_CARD or
BUSINESS_LICENSE automatically sets the tenant's `hasIdCard` /
`hasBusinessLicense` flag.

### Dashboard
| Method | URL | Returns |
|--------|-----|---------|
| GET | /api/dashboard/stats | totalSpaces, vacantSpaces, occupiedSpaces, activeLeases, pendingPayments, overduePayments, totalPenalties |

## Quick test with curl
```bash
# Create a space
curl -X POST http://localhost:8080/api/spaces \
  -H "Content-Type: application/json" \
  -d '{"unitNumber":"G-01","floor":0,"category":"Shop","size":45.5,"status":"VACANT"}'

# Create a tenant
curl -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{"name":"Abebe Trading PLC","type":"COMPANY","contact":"Abebe K.","phone":"+251911000000","email":"abebe@example.com","tinNumber":"0012345678"}'

# Upload a document
curl -X POST http://localhost:8080/api/tenants/1/documents \
  -F "type=ID_CARD" -F "file=@/path/to/id.jpg"

# Dashboard stats
curl http://localhost:8080/api/dashboard/stats
```

## Notes
- `Lease` and `Invoice` are minimal entities for now — just enough to power
  dashboard stats. They'll be expanded in the leases/billing sprint.
- `.gitignore` excludes `target/` and `uploads/`.
