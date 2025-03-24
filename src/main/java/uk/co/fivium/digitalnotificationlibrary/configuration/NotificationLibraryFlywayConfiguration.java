package uk.co.fivium.digitalnotificationlibrary.configuration;

import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NotificationLibraryConfigurationProperties.class)
class NotificationLibraryFlywayConfiguration {

  private static final String MIGRATION_HISTORY_TABLE_NAME = "notification_library_flyway_schema_history";
  private static final String MIGRATION_FILE_LOCATION = "classpath:db/notification-library-migration/%s";

  NotificationLibraryFlywayConfiguration(@Value("${spring.flyway.schemas}") String[] existingSchemas,
                                         DataSource dataSource,
                                         NotificationLibraryConfigurationProperties properties,
                                         DataSourceProperties dataSourceProperties) {
    var vendor = properties.flywayVendor() != null
        ? properties.flywayVendor()
        : "postgresql";

    DataSource selectedDataSource = StringUtils.isNotBlank(properties.flywayUser())
        ? overrideDataSource(dataSourceProperties, properties.flywayUser())
        : dataSource;

    Flyway.configure()
        .dataSource(selectedDataSource) // use the existing datasource
        .schemas(existingSchemas) // and these schemas
        .table(MIGRATION_HISTORY_TABLE_NAME) // use this table to keep track of migrations
        .baselineOnMigrate(true) // create the schema history table
        .baselineVersion("0") // with version 0, our migrations will start from V1
        .locations(MIGRATION_FILE_LOCATION.formatted(vendor)) // look for migrations here
        .load()
        .migrate();
  }

  private DataSource overrideDataSource(DataSourceProperties dataSourceProperties, String flywayUser) {
    return DataSourceBuilder.create()
        .driverClassName(dataSourceProperties.getDriverClassName())
        .url(dataSourceProperties.getUrl())
        .username(flywayUser)
        .password(dataSourceProperties.getPassword())
        .build();
  }
}
