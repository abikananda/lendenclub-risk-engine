package com.abikananda.lendenclub.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(prefix = "npa-import", name = "enabled", havingValue = "true")
public class NpaSourceDataSourceConfig {

    @Bean
    @ConfigurationProperties("npa-import.datasource")
    public DataSourceProperties npaSourceDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "npaSourceDataSource")
    public DataSource npaSourceDataSource() {
        return npaSourceDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean(name = "npaSourceJdbcTemplate")
    public JdbcTemplate npaSourceJdbcTemplate(
            @Qualifier("npaSourceDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
