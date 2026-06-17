package com.berdachuk.aichat.core.repository.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class SqlInjectBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(SqlInjectBeanPostProcessor.class);

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(InjectSql.class)) {
                continue;
            }
            String filePath = field.getAnnotation(InjectSql.class).value();
            try {
                ClassPathResource resource = new ClassPathResource(filePath);
                if (!resource.exists()) {
                    throw new BeanCreationException(beanName,
                            "SQL file not found: " + filePath + " for field " + field.getName()
                                    + " in " + bean.getClass().getName());
                }
                String sql;
                try (var in = resource.getInputStream()) {
                    sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                sql = sql.trim().replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
                field.setAccessible(true);
                ReflectionUtils.setField(field, bean, sql);
                log.debug("Injected SQL from {} into field {} of bean {}", filePath, field.getName(), beanName);
            } catch (IOException e) {
                throw new BeanCreationException(beanName,
                        "Failed to load SQL file: " + filePath + " for field " + field.getName()
                                + " in " + bean.getClass().getName(),
                        e);
            }
        }
        return bean;
    }
}
