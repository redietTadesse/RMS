#### 2026-08-31
The RMS backend is a Spring Boot Java application using Maven for dependency management and JPA/Hibernate for data persistence.

#### 2026-08-31
Code follows a layered architecture with distinct packages for controllers, services, repositories, entities, and DTOs under com.rms.

#### 2026-08-31
Core domain entities include Invoice, Lease, Payment, Tenant, and User, with Invoice requiring a unique invoice_number.

#### 2026-08-31
DTOs utilize Jakarta Validation annotations like @NotNull, @Positive, and @PastOrPresent for input validation.

#### 2026-08-31
Service layers implement concurrency control by catching and handling OptimisticLockingFailureException.

#### 2026-08-31
Database tables are explicitly mapped using @Table annotations (e.g., @Table(name = invoices)) rather than relying on defaults.