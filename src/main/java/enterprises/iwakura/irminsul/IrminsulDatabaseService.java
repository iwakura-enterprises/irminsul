package enterprises.iwakura.irminsul;

import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.JdbcSettings;

import enterprises.iwakura.irminsul.exception.TransactionException;
import jakarta.persistence.Entity;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import lombok.extern.slf4j.Slf4j;

/**
 * Irminsul's Database service class for managing database connections and transactions. You may extend this class for
 * custom configurations.
 */
@Slf4j
public class IrminsulDatabaseService {

    /**
     * The database configuration used for this service.
     */
    protected final DatabaseServiceConfiguration databaseConfiguration;

    /**
     * The Hibernate session factory used for database operations.
     */
    protected SessionFactory sessionFactory;

    /**
     * Creates a new DatabaseService instance with the given configuration.
     *
     * @param configuration the database configuration
     */
    public IrminsulDatabaseService(DatabaseServiceConfiguration configuration) {
        this.databaseConfiguration = configuration;
    }

    /**
     * Initializes the database service with the given entity classes and configuration.
     *
     * @param entityClasses the entity classes to initialize
     */
    public void initialize(Class<?>... entityClasses) {
        log.info("Initializing Irminsul's Hibernate...");
        try {
            Configuration configuration = new Configuration();
            populateHibernateConfiguration(configuration);
            populateEntityClasses(configuration, entityClasses);

            StandardServiceRegistryBuilder serviceRegistry = new StandardServiceRegistryBuilder();
            applyHibernateConfiguration(serviceRegistry, configuration.getProperties());

            sessionFactory = buildSessionFactory(configuration, serviceRegistry);
            log.info("Irminsul's Hibernate successfully initialized");
        } catch (Exception e) {
            throw new RuntimeException("Cannot initialize Irminsul's Hibernate", e);
        }
    }

    /**
     * Runs Liquibase migrations using the specified changelog file and resource accessor.
     *
     * @param changelogFile    the path to the Liquibase changelog file
     * @param resourceAccessor the resource accessor to use for loading resources
     */
    public void runLiquibase(String changelogFile, ResourceAccessor resourceAccessor) {
        log.info("Running Liquibase migrations...");
        // No need to wrap this in a transaction - Liquibase uses transactions by default.
        final var session = sessionFactory.openSession();
        session.doWork(connection -> {
            try {
                Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                Liquibase liquibase = new Liquibase(
                    changelogFile,
                    resourceAccessor,
                    database
                );
                liquibase.update(new Contexts(), new LabelExpression());
                liquibase.close(); // Closes the database connection
            } catch (Exception exception) {
                throw new RuntimeException("Failed to run Liquibase", exception);
            }
        });
        // No need to close the session - liquibase closes the connection.
    }

    /**
     * Runs Liquibase migrations using the specified changelog file and the given class loader to load resources.
     *
     * @param changelogFile the path to the Liquibase changelog file
     * @param classLoader   the class loader to use for loading resources
     */
    public void runLiquibaseFromClassLoader(String changelogFile, ClassLoader classLoader) {
        runLiquibase(changelogFile, new ClassLoaderResourceAccessor(classLoader));
    }

    /**
     * Shuts down the database service and closes the session factory.
     */
    public void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
            log.info("Irminsul's Hibernate session factory closed");
        }
    }

    /**
     * Checks if the session factory is initialized
     *
     * @return true if the session factory is initialized, false otherwise
     */
    public boolean isInitialized() {
        return sessionFactory != null && !sessionFactory.isClosed();
    }

    /**
     * Runs a transaction in the current thread context.
     *
     * @param transaction the transaction to run
     * @param <R>         the result type
     * @return the result of the transaction
     */
    public <R> R runInThreadTransaction(Function<Session, R> transaction) {
        // Run the transaction in current Irminsul context
        if (IrminsulContext.hasCurrent()) {
            final var ctx = IrminsulContext.getCurrent();
            logIfEnabled("[%d] Running transaction in current Irminsul context".formatted(ctx.getID()));
            return transaction.apply(
                ctx.getSession()); // Exceptions here will be caught and handled in the transaction logic
        }

        // Create new session and transaction
        try (Session session = sessionFactory.openSession()) {
            final var ctx = IrminsulContext.initializeCurrent(session);
            logIfEnabled("[%d] Initialized new Irminsul context; Begin transaction".formatted(ctx.getID()));
            try {
                beforeBeginTransaction(ctx);
                ctx.beginTransaction();
                afterBeginTransaction(ctx);
                R result = transaction.apply(session);
                logIfEnabled("[%d] Committing transaction in Irminsul context".formatted(ctx.getID()));
                beforeCommitTransaction(ctx);
                ctx.commit();
                logIfEnabled("[%d] Running after commit actions in Irminsul context".formatted(ctx.getID()));
                afterCommitTransaction(ctx);
                ctx.runAfterCommitActions();
                return result;
            } catch (Throwable throwable) {
                logIfEnabled("[%d] Exception occurred while running transaction, rolling back".formatted(ctx.getID()),
                    throwable);
                beforeRollbackTransaction(ctx);
                ctx.rollback();
                logIfEnabled("[%d] Running after rollback actions in Irminsul context".formatted(ctx.getID()));
                afterRollbackTransaction(ctx);
                ctx.runAfterRollbackActions();
                throw new TransactionException(ctx.getID(), throwable);
            } finally {
                try {
                    afterTransactionProcessing(ctx);
                } catch (Throwable throwable) {
                    log.error(
                        "[{}] Exception occurred while processing after transaction actions. This exception will not "
                            + "be rethrown. Please, do not throw exceptions in #afterTransactionProcessing()",
                        ctx.getID(), throwable);
                }
                ctx.clear();
            }
        } catch (HibernateException exception) {
            logIfEnabled("An error occurred while opening session", exception);
            throw exception;
        }
    }

    /**
     * Runs a transaction in the current thread context without returning a result.
     *
     * @param transaction the transaction to run
     */
    public void runInThreadTransactionEmpty(Consumer<Session> transaction) {
        runInThreadTransaction(session -> {
            transaction.accept(session);
            return null;
        });
    }

    /**
     * Invoked before the transaction begins. This method can be overridden to perform custom actions before the
     * transaction starts.
     *
     * @param ctx the Irminsul context for the current transaction
     */
    protected void beforeBeginTransaction(IrminsulContext ctx) {
    }

    /**
     * Invoked after the transaction begins. This method can be overridden to perform custom actions after the
     * transaction starts.
     *
     * @param ctx the Irminsul context for the current transaction
     */
    protected void afterBeginTransaction(IrminsulContext ctx) {
    }

    /**
     * Invoked before the transaction is committed. This method can be overridden to perform custom actions before the
     * transaction is committed.
     *
     * @param ctx the Irminsul context for the current transaction
     */
    protected void beforeCommitTransaction(IrminsulContext ctx) {
    }

    /**
     * Invoked after the transaction is committed. This method can be overridden to perform custom actions after the
     * transaction is committed.
     *
     * @param ctx the Irminsul context for the current transaction
     */
    protected void afterCommitTransaction(IrminsulContext ctx) {
    }

    /**
     * Invoked before the transaction is rolled back. This method can be overridden to perform custom actions before the
     * transaction is rolled back.
     *
     * @param ctx the Irminsul context for the current transaction
     */
    protected void beforeRollbackTransaction(IrminsulContext ctx) {
    }

    /**
     * Invoked after the transaction is rolled back. This method can be overridden to perform custom actions after the
     * transaction is rolled back.
     *
     * @param ctx the Irminsul context for the current transaction
     */
    protected void afterRollbackTransaction(IrminsulContext ctx) {
    }

    /**
     * Invoked after the transaction processing is completed. This method can be overridden to perform custom actions
     * after the transaction processing.
     *
     * @param ctx the Irminsul context for the current transaction
     */
    protected void afterTransactionProcessing(IrminsulContext ctx) {
    }

    /**
     * Populates the Hibernate configuration with the database configuration.
     *
     * @param configuration the Hibernate configuration to populate
     */
    protected void populateHibernateConfiguration(Configuration configuration) {
        // Hibernate properties
        Properties properties = new Properties();
        populateHibernateProperties(properties);
        configuration.setProperties(properties);
    }

    /**
     * Populates the entity classes for Hibernate.
     *
     * @param configuration the Hibernate configuration to populate
     * @param entityClasses the entity classes to add
     */
    protected void populateEntityClasses(Configuration configuration, Class<?>[] entityClasses) {
        for (Class<?> entityClass : entityClasses) {
            if (entityClass.isAnnotationPresent(Entity.class)) {
                logIfEnabled("Adding entity class %s to Hibernate configuration".formatted(entityClass.getName()));
                configuration.addAnnotatedClass(entityClass);
            } else {
                throw new IllegalArgumentException(
                    "Class %s is not annotated with @Entity".formatted(entityClass.getName()));
            }
        }
    }

    /**
     * Populates the Hibernate properties with the database configuration.
     *
     * @param properties the properties to populate
     */
    protected void populateHibernateProperties(Properties properties) {
        // Database connection settings
        properties.put(JdbcSettings.JAKARTA_JDBC_DRIVER, databaseConfiguration.getJdbcDriver());
        properties.put(JdbcSettings.JAKARTA_JDBC_URL, databaseConfiguration.getUrl());
        properties.put(JdbcSettings.JAKARTA_JDBC_USER, databaseConfiguration.getUsername());
        properties.put(JdbcSettings.JAKARTA_JDBC_PASSWORD, databaseConfiguration.getPassword());

        // HikariCP connection pool
        properties.put("hibernate.connection.provider_class",
            "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
        properties.put("hibernate.connection.CharSet", databaseConfiguration.getCharset());
        properties.put("hibernate.connection.characterEncoding", databaseConfiguration.getCharset());
        properties.put("hibernate.connection.useUnicode", String.valueOf(databaseConfiguration.isUseUnicode()));
        properties.put("hibernate.hikari.minimumIdle", String.valueOf(databaseConfiguration.getMinIdleConnections()));
        properties.put("hibernate.hikari.maximumPoolSize", String.valueOf(databaseConfiguration.getMaxConnections()));

        // SQL settings
        properties.put(Environment.DIALECT, databaseConfiguration.getDialect());
        properties.put(Environment.HBM2DDL_AUTO, databaseConfiguration.getHbm2ddlAuto().name().toLowerCase());
        properties.put(Environment.SHOW_SQL, databaseConfiguration.isDebugSql());
        properties.put(Environment.FORMAT_SQL, databaseConfiguration.isDebugSql());
    }

    /**
     * Applies the Hibernate configuration to the service registry.
     *
     * @param serviceRegistry the service registry to apply the configuration to
     * @param properties      the properties to apply
     */
    protected void applyHibernateConfiguration(StandardServiceRegistryBuilder serviceRegistry, Properties properties) {
        serviceRegistry.applySettings(properties);
    }

    /**
     * Builds the Hibernate session factory.
     *
     * @param configuration   the Hibernate configuration
     * @param serviceRegistry the service registry
     * @return the built session factory
     */
    protected SessionFactory buildSessionFactory(Configuration configuration,
        StandardServiceRegistryBuilder serviceRegistry) {
        return configuration.buildSessionFactory(serviceRegistry.build());
    }

    /**
     * Logs a message if SQL debugging is enabled.
     *
     * @param message the message to log
     */
    protected void logIfEnabled(String message) {
        if (databaseConfiguration.isDebugSql()) {
            log.info(message);
        }
    }

    /**
     * Logs an error message if error logging is enabled.
     *
     * @param message   the error message to log
     * @param throwable the throwable to log
     */
    protected void logIfEnabled(String message, Throwable throwable) {
        if (databaseConfiguration.isLogErrors()) {
            log.error(message, throwable);
        }
    }
}
