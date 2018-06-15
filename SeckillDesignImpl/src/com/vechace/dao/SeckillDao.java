package com.vechace.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
* description:���ݲ�,�������ݿⲢ���¼�¼
* @author vechace
* date 2018/6/9
*
*/

@Repository
public class SeckillDao {
	
	//ʹ��Spring jdbcģ��
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	//����ع����׳��쳣
	@Transactional(rollbackFor = Exception.class)
	public boolean buy(String goodsCode,String userId){
		
		//��Ʒ������1
		String sql = "update tb_seckill set goods_nums = goods_nums-1 where goods_code = '"+ goodsCode +"' and goods_nums-1 >=0 ";
		int count = jdbcTemplate.update(sql);
		if(count !=1){
			return false;
		}
		
		//��ɱ�ɹ�����ӹ����¼
		String insertSql = "insert into tb_records(goods_code,user_id) value('" + goodsCode + "','" + userId + "')";
		int insertCount = jdbcTemplate.update(insertSql);
		if(insertCount !=1){
			return false;
		}
		
		return true;
	}

}
