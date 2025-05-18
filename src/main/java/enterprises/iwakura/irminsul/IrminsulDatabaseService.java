package enterprises.iwakura.irminsul;

import jakarta.persistence.Entity;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.JdbcSettings;

import java.util.Properties;
import java.util.function.Function;

/**
 * Irminsul's Database service class for managing database connections and transactions. You may extend this class
 * for custom configurations.
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
     * Runs Liquibase migrations
     */
    public void runLiquibase() {
        log.info("Running Liquibase migrations...");
        // No need to wrap this in a transaction - Liquibase uses transactions by default.
        final var session = sessionFactory.openSession();
        session.doWork(connection -> {
            try {
                Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
                Liquibase liquibase = new Liquibase(
                        databaseConfiguration.getLiquibaseChangelogFile(),
                        new ClassLoaderResourceAccessor(this.getClass().getClassLoader()),
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
     *
     * @return the result of the transaction
     */
    public <R> R runInThreadTransaction(Function<Session, R> transaction) {
        // Run the transaction in current Irminsul context
        if (IrminsulContext.hasCurrent()) {
            final var ctx = IrminsulContext.getCurrent();
            logIfEnabled("Running transaction in current Irminsul context (ID %d)".formatted(ctx.getID()));
            return transaction.apply(ctx.getSession());
        }

        // Create new session and transaction
        try (Session session = sessionFactory.openSession()) {
            final var ctx = IrminsulContext.initializeCurrent(session);
            logIfEnabled("Initialized new Irminsul context (ID %d); Begin transaction".formatted(ctx.getID()));
            ctx.beginTransaction();
            R result;
            try {
                result = transaction.apply(session);
                logIfEnabled("Committing transaction in Irminsul context (ID %d)".formatted(ctx.getID()));
                ctx.commit();
                logIfEnabled("Running after commit actions in Irminsul context (ID %d)".formatted(ctx.getID()));
                ctx.runAfterCommitActions();
            } catch (Throwable throwable) {
                log.error("Rolling back transaction in Irminsul context (ID {}) due to exception", ctx.getID(), throwable);
                ctx.rollback();
                logIfEnabled("Running after rollback actions in Irminsul context (ID %d)".formatted(ctx.getID()));
                ctx.runAfterRollbackActions();
                throw new RuntimeException("Exception occurred while running transaction", throwable);
            } finally {
                ctx.clear();
            }
            return result;
        }
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
                throw new IllegalArgumentException("Class %s is not annotated with @Entity".formatted(entityClass.getName()));
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
        properties.put("hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
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
     *
     * @return the built session factory
     */
    protected SessionFactory buildSessionFactory(Configuration configuration, StandardServiceRegistryBuilder serviceRegistry) {
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
}
