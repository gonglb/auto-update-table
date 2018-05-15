package com.github.gonglb.tools.autoupdatetable.model;

import javax.persistence.Table;

/**
 * 列结构
 * @author gonglb
 * 2018年5月11日 下午2:49:16
 */
@Table(name="information_schema.columns")
public class SysMysqlColumns {

	private String tableCatalog;
	/**
	 * 库名
	 */
	private String tableSchema;
	/**
	 * 表名
	 */
	private String tableName;
	/**
	 * 字段名
	 */
	private String columnName;
	/**
	 * 字段位置的排序
	 */
	private String ordinalPosition;
	/**
	 * 字段默认值
	 */
	private String columnDefault;
	/**
	 * 是否可以为null
	 */
	private String isNullable;
	/**
	 * 字段类型
	 */
	private String dataType;
	
	private String characterMaximumLength;
	
	private String characterOctetLength;
	/**
	 * 长度
	 */
	private String numericPrecision;
	/**
	 * 小数点数
	 */
	private String numericScale;
	
	private String characterSetName;
	
	private String collationName;
	/**
	 * 类型加长度拼接的字符串，例如varchar(100)
	 */
	private String columnType;
	/**
	 * 主键:PRI；唯一键:UNI
	 */
	private String columnKey;
	/**
	 * 是否为自动增长，是的话为auto_increment
	 */
	private String extra;
	
	private String privileges;
	
	/**
	 * 注释
	 */
	private String columnComment;
	public String getTableCatalog() {
		return tableCatalog;
	}
	public void setTableCatalog(String tableCatalog) {
		this.tableCatalog = tableCatalog;
	}
	public String getTableSchema() {
		return tableSchema;
	}
	public void setTableSchema(String tableSchema) {
		this.tableSchema = tableSchema;
	}
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public String getColumnName() {
		return columnName;
	}
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	public String getOrdinalPosition() {
		return ordinalPosition;
	}
	public void setOrdinalPosition(String ordinalPosition) {
		this.ordinalPosition = ordinalPosition;
	}
	public String getColumnDefault() {
		return columnDefault;
	}
	public void setColumnDefault(String columnDefault) {
		this.columnDefault = columnDefault;
	}
	public String getIsNullable() {
		return isNullable;
	}
	public void setIsNullable(String isNullable) {
		this.isNullable = isNullable;
	}
	public String getDataType() {
		return dataType;
	}
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
	public String getCharacterMaximumLength() {
		return characterMaximumLength;
	}
	public void setCharacterMaximumLength(String characterMaximumLength) {
		this.characterMaximumLength = characterMaximumLength;
	}
	public String getCharacterOctetLength() {
		return characterOctetLength;
	}
	public void setCharacterOctetLength(String characterOctetLength) {
		this.characterOctetLength = characterOctetLength;
	}
	public String getNumericPrecision() {
		return numericPrecision;
	}
	public void setNumericPrecision(String numericPrecision) {
		this.numericPrecision = numericPrecision;
	}
	public String getNumericScale() {
		return numericScale;
	}
	public void setNumericScale(String numericScale) {
		this.numericScale = numericScale;
	}
	public String getCharacterSetName() {
		return characterSetName;
	}
	public void setCharacterSetName(String characterSetName) {
		this.characterSetName = characterSetName;
	}
	public String getCollationName() {
		return collationName;
	}
	public void setCollationName(String collationName) {
		this.collationName = collationName;
	}
	public String getColumnType() {
		return columnType;
	}
	public void setColumnType(String columnType) {
		this.columnType = columnType;
	}
	public String getColumnKey() {
		return columnKey;
	}
	public void setColumnKey(String columnKey) {
		this.columnKey = columnKey;
	}
	public String getExtra() {
		return extra;
	}
	public void setExtra(String extra) {
		this.extra = extra;
	}
	public String getPrivileges() {
		return privileges;
	}
	public void setPrivileges(String privileges) {
		this.privileges = privileges;
	}
	public String getColumnComment() {
		return columnComment;
	}
	public void setColumnComment(String columnComment) {
		this.columnComment = columnComment;
	}

	
}
