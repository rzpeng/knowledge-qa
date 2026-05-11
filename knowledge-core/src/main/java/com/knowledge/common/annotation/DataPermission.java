package com.knowledge.common.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {
    String tableAlias() default "";
    String deptIdField() default "dept_id";
    String regionIdField() default "region_id";
    String createByField() default "create_by";
    boolean enableDept() default true;
    boolean enableRegion() default true;
}
