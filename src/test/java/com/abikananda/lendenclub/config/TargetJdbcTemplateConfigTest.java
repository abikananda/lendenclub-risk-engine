package com.abikananda.lendenclub.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TargetJdbcTemplateConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean("dataSource", DataSource.class, () -> mock(DataSource.class))
            .withUserConfiguration(TargetJdbcTemplateConfig.class, SeleniumSourceDataSourceConfig.class)
            .withPropertyValues(
                    "selenium-borrower-import.enabled=true",
                    "selenium-borrower-import.datasource.url=jdbc:mysql://localhost/pfmp",
                    "selenium-borrower-import.datasource.username=reader",
                    "selenium-borrower-import.datasource.password=secret");

    @Test
    void keepsUnqualifiedJdbcTemplateOnTargetDatabaseWhenSourceImportIsEnabled() {
        contextRunner.run(context -> {
            JdbcTemplate target = context.getBean(JdbcTemplate.class);
            JdbcTemplate source = context.getBean("seleniumSourceJdbcTemplate", JdbcTemplate.class);
            DataSource targetDataSource = context.getBean("dataSource", DataSource.class);

            assertThat(target).isSameAs(context.getBean("targetJdbcTemplate"));
            assertThat(target.getDataSource()).isSameAs(targetDataSource);
            assertThat(source).isNotSameAs(target);
            assertThat(source.getDataSource()).isNotSameAs(targetDataSource);
        });
    }
}
