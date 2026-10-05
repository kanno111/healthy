package com.healthy.gateway.config;

import com.alibaba.cloud.sentinel.datasource.converter.JsonConverter;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class SentinelGatewayConfiguration {
    /**
     * The regular Sentinel WebFlux integration protects URL resources. Gateway
     * route and API-group rules additionally need the dedicated gateway filter.
     */
    @Bean
    @Order(-1)
    SentinelGatewayFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler(
            ObjectProvider<ViewResolver> viewResolverProvider,
            ServerCodecConfigurer serverCodecConfigurer) {
        List<ViewResolver> viewResolvers = viewResolverProvider.orderedStream().toList();
        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }

    /**
     * Spring Cloud Alibaba 2025.1.0.0 recognizes the gw-flow rule type but does not
     * auto-register its JSON converter. Keep the conventional bean name expected by
     * SentinelDataSourceHandler so Nacos-backed gateway rules can be constructed.
     */
    @Bean("sentinel-json-gw-flow-converter")
    JsonConverter<GatewayFlowRule> gatewayFlowRuleJsonConverter() {
        return new JsonConverter<>(JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build(), GatewayFlowRule.class);
    }
}
