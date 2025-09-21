package com.anthropic.claude.tools;

import java.lang.annotation.*;

/**
 * 标记自定义工具方法的注解
 * 用于自动识别和注册可以被Claude调用的工具方法
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Tool {

    /**
     * 工具名称，如果未指定则使用方法名
     * @return 工具名称
     */
    String name() default "";

    /**
     * 工具描述
     * @return 工具描述
     */
    String description() default "";

    /**
     * 是否为异步工具，默认为false
     * @return 是否异步执行
     */
    boolean async() default false;

    /**
     * 工具执行超时时间(毫秒)，默认30秒
     * @return 超时时间
     */
    long timeout() default 30000;

    /**
     * 工具优先级，数字越小优先级越高
     * @return 优先级
     */
    int priority() default 100;
}