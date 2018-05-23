# auto-update-table 自动更新表结构，兼容通用tkmapper

### 1、maven引用
```xml
<dependency>
	<groupId>com.github.gonglb.tools</groupId>
	<artifactId>autoupdatetable-mybatis-tkmapper</artifactId>
	<version>0.0.2</version>
</dependency>
```
### 2、mybatis开启驼峰转换
```xml
<settings>
        <!--开启驼峰下划线自动转换 -->
    <setting name="mapUnderscoreToCamelCase" value="true" />
</settings>
```

### 3、SqlSessionFactory配置
##### xml方式：
```xml
<!-- 配置SqlSessionFactory对象 -->
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
	<!-- 注入数据库连接池 -->
	<property name="dataSource" ref="dataSource" />
	<!-- 扫描model包，使用别名，需要增加com.github.gonglb.tools.autoupdatetable.model-->
	<property name="typeAliasesPackage" value="com.xxxx.xxxx.*.model,com.github.gonglb.tools.autoupdatetable.model" />
	<!-- 扫描sql配置文件：mapper接口需要的xml文件 -->
	<property name="mapperLocations">
		<list>
 			<value>classpath*:mapper/*.xml</value><!--需要扫描包内mapper-->
			<value>classpath*:mapper/*/*.xml</value>
		</list>
		</property>
	<!-- mybatis配置 -->
	<property name="configLocation" value="classpath:mybatis-settings.xml" />
</bean>
 ```
##### 注解方式：
```java
@Bean("sqlSessionFactoryBean")
public SqlSessionFactory sqlSessionFactoryBean(DataSource dataSource) throws Exception {
 
    SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
    sqlSessionFactoryBean.setDataSource(dataSource);
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    sqlSessionFactoryBean.setConfigLocation(resolver.getResource("classpath:mybatis-setting.xml"));//扫描保内的mapper
    Resource[] resources = resolver.getResources("classpath*:mapper/*Mapper.xml");
    sqlSessionFactoryBean.setMapperLocations(resources);
    //增加扫描工具的model,com.github.gonglb.tools.autoupdatetable.model
    sqlSessionFactoryBean.setTypeHandlersPackage("com.xxx.xxx.*.model,com.github.gonglb.tools.autoupdatetable.model");
    return sqlSessionFactoryBean.getObject();
}
```

### 4、MapperScannerConfigurer配置

##### xml方式：
```xml
<!-- mapper接口扫描 -->
<bean class="com.github.gonglb.tools.autoupdatetable.entrance.AutoTableTKMapperScannerConfigurer">
　　<property name="basePackage" value="com.xxxx.xxxx.*.mapper" />
    <property name="properties">
        <value>
            mappers=tk.mybatis.mapper.common.Mapper,tk.mybatis.mapper.common.special.InsertListMapper
        </value>
    </property>
    <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory"></property>
    　　　　<!--关键配置-->
    <!--需要更新表结构的包路径 -->
    <property name="packs" value="com.xxx.xxx.test.model,com.xxx.xxx.test.model"/>
        <!-- 可选参数  create(所有表删除重新创建)、update(更新表)、none(不做操作) -->
    <property name="tableAuto" value="update"/>
</bean>

 ```
##### 注解方式：
```java
@Bean
public MapperScannerConfigurer getMapperScannerConfigurer() throws Exception {
    AutoTableTKMapperScannerConfigurer autoTableTKMapperScannerConfigurer = new AutoTableTKMapperScannerConfigurer();
    autoTableTKMapperScannerConfigurer.setBasePackage("com.xxx.xxx.*.mapper");
    autoTableTKMapperScannerConfigurer.setSqlSessionFactoryBeanName("sqlSessionFactoryBean");
    Properties properties = new Properties();
    properties.setProperty("mappers", "tk.mybatis.mapper.common.Mapper,tk.mybatis.mapper.common.special.InsertListMapper");
    autoTableTKMapperScannerConfigurer.setProperties(properties);
        //关键配置
    autoTableTKMapperScannerConfigurer.setPacks("com.xxx.xxx.test.model,com.xxx.xxx.test2.model");
    autoTableTKMapperScannerConfigurer.setTableAuto("update");
    return autoTableTKMapperScannerConfigurer;
}

```

### 5、model配置方式
```java
import org.apache.ibatis.type.JdbcType;
import javax.persistence.Transient;
import com.github.gonglb.tools.autoupdatetable.common.ColumnType;
@Table(name = "t_test")
public class Test{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "test_id",nullable = false,unique = false,length = 11,columnDefinition = "主键")
    @ColumnType(jdbcType = JdbcType.INTEGER)
    private Integer id;

　　//不指定name默认自动转换为驼峰下划线模式
    @Column(name = "test_name",nullable = true,unique = false,length = 64,columnDefinition = "注释",table="默认值")
    @ColumnType(jdbcType = JdbcType.VARCHAR)
    private String name;
    
    //不指定注解，默认会自动转换为下划线模式
    private String testTitle;

	//跟数据库无关的字段
	@Transient	
	private Boolean testOther;
}

```

附带自动生成java model 和sql 
<a href="http://47.98.124.10:8088/love-web//html/java/java-model-generate.html" target="_blank">http://47.98.124.10:8088/love-web//html/java/java-model-generate.html</a>
4、注意事项
a、该项目根据 mybatis-enhance-actable-0.0.1 开源项目的思路改编而来
b、该项目会自动删除更新表结构，根据实际情况使用