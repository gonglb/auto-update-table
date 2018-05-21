package com.github.gonglb.tools.autoupdatetable.entrance;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.gonglb.tools.autoupdatetable.common.ClassTool;
import com.github.gonglb.tools.autoupdatetable.common.ColumnType;
import com.github.gonglb.tools.autoupdatetable.mapper.CreateMysqlTablesMapper;
import com.github.gonglb.tools.autoupdatetable.model.CreateTableParam;
import com.github.gonglb.tools.autoupdatetable.model.SysMysqlColumns;


public class AutoTableHandle {
	private static final Logger log = LoggerFactory.getLogger(AutoTableHandle.class);

	private CreateMysqlTablesMapper createMysqlTablesMapper;

	/**
	 * 要扫描的model所在的packs，逗号分隔
	 */
	private String packs;

	/**
	 * update  更新或者创建
	 * create 全部重新创建
	 * none 不做操作
	 * 自动创建模式：update表示更新，create表示删除原表重新创建
	 */
	private String tableAuto;


	private boolean isUpdate = false;

	public AutoTableHandle(CreateMysqlTablesMapper createMysqlTablesMapper,String packs,String tableAuto){
		this.createMysqlTablesMapper = createMysqlTablesMapper;
		this.packs = packs;
		this.tableAuto = tableAuto;
	}

	/**
	 * 读取配置文件的三种状态（创建表、更新表、不做任何事情）
	 */
	public void createMysqlTable() {

		// 不做任何事情
		if ("none".equals(tableAuto)) {
			log.info("配置mybatis.table.auto=none，不需要做任何事情");
			return;
		}
		if(isUpdate ) {
			return ;
		}
		isUpdate =true;
		// 用于存需要创建的表名+结构
		Map<String, List<Object>> newTableMap = new HashMap<String, List<Object>>();

		// 用于存需要更新字段类型等的表名+结构
		Map<String, List<Object>> modifyTableMap = new HashMap<String, List<Object>>();

		// 用于存需要增加字段的表名+结构
		Map<String, List<Object>> addTableMap = new HashMap<String, List<Object>>();

		// 用于存需要删除字段的表名+结构
		Map<String, List<Object>> removeTableMap = new HashMap<String, List<Object>>();

		// 用于存需要删除主键的表名+结构
		Map<String, List<Object>> dropKeyTableMap = new HashMap<String, List<Object>>();

		// 用于存需要删除唯一约束的表名+结构
		Map<String, List<Object>> dropUniqueTableMap = new HashMap<String, List<Object>>();

		String [] packArr = packs.split(",");
		Set<Class<?>> classes ;
		for (String pack : packArr) {
			// 从包package中获取所有的Class
			classes = ClassTool.getClasses(pack, true, Table.class);

			// 构建出全部表的增删改的map
			allTableMapConstruct( classes, newTableMap, modifyTableMap, addTableMap, removeTableMap,
					dropKeyTableMap, dropUniqueTableMap);

			// 根据传入的map，分别去创建或修改表结构
			createOrModifyTableConstruct(newTableMap, modifyTableMap, addTableMap, removeTableMap, dropKeyTableMap,
					dropUniqueTableMap);

		}
	}

	/**
	 * 构建出全部表的增删改的map
	 * 
	 * @param mySqlTypeAndLengthMap
	 *            获取Mysql的类型，以及类型需要设置几个长度
	 * @param classes
	 *            从包package中获取所有的Class
	 * @param newTableMap
	 *            用于存需要创建的表名+结构
	 * @param modifyTableMap
	 *            用于存需要更新字段类型等的表名+结构
	 * @param addTableMap
	 *            用于存需要增加字段的表名+结构
	 * @param removeTableMap
	 *            用于存需要删除字段的表名+结构
	 * @param dropKeyTableMap
	 *            用于存需要删除主键的表名+结构
	 * @param dropUniqueTableMap
	 *            用于存需要删除唯一约束的表名+结构
	 */
	private void allTableMapConstruct(Set<Class<?>> classes,
			Map<String, List<Object>> newTableMap, Map<String, List<Object>> modifyTableMap,
			Map<String, List<Object>> addTableMap, Map<String, List<Object>> removeTableMap,
			Map<String, List<Object>> dropKeyTableMap, Map<String, List<Object>> dropUniqueTableMap) {
		for (Class<?> clas : classes) {

			Table table = clas.getAnnotation(Table.class);
			// 没有打注解不需要创建变
			if (null == table) {
				continue;
			}

			// 用于存新增表的字段
			List<Object> newFieldList = new ArrayList<Object>();
			// 用于存删除的字段
			List<Object> removeFieldList = new ArrayList<Object>();
			// 用于存新增的字段
			List<Object> addFieldList = new ArrayList<Object>();
			// 用于存修改的字段
			List<Object> modifyFieldList = new ArrayList<Object>();
			// 用于存删除主键的字段
			List<Object> dropKeyFieldList = new ArrayList<Object>();
			// 用于存删除唯一约束的字段
			List<Object> dropUniqueFieldList = new ArrayList<Object>();

			// 迭代出所有model的所有fields存到newFieldList中
			tableFieldsConstruct( clas, newFieldList);


			// 如果配置文件配置的是create，表示将所有的表删掉重新创建
			if ("create".equals(tableAuto)) {
				createMysqlTablesMapper.dorpTableByName(table.name());
			}

			// 先查该表是否以存在
			int exist = createMysqlTablesMapper.findTableCountByTableName(table.name());

			// 不存在时
			if (exist == 0) {
				newTableMap.put(table.name(), newFieldList);
			} else {
				// 已存在时理论上做修改的操作，这里查出该表的结构
				List<SysMysqlColumns> tableColumnList = createMysqlTablesMapper
						.findTableEnsembleByTableName(table.name());

				// 从sysColumns中取出我们需要比较的列的List
				// 先取出name用来筛选出增加和删除的字段
				List<String> columnNames = tableColumnList.stream().map(SysMysqlColumns::getColumnName).collect(Collectors.toList());

				// 验证对比从model中解析的fieldList与从数据库查出来的columnList
				// 1. 找出增加的字段
				// 2. 找出删除的字段
				// 3. 找出更新的字段
				buildAddAndRemoveAndModifyFields(modifyTableMap, addTableMap, removeTableMap,
						dropKeyTableMap, dropUniqueTableMap, table, newFieldList, removeFieldList, addFieldList,
						modifyFieldList, dropKeyFieldList, dropUniqueFieldList, tableColumnList, columnNames);

			}
		}
	}

	/**
	 * 构建增加的删除的修改的字段
	 * 
	 * @param mySqlTypeAndLengthMap
	 *            获取Mysql的类型，以及类型需要设置几个长度
	 * @param modifyTableMap
	 *            用于存需要更新字段类型等的表名+结构
	 * @param addTableMap
	 *            用于存需要增加字段的表名+结构
	 * @param removeTableMap
	 *            用于存需要删除字段的表名+结构
	 * @param dropKeyTableMap
	 *            用于存需要删除主键的表名+结构
	 * @param dropUniqueTableMap
	 *            用于存需要删除唯一约束的表名+结构
	 * @param table
	 *            表
	 * @param newFieldList
	 *            用于存新增表的字段
	 * @param removeFieldList
	 *            用于存删除的字段
	 * @param addFieldList
	 *            用于存新增的字段
	 * @param modifyFieldList
	 *            用于存修改的字段
	 * @param dropKeyFieldList
	 *            用于存删除主键的字段
	 * @param dropUniqueFieldList
	 *            用于存删除唯一约束的字段
	 * @param tableColumnList
	 *            已存在时理论上做修改的操作，这里查出该表的结构
	 * @param columnNames
	 *            从sysColumns中取出我们需要比较的列的List
	 */
	private void buildAddAndRemoveAndModifyFields(
			Map<String, List<Object>> modifyTableMap, Map<String, List<Object>> addTableMap,
			Map<String, List<Object>> removeTableMap, Map<String, List<Object>> dropKeyTableMap,
			Map<String, List<Object>> dropUniqueTableMap, Table table, List<Object> newFieldList,
			List<Object> removeFieldList, List<Object> addFieldList, List<Object> modifyFieldList,
			List<Object> dropKeyFieldList, List<Object> dropUniqueFieldList, List<SysMysqlColumns> tableColumnList,
			List<String> columnNames) {
		// 1. 找出增加的字段
		// 根据数据库中表的结构和model中表的结构对比找出新增的字段
		buildNewFields(addTableMap, table, newFieldList, addFieldList, columnNames);

		// 将fieldList转成Map类型，字段名作为主键
		Map<String, CreateTableParam> fieldMap = new HashMap<String, CreateTableParam>();
		for (Object obj : newFieldList) {
			CreateTableParam createTableParam = (CreateTableParam) obj;
			fieldMap.put(createTableParam.getFieldName(), createTableParam);
		}

		// 2. 找出删除的字段
		buildRemoveFields(removeTableMap, table, removeFieldList, columnNames, fieldMap);

		// 3. 找出更新的字段
		buildModifyFields(modifyTableMap, dropKeyTableMap, dropUniqueTableMap, table,
				modifyFieldList, dropKeyFieldList, dropUniqueFieldList, tableColumnList, fieldMap);
	}

	/**
	 * 根据数据库中表的结构和model中表的结构对比找出修改类型默认值等属性的字段
	 * 
	 * @param mySqlTypeAndLengthMap
	 *            获取Mysql的类型，以及类型需要设置几个长度
	 * @param modifyTableMap
	 *            用于存需要更新字段类型等的表名+结构
	 * @param dropKeyTableMap
	 *            用于存需要删除主键的表名+结构
	 * @param dropUniqueTableMap
	 *            用于存需要删除唯一约束的表名+结构
	 * @param table
	 *            表
	 * @param modifyFieldList
	 *            用于存修改的字段
	 * @param dropKeyFieldList
	 *            用于存删除主键的字段
	 * @param dropUniqueFieldList
	 *            用于存删除唯一约束的字段
	 * @param tableColumnList
	 *            已存在时理论上做修改的操作，这里查出该表的结构
	 * @param fieldMap
	 *            从sysColumns中取出我们需要比较的列的List
	 */
	private void buildModifyFields(Map<String, List<Object>> modifyTableMap,
			Map<String, List<Object>> dropKeyTableMap, Map<String, List<Object>> dropUniqueTableMap, Table table,
			List<Object> modifyFieldList, List<Object> dropKeyFieldList, List<Object> dropUniqueFieldList,
			List<SysMysqlColumns> tableColumnList, Map<String, CreateTableParam> fieldMap) {
		for (SysMysqlColumns sysColumn : tableColumnList) {
			// 数据库中有该字段时
			CreateTableParam createTableParam = fieldMap.get(sysColumn.getColumnName());
			if(createTableParam == null) {
				// 原本是主键，现在不是了，那么要去做删除主键的操作
				if ("PRI".equals(sysColumn.getColumnKey()) ) {
					CreateTableParam tableParam = new CreateTableParam();
					tableParam.setFieldName(sysColumn.getColumnName());
					tableParam.setFieldType("int");
					tableParam.setFieldLength(11);
					dropKeyFieldList.add(tableParam);
				}
			}else {
				// 检查是否要删除已有主键和是否要删除已有唯一约束的代码必须放在其他检查的最前面
				// 原本是主键，现在不是了，那么要去做删除主键的操作
				if ("PRI".equals(sysColumn.getColumnKey()) && !createTableParam.isFieldIsKey()) {
					dropKeyFieldList.add(createTableParam);
				}
				
				if("PRI".equals(sysColumn.getColumnKey()) && createTableParam.isFieldIsKey()) {
					createTableParam.setFieldIsKey(false);//解决重复主键错误问题
				}

				// 原本是唯一，现在不是了，那么要去做删除唯一的操作
				if ("UNI".equals(sysColumn.getColumnKey()) && !createTableParam.isFieldIsUnique()) {
					dropUniqueFieldList.add(createTableParam);
				}

				// 验证是否有更新
				// 1.验证类型
				if (!sysColumn.getDataType().toLowerCase().equals(createTableParam.getFieldType().toLowerCase())) {
					//int 和 integer 都是同一种类型不用修改
					if(sysColumn.getDataType().toLowerCase().equals("int") && createTableParam.getFieldType().toLowerCase().equals("integer")) {

					}else {
						modifyFieldList.add(createTableParam);
						continue;
					}
				}
				// 2.验证长度
				// 3.验证小数点位数
				String typeAndLength = createTableParam.getFieldType().toLowerCase();
				if(createTableParam.getFieldLength() != 0 && createTableParam.getFieldDecimalLength() !=0) {
					typeAndLength = typeAndLength + "(" + createTableParam.getFieldLength() + ","
							+ createTableParam.getFieldDecimalLength() + ")";
				}else if (createTableParam.getFieldLength() != 0) {
					// 拼接出类型加长度，比如varchar(1)
					typeAndLength = typeAndLength + "(" + createTableParam.getFieldLength() + ")";

				}
				// 判断类型+长度是否相同
				if (!sysColumn.getColumnType().toLowerCase().equals(typeAndLength)) {
					modifyFieldList.add(createTableParam);
					continue;
				}

				// 4.验证主键
				if (!"PRI".equals(sysColumn.getColumnKey()) && createTableParam.isFieldIsKey()) {
					// 原本不是主键，现在变成了主键，那么要去做更新
					modifyFieldList.add(createTableParam);
					continue;
				}

				// 5.验证自增
				if ("auto_increment".equals(sysColumn.getExtra()) && !createTableParam.isFieldIsAutoIncrement()) {
					modifyFieldList.add(createTableParam);
					continue;
				}
				
				// 6.验证默认值不同时
				if (sysColumn.getColumnDefault() != null && !sysColumn.getColumnDefault().equals(createTableParam.getFieldDefaultValue())
						||createTableParam.getFieldDefaultValue() !=null && !createTableParam.getFieldDefaultValue().equals(sysColumn.getColumnDefault())) {
					modifyFieldList.add(createTableParam);
					continue;
				}

				// 7.验证是否可以为null(主键不参与是否为null的更新)
				if (sysColumn.getIsNullable().equals("NO") && !createTableParam.isFieldIsKey()) {
					if (createTableParam.isFieldIsNull()) {
						// 一个是可以一个是不可用，所以需要更新该字段
						modifyFieldList.add(createTableParam);
						continue;
					}
				} else if (sysColumn.getIsNullable().equals("YES") && !createTableParam.isFieldIsKey()) {
					if (!createTableParam.isFieldIsNull()) {
						// 一个是可以一个是不可用，所以需要更新该字段
						modifyFieldList.add(createTableParam);
						continue;
					}
				}

				// 8.验证是否唯一
				if (!"UNI".equals(sysColumn.getColumnKey()) && createTableParam.isFieldIsUnique()) {
					// 原本不是唯一，现在变成了唯一，那么要去做更新
					modifyFieldList.add(createTableParam);
					continue;
				}

				//9.注释是否相同
				if (sysColumn.getColumnComment() != null && !sysColumn.getColumnComment().equals(createTableParam.getFieldComment())
						||createTableParam.getFieldComment() !=null && !createTableParam.getFieldComment().equals(sysColumn.getColumnComment())) {
					modifyFieldList.add(createTableParam);
					continue;
				}
			}
		}

		if (modifyFieldList.size() > 0) {
			modifyTableMap.put(table.name(), modifyFieldList);
		}

		if (dropKeyFieldList.size() > 0) {
			dropKeyTableMap.put(table.name(), dropKeyFieldList);
		}

		if (dropUniqueFieldList.size() > 0) {
			dropUniqueTableMap.put(table.name(), dropUniqueFieldList);
		}
	}

	/**
	 * 根据数据库中表的结构和model中表的结构对比找出删除的字段
	 * 
	 * @param removeTableMap
	 *            用于存需要删除字段的表名+结构
	 * @param table
	 *            表
	 * @param removeFieldList
	 *            用于存删除的字段
	 * @param columnNames
	 *            数据库中的结构
	 * @param fieldMap
	 *            model中的字段，字段名为key
	 */
	private void buildRemoveFields(Map<String, List<Object>> removeTableMap, Table table, List<Object> removeFieldList,
			List<String> columnNames, Map<String, CreateTableParam> fieldMap) {
		for (String fieldNm : columnNames) {
			// 判断该字段在新的model结构中是否存在
			if (fieldMap.get(fieldNm) == null) {
				// 不存在，做删除处理
				removeFieldList.add(fieldNm);
			}
		}
		if (removeFieldList.size() > 0) {
			removeTableMap.put(table.name(), removeFieldList);
		}
	}

	/**
	 * 根据数据库中表的结构和model中表的结构对比找出新增的字段
	 * 
	 * @param addTableMap
	 *            用于存需要增加字段的表名+结构
	 * @param table
	 *            表
	 * @param newFieldList
	 *            model中的结构
	 * @param addFieldList
	 *            用于存新增的字段
	 * @param columnNames
	 *            数据库中的结构
	 */
	private void buildNewFields(Map<String, List<Object>> addTableMap, Table table, List<Object> newFieldList,
			List<Object> addFieldList, List<String> columnNames) {
		for (Object obj : newFieldList) {
			CreateTableParam createTableParam = (CreateTableParam) obj;
			// 循环新的model中的字段，判断是否在数据库中已经存在
			if (!columnNames.contains(createTableParam.getFieldName())) {
				// 不存在，表示要在数据库中增加该字段
				addFieldList.add(obj);
			}
		}
		if (addFieldList.size() > 0) {
			addTableMap.put(table.name(), addFieldList);
		}
	}

	/**
	 * 迭代出所有model的所有fields存到newFieldList中
	 * 
	 * @param mySqlTypeAndLengthMap
	 *            mysql数据类型和对应几个长度的map
	 * @param clas
	 *            准备做为创建表依据的class
	 * @param newFieldList
	 *            用于存新增表的字段
	 */
	private void tableFieldsConstruct(Class<?> clas,
			List<Object> newFieldList) {
		Field[] fields = clas.getDeclaredFields();

		// 判断是否有父类，如果有拉取父类的field，这里只支持多层继承
		fields = recursionParents(clas, fields);
		String type;
		boolean hasAnnotation;;
		boolean hasId;
		Column column;
		ColumnType columnType;
		CreateTableParam param;
		int length = 0;
		JdbcType jdbcType;
		for (Field field : fields) {
			length = 0;
			// 判断方法中是否有指定注解类型的注解
			hasAnnotation = field.isAnnotationPresent(Column.class);
			hasId = field.isAnnotationPresent(Id.class);
			if (hasAnnotation) {
				// 根据注解类型返回方法的指定类型注解
				column = field.getAnnotation(Column.class);
				columnType = field.getAnnotation(ColumnType.class);
				if(columnType ==null ) {
					jdbcType = JdbcType.VARCHAR;
				}else {
					jdbcType = columnType.jdbcType();
				}
				param = new CreateTableParam();
				param.setFieldName(column.name().equals("")?camel2Underline(field.getName()):column.name());
				
				switch (jdbcType) {
				case LONGVARCHAR:
					type="text";
					break;
				case INTEGER:
					type="int";
					break;
				case DATETIMEOFFSET:
					type="datetime";
				break;
				default:
					type = jdbcType.name().toLowerCase();
					break;
				}
				if(jdbcType != JdbcType.VARCHAR && column.length() == 255) {
					length = 0;
				}else {
					length = column.length();
				}

				//由于注解没用定义默认值属性，暂时使用table代替
				param.setFieldDefaultValue(jdbcType != JdbcType.VARCHAR && StringUtils.isBlank(column.table())?null:column.table());
				param.setFieldType(type);
				param.setFieldLength(length);
				param.setFieldDecimalLength(column.scale());
				// 主键或唯一键时设置必须不为null
				if (hasId || column.unique()) {
					param.setFieldIsNull(false);
				} else {
					param.setFieldIsNull(column.nullable());
				}
				param.setFieldIsKey(hasId);
				param.setFieldIsAutoIncrement(hasId);
				param.setFieldIsUnique(column.unique());

				//由于注解没用定义注释属性，暂时使用columnDefinition代替
				param.setFieldComment(StringUtils.isBlank(column.columnDefinition())?null:column.columnDefinition());

				

				newFieldList.add(param);
			}
		}
	}

	/**
	 * 递归扫描父类的fields
	 * 
	 * @param clas
	 *            类
	 * @param fields
	 *            属性
	 */
	@SuppressWarnings("rawtypes")
	private Field[] recursionParents(Class<?> clas, Field[] fields) {
		if (clas.getSuperclass() != null) {
			Class clsSup = clas.getSuperclass();
			fields = (Field[]) ArrayUtils.addAll(fields, clsSup.getDeclaredFields());
			fields = recursionParents(clsSup, fields);
		}
		return fields;
	}

	/**
	 * 根据传入的map创建或修改表结构
	 * 
	 * @param newTableMap
	 *            用于存需要创建的表名+结构
	 * @param modifyTableMap
	 *            用于存需要更新字段类型等的表名+结构
	 * @param addTableMap
	 *            用于存需要增加字段的表名+结构
	 * @param removeTableMap
	 *            用于存需要删除字段的表名+结构
	 * @param dropKeyTableMap
	 *            用于存需要删除主键的表名+结构
	 * @param dropUniqueTableMap
	 *            用于存需要删除唯一约束的表名+结构
	 */
	private void createOrModifyTableConstruct(Map<String, List<Object>> newTableMap,
			Map<String, List<Object>> modifyTableMap, Map<String, List<Object>> addTableMap,
			Map<String, List<Object>> removeTableMap, Map<String, List<Object>> dropKeyTableMap,
			Map<String, List<Object>> dropUniqueTableMap) {
		// 1. 创建表
		createTableByMap(newTableMap);
		// 2. 删除要变更主键的表的原来的字段的主键
		dropFieldsKeyByMap(dropKeyTableMap);
		// 3. 删除要变更唯一约束的表的原来的字段的唯一约束
		dropFieldsUniqueByMap(dropUniqueTableMap);
		// 4. 添加新的字段
		addFieldsByMap(addTableMap);
		// 5. 删除字段
		removeFieldsByMap(removeTableMap);
		// 6. 修改字段类型等
		modifyFieldsByMap(modifyTableMap);

	}

	/**
	 * 根据map结构修改表中的字段类型等
	 * 
	 * @param modifyTableMap
	 *            用于存需要更新字段类型等的表名+结构
	 */
	private void modifyFieldsByMap(Map<String, List<Object>> modifyTableMap) {
		// 做修改字段操作
		if (modifyTableMap.size() > 0) {
			for (Entry<String, List<Object>> entry : modifyTableMap.entrySet()) {
				for (Object obj : entry.getValue()) {
					Map<String, Object> map = new HashMap<String, Object>();
					map.put(entry.getKey(), obj);
					CreateTableParam fieldProperties = (CreateTableParam) obj;
					log.info("开始修改表" + entry.getKey() + "中的字段" + fieldProperties.getFieldName());
					createMysqlTablesMapper.modifyTableField(map);
					log.info("完成修改表" + entry.getKey() + "中的字段" + fieldProperties.getFieldName());
				}
			}
		}
	}

	/**
	 * 根据map结构删除表中的字段
	 * 
	 * @param removeTableMap
	 *            用于存需要删除字段的表名+结构
	 */
	private void removeFieldsByMap(Map<String, List<Object>> removeTableMap) {
		// 做删除字段操作
		if (removeTableMap.size() > 0) {
			for (Entry<String, List<Object>> entry : removeTableMap.entrySet()) {
				for (Object obj : entry.getValue()) {
					Map<String, Object> map = new HashMap<String, Object>();
					map.put(entry.getKey(), obj);
					String fieldName = (String) obj;
					log.info("开始删除表" + entry.getKey() + "中的字段" + fieldName);
					createMysqlTablesMapper.removeTableField(map);
					log.info("完成删除表" + entry.getKey() + "中的字段" + fieldName);
				}
			}
		}
	}

	/**
	 * 根据map结构对表中添加新的字段
	 * 
	 * @param addTableMap
	 *            用于存需要增加字段的表名+结构
	 */
	private void addFieldsByMap(Map<String, List<Object>> addTableMap) {
		// 做增加字段操作
		if (addTableMap.size() > 0) {
			for (Entry<String, List<Object>> entry : addTableMap.entrySet()) {
				for (Object obj : entry.getValue()) {
					Map<String, Object> map = new HashMap<String, Object>();
					map.put(entry.getKey(), obj);
					CreateTableParam fieldProperties = (CreateTableParam) obj;
					log.info("开始为表" + entry.getKey() + "增加字段" + fieldProperties.getFieldName());
					createMysqlTablesMapper.addTableField(map);
					log.info("完成为表" + entry.getKey() + "增加字段" + fieldProperties.getFieldName());
				}
			}
		}
	}

	/**
	 * 根据map结构删除要变更表中字段的主键
	 * 
	 * @param dropKeyTableMap
	 *            用于存需要删除主键的表名+结构
	 */
	private void dropFieldsKeyByMap(Map<String, List<Object>> dropKeyTableMap) {
		// 先去做删除主键的操作，这步操作必须在增加和修改字段之前！
		if (dropKeyTableMap.size() > 0) {
			for (Entry<String, List<Object>> entry : dropKeyTableMap.entrySet()) {
				for (Object obj : entry.getValue()) {
					Map<String, Object> map = new HashMap<String, Object>();
					map.put(entry.getKey(), obj);
					CreateTableParam fieldProperties = (CreateTableParam) obj;
					log.info("开始为表" + entry.getKey() + "删除主键" + fieldProperties.getFieldName());
					createMysqlTablesMapper.dropKeyTableField(map);
					log.info("完成为表" + entry.getKey() + "删除主键" + fieldProperties.getFieldName());
				}
			}
		}
	}

	/**
	 * 根据map结构删除要变更表中字段的唯一约束
	 * 
	 * @param dropUniqueTableMap
	 *            用于存需要删除唯一约束的表名+结构
	 */
	private void dropFieldsUniqueByMap(Map<String, List<Object>> dropUniqueTableMap) {
		// 先去做删除唯一约束的操作，这步操作必须在增加和修改字段之前！
		if (dropUniqueTableMap.size() > 0) {
			for (Entry<String, List<Object>> entry : dropUniqueTableMap.entrySet()) {
				for (Object obj : entry.getValue()) {
					Map<String, Object> map = new HashMap<String, Object>();
					map.put(entry.getKey(), obj);
					CreateTableParam fieldProperties = (CreateTableParam) obj;
					log.info("开始为表" + entry.getKey() + "删除唯一约束" + fieldProperties.getFieldName());
					createMysqlTablesMapper.dropUniqueTableField(map);
					log.info("完成为表" + entry.getKey() + "删除唯一约束" + fieldProperties.getFieldName());
					log.info("开始修改表" + entry.getKey() + "中的字段" + fieldProperties.getFieldName());
					createMysqlTablesMapper.modifyTableField(map);
					log.info("完成修改表" + entry.getKey() + "中的字段" + fieldProperties.getFieldName());
				}
			}
		}
	}

	/**
	 * 根据map结构创建表
	 * 
	 * @param newTableMap
	 *            用于存需要创建的表名+结构
	 */
	private void createTableByMap(Map<String, List<Object>> newTableMap) {
		// 做创建表操作
		if (newTableMap.size() > 0) {
			for (Entry<String, List<Object>> entry : newTableMap.entrySet()) {
				Map<String, List<Object>> map = new HashMap<String, List<Object>>();
				map.put(entry.getKey(), entry.getValue());
				log.info("开始创建表：" + entry.getKey());
				createMysqlTablesMapper.createTable(map);
				log.info("完成创建表：" + entry.getKey());
			}
		}
	}


	/**
	 * 驼峰转下划线
	 * @param line 驼峰字符串
	 * @return 下划线字符串
	 */
	public static String camel2Underline(String line){
		if(line==null||"".equals(line)){
			return "";
		}
		line=String.valueOf(line.charAt(0)).toUpperCase().concat(line.substring(1));
		StringBuffer sb=new StringBuffer();
		Pattern pattern=Pattern.compile("[A-Z]([a-z\\d]+)?");
		Matcher matcher=pattern.matcher(line);
		while(matcher.find()){
			String word=matcher.group();
			sb.append(word.toLowerCase());
			sb.append(matcher.end()==line.length()?"":"_");
		}
		return sb.toString().toLowerCase();
	}

}
