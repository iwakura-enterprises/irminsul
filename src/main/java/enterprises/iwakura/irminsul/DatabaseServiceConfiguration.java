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

    protected @Builder.Default String jdbcDriver = "";
    protected @Builder.Default String dialect = "";
    protected @Builder.Default String url = "";
    protected @Builder.Default String username = "";
    protected @Builder.Default String password = "";
    protected @Builder.Default boolean debugSql = false;
    protected @Builder.Default int minIdleConnections = 1;
    protected @Builder.Default int maxConnections = 10;
    protected @Builder.Default String liquibaseChangelogFile = "liquibase/changelog.yaml";
    protected @Builder.Default String charset = "utf8mb4";
    protected @Builder.Default boolean useUnicode = true;
    protected @Builder.Default Action hbm2ddlAuto = Action.NONE;
}
