package com.healthy.appointment.service;

import com.healthy.appointment.service.impl.RedisAppointmentStockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAppointmentStockServiceTest {
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void initializeIfAbsentUsesSetNxAndNeverOverwritesStock() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        RedisAppointmentStockService service = new RedisAppointmentStockService(stringRedisTemplate);

        service.initializeIfAbsent(10L, 8);

        verify(valueOperations).setIfAbsent("appointment:stock:10", "8");
        verify(valueOperations, never()).set(eq("appointment:stock:10"), any(String.class));
    }

    @Test
    void restoreUnexpectedLuaResultIsLoggedAndDoesNotThrow() {
        when(stringRedisTemplate.execute(any(org.springframework.data.redis.core.script.RedisScript.class), anyList()))
                .thenReturn(0L);
        RedisAppointmentStockService service = new RedisAppointmentStockService(stringRedisTemplate);

        assertThatCode(() -> service.restore(10L)).doesNotThrowAnyException();
    }

    @Test
    void restoreScriptReturnsFixedSuccessFlagInsteadOfIncrementedStock() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList())).thenReturn(1L);
        RedisAppointmentStockService service = new RedisAppointmentStockService(stringRedisTemplate);

        service.restore(10L);

        ArgumentCaptor<RedisScript<Long>> scriptCaptor = ArgumentCaptor.forClass(RedisScript.class);
        verify(stringRedisTemplate).execute(scriptCaptor.capture(), eq(java.util.List.of("appointment:stock:10")));
        assertThat(scriptCaptor.getValue().getScriptAsString())
                .contains("redis.call('INCR', KEYS[1])")
                .contains("return 1");
    }

    @Test
    void adjustByDeltaKeepsMissingKeyMissing() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(String.class))).thenReturn(0L);
        RedisAppointmentStockService service = new RedisAppointmentStockService(stringRedisTemplate);

        assertThat(service.adjustByDeltaIfPresent(10L, 5)).isFalse();

        ArgumentCaptor<RedisScript<Long>> scriptCaptor = ArgumentCaptor.forClass(RedisScript.class);
        verify(stringRedisTemplate).execute(scriptCaptor.capture(), eq(java.util.List.of("appointment:stock:10")), eq("5"));
        assertThat(scriptCaptor.getValue().getScriptAsString())
                .contains("redis.call('EXISTS', KEYS[1])")
                .contains("redis.call('INCRBY', KEYS[1], delta)")
                .doesNotContain("redis.call('SET'");
        verify(stringRedisTemplate, never()).opsForValue();
    }
}
