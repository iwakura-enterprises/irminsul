# Irminsul

Opinionated Hibernate ORM wrapper for Java 17+.

I have worked with Spring Boot for some time. When I was working on non-Spring Boot projects, I've been
missing a lot of the database features that Spring Boot and its JPA module provide. So I decided to create
this little library to make my life easier.

As mentioned, this is opinionated library. It is fit to suit my needs - and maybe needs of other developers as well.
It should not be viewed as definitive way to work with Hibernate ORM. I will probably miss out a lot of the features
that it provides.

## Features

- JPA-like repositories with predefined methods
  - Plus some extra methods using the `RepositoryExtension` interface
- ThreadLocal transaction management
- For better or worse, programmatical Hibernate ORM configuration

## Documentation

For documentation on installation and usage, please check [my documentations](https://docs.iwakura.enterprises/irminsul.html).

## Showcase

```java
// Creates database service
final var databaseService = new IrminsulDatabaseService(/* config */);

// Initializes database service, e.g., connects to the database
databaseService.initialize(
    Company.class,
    Employee.class
);

// OPTIONAL: Runs liquibase migrations
databaseService.runLiquibase();

// Creates repositories based on the database service
final var companyRepository = new CompanyRepository(databaseService);
final var employeeRepository = new EmployeeRepository(databaseService);

// Runs database transaction in current thread
final var employee = databaseService.runInThreadTransaction(session -> {
    final var companyToCreate = Company.create("Test Company");
    final var savedCompany = companyRepository.save(companyToCreate);
    
    final var employeeToCreate = Employee.create("Test Employee", savedCompany);
    final var savedEmployee = employeeRepository.save(employeeToCreate);
    
    final var fetchedCompany = companyRepository.findById(savedCompany.getId());
    final var fetchedEmployee = employeeRepository.findById(savedEmployee.getId());
    
    return savedEmployee;
});
```

> [!CAUTION]
> Transactions are *not* thread-safe! Thus, you should not use the same transaction in multiple threads. In the
> background, it uses `ThreadLocal` to store current session and transaction.

> [!TIP]
> If you run another transaction within a thread that has active transaction, the same transaction will be used.
> In practice this means you don't create new session and transaction for every database operations, as that
> would not make sense.