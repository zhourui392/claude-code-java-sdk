package com.anthropic.claude.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具定义类，包含工具的元数据信息
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class ToolDefinition {

    @JsonProperty("type")
    private final String type = "function";

    @JsonProperty("function")
    private final FunctionSpec function;

    private final Method method;
    private final Object instance;
    private final boolean async;
    private final long timeout;
    private final int priority;

    public ToolDefinition(String name, String description, Method method, Object instance,
                         boolean async, long timeout, int priority) {
        this.method = method;
        this.instance = instance;
        this.async = async;
        this.timeout = timeout;
        this.priority = priority;
        this.function = new FunctionSpec(name, description, method);
    }

    public String getType() {
        return type;
    }

    public FunctionSpec getFunction() {
        return function;
    }

    public Method getMethod() {
        return method;
    }

    public Object getInstance() {
        return instance;
    }

    public boolean isAsync() {
        return async;
    }

    public long getTimeout() {
        return timeout;
    }

    public int getPriority() {
        return priority;
    }

    public String getName() {
        return function.getName();
    }

    /**
     * 函数规格定义
     */
    public static class FunctionSpec {
        @JsonProperty("name")
        private final String name;

        @JsonProperty("description")
        private final String description;

        @JsonProperty("parameters")
        private final ParameterSchema parameters;

        public FunctionSpec(String name, String description, Method method) {
            this.name = name;
            this.description = description;
            this.parameters = new ParameterSchema(method);
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public ParameterSchema getParameters() {
            return parameters;
        }
    }

    /**
     * 参数模式定义
     */
    public static class ParameterSchema {
        @JsonProperty("type")
        private final String type = "object";

        @JsonProperty("properties")
        private final Map<String, PropertySpec> properties;

        @JsonProperty("required")
        private final List<String> required;

        public ParameterSchema(Method method) {
            this.properties = new HashMap<>();
            this.required = new ArrayList<>();

            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                Class<?> paramType = param.getType();

                // 获取参数名称
                String paramName = getParameterName(param, i);

                // 分析参数类型和注解
                PropertySpec spec = new PropertySpec(paramType, param);
                properties.put(paramName, spec);

                // 检查是否为必需参数
                if (isRequiredParameter(param, paramType)) {
                    required.add(paramName);
                }
            }
        }

        private String getParameterName(Parameter param, int index) {
            // 首先检查@Param注解
            Param paramAnnotation = param.getAnnotation(Param.class);
            if (paramAnnotation != null) {
                return paramAnnotation.value();
            }

            // 尝试获取实际参数名
            if (param.isNamePresent()) {
                return param.getName();
            }

            // 使用默认参数名
            return "arg" + index;
        }

        private boolean isRequiredParameter(Parameter param, Class<?> paramType) {
            Param paramAnnotation = param.getAnnotation(Param.class);
            if (paramAnnotation != null) {
                return paramAnnotation.required();
            }

            // Optional类型不是必需的
            if (paramType.equals(java.util.Optional.class)) {
                return false;
            }

            // 基本类型通常是必需的
            return paramType.isPrimitive();
        }

        public String getType() {
            return type;
        }

        public Map<String, PropertySpec> getProperties() {
            return properties;
        }

        public List<String> getRequired() {
            return required;
        }
    }

    /**
     * 属性规格定义
     */
    public static class PropertySpec {
        @JsonProperty("type")
        private final String type;

        @JsonProperty("description")
        private final String description;

        public PropertySpec(Class<?> javaType) {
            this.type = mapJavaTypeToJsonType(javaType);
            this.description = "";
        }

        public PropertySpec(Class<?> javaType, Parameter param) {
            this.type = mapJavaTypeToJsonType(javaType);

            // 从@Param注解获取描述
            Param paramAnnotation = param.getAnnotation(Param.class);
            this.description = paramAnnotation != null ? paramAnnotation.description() : "";
        }

        private String mapJavaTypeToJsonType(Class<?> javaType) {
            if (javaType == String.class) {
                return "string";
            } else if (javaType == Integer.class || javaType == int.class ||
                      javaType == Long.class || javaType == long.class) {
                return "integer";
            } else if (javaType == Double.class || javaType == double.class ||
                      javaType == Float.class || javaType == float.class) {
                return "number";
            } else if (javaType == Boolean.class || javaType == boolean.class) {
                return "boolean";
            } else if (javaType.isArray() || List.class.isAssignableFrom(javaType)) {
                return "array";
            } else {
                return "object";
            }
        }

        public String getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }
    }
}