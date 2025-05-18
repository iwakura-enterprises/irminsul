package enterprises.iwakura.irminsul;

import org.hibernate.tool.schema.Action;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class DatabaseTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER;

    static {
        POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:latest")
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpassword");
        POSTGRESQL_CONTAINER.start();
    }

    /**
     * Creates a new DatabaseServiceConfiguration with the PostgreSQL container's connection details.
     *
     * @param configuration the configuration to apply the PostgreSQL connection details to
     */
    protected DatabaseServiceConfiguration applyTestcontainersConfig(DatabaseServiceConfiguration configuration) {
        configuration.setUrl("jdbc:postgresql://" + POSTGRESQL_CONTAINER.getHost() + ":" + POSTGRESQL_CONTAINER.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT) + "/testdb");
        configuration.setUsername(POSTGRESQL_CONTAINER.getUsername());
        configuration.setPassword(POSTGRESQL_CONTAINER.getPassword());
        configuration.setJdbcDriver("org.postgresql.Driver");
        configuration.setDialect("org.hibernate.dialect.PostgreSQLDialect");
        configuration.setDebugSql(true);
        configuration.setHbm2ddlAuto(Action.UPDATE);
        return configuration;
    }
}
