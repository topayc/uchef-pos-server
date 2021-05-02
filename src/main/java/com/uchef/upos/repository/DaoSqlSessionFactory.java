package com.uchef.upos.repository;

import java.io.IOException;
import java.io.Reader;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

public class DaoSqlSessionFactory {
	
	private static SqlSessionFactory factory = null;
	public static final String RESOURCE = "mapperConfiguration.xml";

	public static SqlSessionFactory getInstance() throws IOException {
		if (DaoSqlSessionFactory.factory == null) {
			Reader reader = Resources.getResourceAsReader(RESOURCE);
			SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();
			DaoSqlSessionFactory.factory = sqlSessionFactoryBuilder.build(reader);
		}
		return DaoSqlSessionFactory.factory;
	}
}
