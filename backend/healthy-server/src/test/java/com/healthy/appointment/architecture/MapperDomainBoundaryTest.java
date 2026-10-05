package com.healthy.appointment.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class MapperDomainBoundaryTest {
    private static final Pattern CROSS_DOMAIN_JOIN = Pattern.compile(
            "(?i)\\b(?:from|join)\\s+(patient|sys_user|doctor|department)\\b");

    @Test
    void bookingMappersDoNotReadIdentityOrDoctorMasterTables() throws IOException {
        Resource[] bookingMappers = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/*.xml");

        for (Resource mapper : bookingMappers) {
            String sql = mapper.getContentAsString(StandardCharsets.UTF_8);
            assertThat(CROSS_DOMAIN_JOIN.matcher(sql).find())
                    .as("%s must obtain identity/doctor data through domain ports", mapper.getFilename())
                    .isFalse();
        }
    }
}
