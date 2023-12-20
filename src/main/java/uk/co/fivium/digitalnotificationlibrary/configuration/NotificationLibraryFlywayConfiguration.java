package uk.co.fivium.digitalnotificationlibrary.configuration;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
class NotificationLibraryFlywayConfiguration {

  private static final String MIGRATION_HISTORY_TABLE_NAME = "notification_library_flyway_schema_history";
  private static final String MIGRATION_FILE_LOCATION = "classpath:db/notification-library-migration";

  NotificationLibraryFlywayConfiguration(@Value("${spring.flyway.schemas}") String[] existingSchemas,
                                         DataSource dataSource) {
    Flyway.configure()
        .dataSource(dataSource) // use the existing datasource
        .schemas(existingSchemas) // and these schemas
        .table(MIGRATION_HISTORY_TABLE_NAME) // use this table to keep track of migrations
        .baselineOnMigrate(true) // create the schema history table
        .baselineVersion("0") // with version 0, our migrations will start from V1
        .locations(MIGRATION_FILE_LOCATION) // look for migrations here
        .load()
        .migrate();
  }
}
