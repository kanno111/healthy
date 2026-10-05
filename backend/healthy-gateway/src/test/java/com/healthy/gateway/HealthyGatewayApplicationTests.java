package com.healthy.gateway;

import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import com.healthy.gateway.filter.GatewayAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.sentinel.enabled=false"
})
class HealthyGatewayApplicationTests {
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void contextLoads() {
        assertThat(applicationContext.containsBean("sentinel-json-gw-flow-converter")).isTrue();
        assertThat(applicationContext.getBean(SentinelGatewayFilter.class)).isNotNull();
        assertThat(applicationContext.getBean(SentinelGatewayBlockExceptionHandler.class)).isNotNull();
        assertThat(applicationContext.getBean(GatewayAuthenticationFilter.class)).isNotNull();
    }

    @Test
    void definesDoctorServiceDiscoveryRoute() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();

        assertThat(routes)
                .isNotNull()
                .filteredOn(route -> "healthy-doctor-internal".equals(route.getId()))
                .singleElement()
                .satisfies(route -> {
                    assertThat(route.getUri()).hasToString("lb://healthy-doctor");
                    assertThat(route.getPredicates()).singleElement()
                            .satisfies(predicate -> {
                                assertThat(predicate.getName()).isEqualTo("Path");
                                assertThat(predicate.getArgs()).containsValue("/doctor-service/actuator/health");
                            });
                    assertThat(route.getFilters())
                            .extracting(filter -> filter.getName())
                            .containsExactly("StripPrefix", "PrefixPath");
                });
    }

    @Test
    void routesAuthApisToIdentityServiceBeforeServerCatchAll() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();

        assertThat(routes).isNotNull();
        RouteDefinition identityRoute = routes.stream()
                .filter(route -> "healthy-identity-auth".equals(route.getId()))
                .findFirst()
                .orElseThrow();
        RouteDefinition serverRoute = routes.stream()
                .filter(route -> "healthy-server".equals(route.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(identityRoute.getUri()).hasToString("lb://healthy-identity");
        assertThat(identityRoute.getOrder()).isLessThan(serverRoute.getOrder());
        assertThat(identityRoute.getPredicates()).singleElement()
                .satisfies(predicate -> {
                    assertThat(predicate.getName()).isEqualTo("Path");
                    assertThat(predicate.getArgs()).containsValue("/api/auth/**");
                });
    }

    @Test
    void routesDoctorAdminApisToDoctorServiceBeforeServerCatchAll() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();

        assertThat(routes).isNotNull();
        RouteDefinition doctorAdminRoute = routes.stream()
                .filter(route -> "healthy-doctor-admin".equals(route.getId()))
                .findFirst()
                .orElseThrow();
        RouteDefinition serverRoute = routes.stream()
                .filter(route -> "healthy-server".equals(route.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(doctorAdminRoute.getUri()).hasToString("lb://healthy-doctor");
        assertThat(doctorAdminRoute.getOrder()).isLessThan(serverRoute.getOrder());
        assertThat(doctorAdminRoute.getPredicates()).singleElement()
                .satisfies(predicate -> {
                    assertThat(predicate.getName()).isEqualTo("Path");
                    assertThat(predicate.getArgs())
                            .containsValue("/api/admin/doctors")
                            .containsValue("/api/admin/doctors/**")
                            .containsValue("/api/admin/departments")
                            .containsValue("/api/admin/departments/**");
                });
    }

    @Test
    void routesPatientDepartmentApiToDoctorServiceBeforeServerCatchAll() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();

        assertThat(routes).isNotNull();
        RouteDefinition departmentRoute = routes.stream()
                .filter(route -> "healthy-doctor-user-departments".equals(route.getId()))
                .findFirst()
                .orElseThrow();
        RouteDefinition serverRoute = routes.stream()
                .filter(route -> "healthy-server".equals(route.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(departmentRoute.getUri()).hasToString("lb://healthy-doctor");
        assertThat(departmentRoute.getOrder()).isLessThan(serverRoute.getOrder());
        assertThat(departmentRoute.getPredicates()).singleElement()
                .satisfies(predicate -> {
                    assertThat(predicate.getName()).isEqualTo("Path");
                    assertThat(predicate.getArgs()).containsValue("/api/user/departments");
                });
    }
}
