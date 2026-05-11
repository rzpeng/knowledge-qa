package com.knowledge.common.interceptor;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.knowledge.common.annotation.DataPermission;
import com.knowledge.common.annotation.DataScopeService;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class DataPermissionInterceptor implements InnerInterceptor {

    private final DataScopeService dataScopeService;

    public DataPermissionInterceptor(DataScopeService dataScopeService) {
        this.dataScopeService = dataScopeService;
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        // Skip if not authenticated
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return;
        }

        // Check for @DataPermission annotation
        DataPermission dataPermission = getDataPermissionAnnotation(ms);
        if (dataPermission == null) {
            return;
        }

        // Only intercept SELECT statements
        String originalSql = boundSql.getSql();
        if (originalSql == null || !originalSql.trim().toUpperCase().startsWith("SELECT")) {
            return;
        }

        // Build data scope SQL
        String dataSql = dataScopeService.buildDataScopeSql(dataPermission);
        if (!StringUtils.hasText(dataSql)) {
            return;
        }

        // Append WHERE condition
        String newSql;
        if (originalSql.toUpperCase().contains("WHERE")) {
            newSql = originalSql + " AND (" + dataSql + ")";
        } else {
            newSql = originalSql + " WHERE " + dataSql;
        }

        // Inject modified SQL via reflection
        try {
            Field field = BoundSql.class.getDeclaredField("sql");
            field.setAccessible(true);
            field.set(boundSql, newSql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject data permission SQL", e);
        }
    }

    private DataPermission getDataPermissionAnnotation(MappedStatement ms) {
        try {
            String id = ms.getId();
            Class<?> clazz = Class.forName(id.substring(0, id.lastIndexOf('.')));
            String methodName = id.substring(id.lastIndexOf('.') + 1);

            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName) && method.isAnnotationPresent(DataPermission.class)) {
                    return method.getAnnotation(DataPermission.class);
                }
            }
            if (clazz.isAnnotationPresent(DataPermission.class)) {
                return clazz.getAnnotation(DataPermission.class);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
