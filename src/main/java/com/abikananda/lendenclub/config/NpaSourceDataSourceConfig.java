package com.abikananda.lendenclub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
@ConditionalOnProperty(prefix = "npa-import", name = "enabled", havingValue = "true")
public class NpaSourceDataSourceConfig {

    @Bean(name = "npaSourceJdbcTemplate")
    public JdbcTemplate npaSourceJdbcTemplate(
            @Value("${npa-import.datasource.url}") String url,
            @Value("${npa-import.datasource.username}") String username,
            @Value("${npa-import.datasource.password}") String password,
            @Value("${npa-import.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}") String driverClassName) {

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        if (driverClassName != null && !driverClassName.isBlank()) {
            dataSource.setDriverClassName(driverClassName);
        }
        return new JdbcTemplate(dataSource);
    }
}
