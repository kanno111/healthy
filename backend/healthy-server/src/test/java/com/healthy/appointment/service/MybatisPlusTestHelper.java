package com.healthy.appointment.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

final class MybatisPlusTestHelper {
    private MybatisPlusTestHelper() {
    }

    static void initializeTableInfo(Class<?>... entityTypes) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "unit-test-mapper");
        for (Class<?> entityType : entityTypes) {
            if (TableInfoHelper.getTableInfo(entityType) == null) {
                TableInfoHelper.initTableInfo(assistant, entityType);
            }
        }
    }
}
