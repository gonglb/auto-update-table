package com.github.binz.tools.autoupdatetable.model;

public class CreateTableParam {
	/**
	 * 字段名
	 */
	private String	fieldName;

	/**
	 * 字段类型
	 */
	private String	fieldType;

	/**
	 * 类型长度
	 */
	private int		fieldLength;

	/**
	 * 类型小数长度
	 */
	private int		fieldDecimalLength;

	/**
	 * 字段是否非空
	 */
	private boolean	fieldIsNull;

	/**
	 * 字段是否是主键
	 */
	private boolean	fieldIsKey;

	/**
	 * 主键是否自增
	 */
	private boolean	fieldIsAutoIncrement;

	/**
	 * 字段默认值
	 */
	private String	fieldDefaultValue;

	/**
	 * 值是否唯一
	 */
	private boolean	fieldIsUnique;
	
	/**
	 * 注释
	 */
	private String fieldComment;
	
	/**
	 * 数据表的sql
	 */
	private String tableDdl;
	

	public String getFieldName(){
		return fieldName;
	}

	public void setFieldName(String fieldName){
		this.fieldName = fieldName;
	}

	public String getFieldType(){
		return fieldType;
	}

	public void setFieldType(String fieldType){
		this.fieldType = fieldType;
	}

	public int getFieldLength(){
		return fieldLength;
	}

	public void setFieldLength(int fieldLength){
		this.fieldLength = fieldLength;
	}

	public int getFieldDecimalLength(){
		return fieldDecimalLength;
	}

	public void setFieldDecimalLength(int fieldDecimalLength){
		this.fieldDecimalLength = fieldDecimalLength;
	}

	public boolean isFieldIsNull(){
		if(this.fieldIsKey) {
			return false;
		}
		return fieldIsNull;
	}

	public void setFieldIsNull(boolean fieldIsNull){
		this.fieldIsNull = fieldIsNull;
	}

	public boolean isFieldIsKey(){
		return fieldIsKey;
	}

	public void setFieldIsKey(boolean fieldIsKey){
		this.fieldIsKey = fieldIsKey;
	}

	public boolean isFieldIsAutoIncrement(){
		return fieldIsAutoIncrement;
	}

	public void setFieldIsAutoIncrement(boolean fieldIsAutoIncrement){
		this.fieldIsAutoIncrement = fieldIsAutoIncrement;
	}

	public String getFieldDefaultValue(){
		return fieldDefaultValue;
	}

	public void setFieldDefaultValue(String fieldDefaultValue){
		this.fieldDefaultValue = fieldDefaultValue;
	}

	public boolean isFieldIsUnique(){
		return fieldIsUnique;
	}

	public void setFieldIsUnique(boolean fieldIsUnique){
		this.fieldIsUnique = fieldIsUnique;
	}

	public String getFieldComment() {
		return fieldComment;
	}

	public void setFieldComment(String fieldComment) {
		this.fieldComment = fieldComment;
	}

	public String getTableDdl() {
		return tableDdl;
	}

	public void setTableDdl(String tableDdl) {
		this.tableDdl = tableDdl;
	}

}
