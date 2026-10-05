package com.healthy.appointment.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class MapperDomainBoundaryTest {
    private static final Pattern CROSS_DOMAIN_JOIN = Pattern.compile(
            "(?i)\\bjoin\\s+(patient|sys_user|doctor|department)\\b");

    @Test
    void bookingMappersDoNotJoinIdentityOrProviderTables() throws IOException {
        List<String> bookingMappers = List.of(
                "mapper/AppointmentMapper.xml",
                "mapper/AppointmentWaitlistMapper.xml",
                "mapper/DoctorScheduleSlotMapper.xml");

        for (String mapper : bookingMappers) {
            String sql = new ClassPathResource(mapper).getContentAsString(StandardCharsets.UTF_8);
            assertThat(CROSS_DOMAIN_JOIN.matcher(sql).find())
                    .as("%s must obtain identity/doctor data through domain ports", mapper)
                    .isFalse();
        }
    }
}
