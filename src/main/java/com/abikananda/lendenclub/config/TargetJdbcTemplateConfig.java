package com.abikananda.lendenclub.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Keeps application writes pinned to the primary risk-engine database when
 * optional read-only import data sources are enabled.
 */
@Configuration
public class TargetJdbcTemplateConfig {

    @Bean(name = "targetJdbcTemplate")
    @Primary
    JdbcTemplate targetJdbcTemplate(@Qualifier("dataSource") DataSource targetDataSource) {
        return new JdbcTemplate(targetDataSource);
    }
}
