package com.anthropic.claude.tools;

import java.lang.annotation.*;

/**
 * 工具方法参数注解
 * 用于显式指定参数名称，解决Java编译时参数名丢失的问题
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {

    /**
     * 参数名称
     * @return 参数名称
     */
    String value();

    /**
     * 参数描述
     * @return 参数描述
     */
    String description() default "";

    /**
     * 是否为必需参数
     * @return 是否必需
     */
    boolean required() default true;
}