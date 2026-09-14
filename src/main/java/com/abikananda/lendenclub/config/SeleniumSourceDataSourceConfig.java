package com.abikananda.lendenclub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
@ConditionalOnProperty(prefix = "selenium-borrower-import", name = "enabled", havingValue = "true")
public class SeleniumSourceDataSourceConfig {

    @Bean(name = "seleniumSourceJdbcTemplate")
    JdbcTemplate seleniumSourceJdbcTemplate(
            @Value("${selenium-borrower-import.datasource.url}") String url,
            @Value("${selenium-borrower-import.datasource.username}") String username,
            @Value("${selenium-borrower-import.datasource.password}") String password,
            @Value("${selenium-borrower-import.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}") String driver) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driver);
        return new JdbcTemplate(dataSource);
    }
}
