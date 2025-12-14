<show-structure depth="2"/>

# Irminsul

<img src="https://akasha.iwakura.enterprises/data-source/hetzner/public/logo/irminsul.png" alt="Irminsul logo" width="300" border-effect="rounded"/>

Irminsul is a Java library wrapping Hibernate ORM. This allows you to create applications on top of Hibernate with less
boilerplate code. It is an opinionated library, fit to suit my needs - and maybe needs of other developers as well. It
should  not be viewed as definitive way to work with Hibernate ORM. I will probably miss out a lot of the features
that it provides.

[Source Code](https://github.com/iwakura-enterprises/irminsul) —
[Documentation](https://docs.iwakura.enterprises/irminsul/) —
[Maven Central](https://central.sonatype.com/artifact/enterprises.iwakura/irminsul)

## Example

<procedure>

```java
// Load the database configuration
var config = DatabaseServiceConfiguration.builder().build();

// Create the database service with the configuration
var databaseService = new IrminsulDatabaseService(config);

// Initialize the database service
databaseService.initialize(Company.class);

// OPTIONAL: Run Liquibase migrations
databaseService.runLiquibase();

// Initialize repositories
var companyRepository = new CompanyRepository(databaseService);

// Fetch all companies
var companies = companyRepository.findAll();
```

</procedure>

## Installation

<a id="irminsul_version" href="https://central.sonatype.com/artifact/enterprises.iwakura/irminsul"><img src="https://maven-badges.sml.io/sonatype-central/enterprises.iwakura/irminsul/badge.png?style=for-the-badge" alt=""></img></a>

> Java 17 or higher is required.

<note>
You might need to click the version badge to see the latest version.
</note>

### Gradle
```groovy
implementation 'enterprises.iwakura:irminsul:VERSION'
// Hibernate ORM
implementation 'org.hibernate.orm:hibernate-core:7.0.0.CR1'
implementation 'org.hibernate.orm:hibernate-processor:7.0.0.CR1'
implementation 'org.hibernate.orm:hibernate-hikaricp:7.0.0.CR1'
annotationProcessor 'org.hibernate.orm:hibernate-processor:7.0.0.CR1'
// JDBC, for example PostgreSQL
implementation 'org.postgresql:postgresql:42.7.5'
// Optional: Liquibase
implementation 'org.liquibase:liquibase-core:4.32.0'
```

### Maven

```xml
<dependency>
    <groupId>enterprises.iwakura</groupId>
    <artifactId>irminsul</artifactId>
    <version>VERSION</version>
</dependency>
<!-- Hibernate ORM -->
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-core</artifactId>
    <version>7.0.0.CR1</version>
</dependency>
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-processor</artifactId>
    <version>7.0.0.CR1</version>
</dependency>
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-hikaricp</artifactId>
    <version>7.0.0.CR1</version>
</dependency>
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-processor</artifactId>
    <version>7.0.0.CR1</version>
    <scope>provided</scope>
</dependency>
<!-- JDBC, for example PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.5</version>
</dependency>
<!-- Optional: Liquibase -->
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
    <version>4.32.0</version>
</dependency>
```

## Usage
First, we need to initialize the database service. Then, we can define our entities and initialize the repositories.
After that, we can use the repositories to interact with the database.

In the following sections, we will go through the steps to set up the database service, define entities, use repositories,
perform transactions and more.

<procedure title="Initializing Database Service" id="initializing-database-service" collapsible="true" default-state="expanded">

```java
// Load the database configuration
var config = DatabaseServiceConfiguration.builder()
    .jdbcDriver("org.postgresql.Driver")
    .dialect("org.hibernate.dialect.PostgreSQLDialect")
    .url("jdbc:postgresql://localhost:5432/testdb")
    .username("testuser")
    .password("testpassword")
    .debugSql(true) // Prints SQL statements to the console
    .logErrors(true) // Logs errors to the console
    .minIdleConnections(1)
    .maxConnections(10)
    .hbm2ddlAuto(Action.UPDATE)
    // OPTIONAL: Specify liquibase file
    .liquibaseChangelogFile("classpath:liquibase/changelog.yaml")
    .build();

// Create the database service with the configuration
var databaseService = new IrminsulDatabaseService(config);

// Initialize the database service
databaseService.initialize(/* entities */);

// OPTIONAL: Run Liquibase migrations
databaseService.runLiquibase();
```

</procedure>

<procedure title="Defining entities" id="defining-entities" collapsible="true" default-state="expanded">
<step>

Let's create entity called a `Company` **entity**:

```java
@Entity
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // Getters and setters

    public static Company create(String name) {
        var company = new Company();
        company.name = name;
        return company;
    }
}
```

</step>
<step>

We will need a corresponding **repository** to interact with the `Company` entity. We will extend
the `BaseRepository` abstract class so we can use some of its predefined methods. In the parameters,
we specify the entity type and its ID type.

```java
public class CompanyRepository extends BaseRepository<Company, Long> {
    
    public CompanyRepository(IrminsulDatabaseService databaseService) {
        super(databaseService);
    }

    @Override
    protected Class<Company> getEntityClass() {
        return Company.class;
    }

    @Override
    protected boolean hasId(Company company) {
        // Determines if the entity has an ID
        // Used by the #save() and #saveAll() methods
        return company.getId() != null;
    }
}
```

</step>
<step>

Now we can **initialize** the **database service** with our **entity** and **repository**:

```java
var databaseService = /* ... */;

// Takes array of objects
databaseService.initialize(Company.class);

// Initializes the repository
var companyRepository = new CompanyRepository(databaseService);
```

Now you may use the `CompanyRepository` to interact with the `Company` entity.

</step>

</procedure>

<procedure title="Using repositories" id="using-repositories" collapsible="true" default-state="expanded">

Using repositories we can perform various operations on the entities, such as finding (querying), saving and deleting them.
The `BaseRepository` abstract class provides some basic methods for these operations.

> **All interactions with the database are done within a transaction.**
> If you run a transaction within a thread that has an active transaction, **the same transaction will be used**.

```java
var companyRepository = /* ... */;

// Saves the company entity to the database
companyRepository.save(company);

// Saves all companies in the list to the database
companyRepository.saveAll(List.of(company1, company2));

// Finds all companies in the database
List<Company> companies = companyRepository.findAll();

// Finds a company by its ID
Optional<Company> optionalCompany = companyRepository.findById(1L);

// Deletes a company entity
companyRepository.delete(company);

// Deletes all companies in the list
companyRepository.deleteAll(List.of(company1, company2));

// Deletes a company by its ID
companyRepository.deleteById(1L);

// Querying for companies with a specific name
companyRepository.findByCriteria((root, query, cb) -> {
    return cb.equal(root.get("name", "Some Company"));
});
```

> For more information on querying, see the [Hibernate ORM documentation](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#criteria).

There are other methods, such as `#insert()` and `#update()`.
You may only insert an entity, which does not have an ID yet.
Similarly, you may only update an entity that has an ID.

> You may also implement the `RepositoryExtension` interface to add additional predefined methods.

</procedure>

<procedure title="Adding additional methods to repositories" id="adding-methods-to-repositories" collapsible="true" default-state="expanded">

In practice, we often need more than just basic CRUD operations. You may simply add new methods into your repository
to suit your needs. For example, let's say we want to find a company by its name:

```java
public class CompanyRepository extends BaseRepository<Company, Long> {
    
    /* ... */

    public Optional<Company> findByName(String name) {
        return findByCriteria((root, query, cb) -> {
            return cb.equal(root.get("name"), name);
        });
    }
}
```

But what if we need entirely different method that does not fit into the predefined methods? In that case,
we can use the database service's `runInThreadTransaction()` method to run a custom query. Using this method,
you may **access the Hibernate's `Session` instance** and create a custom query.

```java
public class CompanyRepository extends BaseRepository<Company, Long> {
    
    /* ... */

    public List<Company> containsName(String name) {
        return databaseService.runInThreadTransaction(session -> {
            return session.createQuery("SELECT c FROM Company c WHERE c.name LIKE :name", Company.class)
                .setParameter("name", name)
                .getResultStream()
                .toList();
        });
    }
}
```

</procedure>

<procedure title="Transactions" id="transactions" collapsible="true" default-state="expanded">

**All interactions with the database are done within a transaction** by default. You can run a transaction using the
`runInThreadTransaction()` method of the `IrminsulDatabaseService`. This method takes a lambda function
that receives a `Session` instance, which you can use to perform database operations, and returns the result of the transaction.

The repository methods use this method internally, so you don't have to worry about managing transactions yourself.

> There's also `runInThreadTransactionEmpty()` method, which does not return any result.

```java
databaseService.runInThreadTransaction(session -> {
    return session.createQuery("SELECT c FROM Company c WHERE c.name LIKE :name", Company.class)
        .setParameter("name", name)
        .getResultStream()
        .toList();
});
```

Any subsequent database operations within the same thread will use the same transaction. This means, if
you throw an exception anywhere inside the transaction, **the transaction will be rolled back** and no changes
will be committed to the database.

```java
databaseService.runInThreadTransaction(session -> {
    var company = new Company("Company");
    
    // Runs within the same transaction
    var savedCompany = companyRepository.save(company);

    throw new RuntimeException("Something went wrong!");
    // ^ Rollbacks the transactions
    // The company will not be saved
});
```

<warning>

**Transactions are not thread-safe!** You should not use the same transaction in multiple threads.

</warning>

> If a **transaction throws an exceptions**, it will be wrapped in a `TransactionException` and rethrown.

In the background, the `#runInThreadTransaction()` method uses a `IrminsulContext` to store the current session
and transaction.

</procedure>

<procedure title="After commit and rollback actions" id="commit-rollback-actions" collapsible="true" default-state="expanded">

Within a transactions, you may define a callback that will be executed after the transaction is committed or rolled back.

```java
databaseService.runInThreadTransaction(session -> {
    var company = new Company("Company");
    
    var savedCompany = companyRepository.save(company);
    
    IrminsulContext.addAfterCommitAction(() -> {
        // Executed after the transaction is committed
    });

    IrminsulContext.addRollbackAction(() -> {
        // Executed after the transaction is rolled back
    });
});
```

`IrminsulContext` is used to hold the current transaction context, including the session and transaction.

> These actions are ran in the same thread as the transaction.

> You may add multiple actions to be executed after the transaction is committed or rolled back.

<warning>
Throwing an exception in these actions may result in unexpected behavior.
</warning>

</procedure>

<procedure title="Extending the database service class" id="extending-database-service" collapsible="true" default-state="expanded">

You may extend the `IrminsulDatabaseService` class for extra configuration or functionality. There are some methods
that allows you to easily customize the logic around transactions.

```java
public class DatabaseExtension extends IrminsulDatabaseService {

    public DatabaseExtension(DatabaseServiceConfiguration configuration) {
        super(configuration);
    }

    @Override
    protected void beforeBeginTransaction(IrminsulContext ctx) {
    }

    @Override
    protected void afterBeginTransaction(IrminsulContext ctx) {
    }

    @Override
    protected void beforeCommitTransaction(IrminsulContext ctx) {
    }

    @Override
    protected void afterCommitTransaction(IrminsulContext ctx) {
    }

    @Override
    protected void beforeRollbackTransaction(IrminsulContext ctx) {
    }

    @Override
    protected void afterRollbackTransaction(IrminsulContext ctx) {
    }

    @Override
    protected void afterTransactionProcessing(IrminsulContext ctx) {
    }
}
```

These methods are invoked as follows:

| Method                          | Invoked when                                                  |
|---------------------------------|---------------------------------------------------------------|
| `#beforeBeginTransaction()`     | Before the transaction is started                             |
| `#afterBeginTransaction()`      | After the transaction is started                              |
| `#beforeCommitTransaction()`    | Before the transaction is committed                           |
| `#afterCommitTransaction()`     | After the transaction is committed                            |
| `#beforeRollbackTransaction()`  | Before the transaction is rolled back                         |
| `#afterRollbackTransaction()`   | After the transaction is rolled back                          |
| `#afterTransactionProcessing()` | After the transaction is processed (committed or rolled back) |

<warning>
You should not throw any exceptions in these methods, as it may lead to unexpected behavior.
</warning>

> If the `#beforeBeginTransaction()` is invoked, it is gauranteed that the `#afterTransactionProcessing()` will
> be invoked as well, regardless of whether the transaction was committed or rolled back.

</procedure>
