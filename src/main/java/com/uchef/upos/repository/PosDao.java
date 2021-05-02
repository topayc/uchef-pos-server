package com.uchef.upos.repository;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;

public class PosDao {
	public Map<String, Object>  getPosInfo(String macAddress){
		SqlSession sqlSession= null;	
		
		try{
			sqlSession = MyBatisHelper.getSession();
			Map<String, String> paramMap = new HashMap<String, String>();					
			paramMap.put("macAddress", macAddress);
			return  (Map<String, Object>)sqlSession.selectOne("com.uchef.pos.repository.PosMapper.getPosInfo", paramMap);	
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}finally{
			if (sqlSession != null){
				sqlSession.close();
			}
		}
	}
}
