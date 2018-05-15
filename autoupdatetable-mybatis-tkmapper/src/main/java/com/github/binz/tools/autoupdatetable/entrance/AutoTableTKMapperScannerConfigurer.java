package com.github.binz.tools.autoupdatetable.entrance;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.github.binz.tools.autoupdatetable.mapper.CreateMysqlTablesMapper;

/**
 * 基于tkmapper管理
 * @author gonglb
 * 2018年5月10日 上午9:44:44
 */
public class AutoTableTKMapperScannerConfigurer extends tk.mybatis.spring.mapper.MapperScannerConfigurer implements ApplicationContextAware,ApplicationListener<ContextRefreshedEvent> {

	private AutoTableHandle autoTableHandle;

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

	private ApplicationContext appCtx;

	
	/**
	 * 容器加载完成执行
	 */
	@Override
	public void onApplicationEvent(ContextRefreshedEvent arg0) {
		if(autoTableHandle == null) {
			CreateMysqlTablesMapper createMysqlTablesMapper = this.appCtx.getBean(CreateMysqlTablesMapper.class);
			autoTableHandle = new AutoTableHandle(createMysqlTablesMapper, packs, tableAuto);
			autoTableHandle.createMysqlTable();
		}
	}
	

	/**
	 * 此方法可以把ApplicationContext对象inject到当前类中作为一个静态成员变量。
	 *
	 * @param applicationContext ApplicationContext 对象.
	 * @throws BeansException
	 * @author wangdf
	 */
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.appCtx = applicationContext;
	}


	public void setPacks(String packs) {
		this.packs = packs;
	}


	public void setTableAuto(String tableAuto) {
		this.tableAuto = tableAuto;
	}
	
	@Override
	public void setBasePackage(String basePackage) {
		basePackage+=",com.binz.tools.com.binz.tools.autoupdatetable.mapper";
		super.setBasePackage(basePackage);
	}

}
