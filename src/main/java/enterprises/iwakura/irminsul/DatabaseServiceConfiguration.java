package enterprises.iwakura.irminsul;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.tool.schema.Action;

/**
 * Database service configuration
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseServiceConfiguration {

    /**
     * JDBC driver class name
     */
    protected @Builder.Default String jdbcDriver = "";

    /**
     * Hibernate dialect class name
     */
    protected @Builder.Default String dialect = "";

    /**
     * JDBC URL
     */
    protected @Builder.Default String url = "";

    /**
     * JDBC username
     */
    protected @Builder.Default String username = "";

    /**
     * JDBC password
     */
    protected @Builder.Default String password = "";

    /**
     * If Hibernate should show SQL statements in the console and Irminsul should log some debug logs
     */
    protected @Builder.Default boolean debugSql = false;

    /**
     * Minimum number of idle connections in the pool
     */
    protected @Builder.Default int minIdleConnections = 1;

    /**
     * Maximum number of idle connections in the pool
     */
    protected @Builder.Default int maxConnections = 10;

    /**
     * Path to the Liquibase changelog file in resources
     */
    protected @Builder.Default String liquibaseChangelogFile = "liquibase/changelog.yaml";

    /**
     * Charset to use for database operations
     */
    protected @Builder.Default String charset = "utf8mb4";

    /**
     * If the database should use Unicode
     */
    protected @Builder.Default boolean useUnicode = true;

    /**
     * The HBM2DDL auto action to perform on startup
     */
    protected @Builder.Default Action hbm2ddlAuto = Action.NONE;
}
