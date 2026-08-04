package com.dbtraining.tradeflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class DatabaseMigrationH2Test {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoadsWithDevProfile() {
        assertThat(applicationContext).isNotNull();
    }
}
