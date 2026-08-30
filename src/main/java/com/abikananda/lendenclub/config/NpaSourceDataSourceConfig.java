package com.abikananda.lendenclub.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
@ConditionalOnProperty(prefix = "npa-import", name = "enabled", havingValue = "true")
public class NpaSourceDataSourceConfig {

    @Bean
    @ConfigurationProperties("npa-import.datasource")
    public DataSourceProperties npaSourceDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "npaSourceJdbcTemplate")
    public JdbcTemplate npaSourceJdbcTemplate() {
        DataSourceProperties properties = npaSourceDataSourceProperties();
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        if (properties.getDriverClassName() != null && !properties.getDriverClassName().isBlank()) {
            dataSource.setDriverClassName(properties.getDriverClassName());
        }
        return new JdbcTemplate(dataSource);
    }
}
