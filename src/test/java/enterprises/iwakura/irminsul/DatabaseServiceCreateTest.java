package enterprises.iwakura.irminsul;

import enterprises.iwakura.irminsul.entity.Company;
import enterprises.iwakura.irminsul.entity.Employee;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class DatabaseServiceCreateTest extends DatabaseTest {

    @Test
    public void createDatabaseService() {
        final var databaseService = new DatabaseService(applyTestcontainersConfig(new DatabaseServiceConfiguration()));
        databaseService.initialize(
                Company.class,
                Employee.class
        );
        databaseService.shutdown();
    }

    @Test
    public void invalidDialectFail() {
        final var config = applyTestcontainersConfig(new DatabaseServiceConfiguration());
        // We are using PostgreSQL, so this should fail
        config.setDialect("org.hibernate.dialect.MariaDBDialect");
        final var databaseService = new DatabaseService(config);

        assertThrows(RuntimeException.class, () -> databaseService.initialize(
                Company.class,
                Employee.class
        ));

        databaseService.shutdown();
    }

    @Test
    public void invalidEntityClassFail() {
        final var databaseService = new DatabaseService(applyTestcontainersConfig(new DatabaseServiceConfiguration()));
        assertThrows(RuntimeException.class, () -> databaseService.initialize(
                Company.class,
                Employee.class,
                String.class // Invalid entity class
        ));

        databaseService.shutdown();
    }
}
