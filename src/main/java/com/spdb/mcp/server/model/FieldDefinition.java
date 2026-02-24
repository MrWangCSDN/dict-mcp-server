package com.spdb.mcp.server.model;

/**
 * 字段定义对象
 */
public class FieldDefinition {
    
    /**
     * 字段英文名
     */
    private String id;
    
    /**
     * 字段中文名
     */
    private String longname;
    
    /**
     * 字段类型
     */
    private String type;
    
    /**
     * 数据库字段名
     */
    private String dbname;
    
    /**
     * 字典引用 (如 MDict.A.afMdCntDsc)
     */
    private String ref;

    public FieldDefinition() {
    }

    public FieldDefinition(String id, String longname, String type, String dbname, String ref) {
        this.id = id;
        this.longname = longname;
        this.type = type;
        this.dbname = dbname;
        this.ref = ref;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLongname() {
        return longname;
    }

    public void setLongname(String longname) {
        this.longname = longname;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDbname() {
        return dbname;
    }

    public void setDbname(String dbname) {
        this.dbname = dbname;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String longname;
        private String type;
        private String dbname;
        private String ref;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder longname(String longname) {
            this.longname = longname;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder dbname(String dbname) {
            this.dbname = dbname;
            return this;
        }

        public Builder ref(String ref) {
            this.ref = ref;
            return this;
        }

        public FieldDefinition build() {
            return new FieldDefinition(id, longname, type, dbname, ref);
        }
    }
}
