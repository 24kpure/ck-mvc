package com.lmj.ckmvc.constant;

/**
 * canal 枚举
 */
public enum CanalFieldEnum {

    /**
     * 类型
     */
    TYPE("type", "类型"),

    /**
     * 表名
     */
    TABLE("table", "表名"),

    /**
     * 是否DDL语句
     */
    IS_DDL("isDdl", "是否DDL语句"),

    /**
     * 变更后的新文档数据
     */
    DATA("data", "变更后的新文档数据"),

    /**
     * 需要更新的旧的字段的值
     */
    OLD("old", "需要更新的旧的字段的值");

    CanalFieldEnum(String key, String value) {
        this.key = key;
        this.value = value;
    }

    private String key;

    private String value;

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}
