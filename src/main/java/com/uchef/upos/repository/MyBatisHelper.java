package com.uchef.upos.repository;

import java.io.IOException;

import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

public class MyBatisHelper {

	public static SqlSession getSession() throws IOException {
		SqlSessionFactory factory = DaoSqlSessionFactory.getInstance();
		return factory.openSession(ExecutorType.SIMPLE);
	}

	public static void initMapper(Class<?> classes) {
		try {
			SqlSessionFactory factory = DaoSqlSessionFactory.getInstance();
			factory.getConfiguration().addMapper(classes);
		} catch (IOException e) {
			throw new RuntimeException(
					"Could not initialize iBATIS 3 Config.  Cause: " + e, e);
		} catch (IllegalArgumentException e) {
		} catch (BindingException e) {
		}
	}

	public static SqlSession getSession(ExecutorType type) throws IOException {
		SqlSessionFactory factory = DaoSqlSessionFactory.getInstance();
		return factory.openSession(type);
	}

	public static void initMappers(Class<?>[] mapperobjects) {
		try {
			SqlSessionFactory factory = DaoSqlSessionFactory.getInstance();
			for (Class<?> mapperobject : mapperobjects)
				factory.getConfiguration().addMapper(mapperobject);
		} catch (IOException e) {
			throw new RuntimeException(
					"Could not initialize iBATIS 3 Config.  Cause: " + e, e);
		} catch (IllegalArgumentException e) {
		} catch (BindingException e) {
		}
	}
}
 