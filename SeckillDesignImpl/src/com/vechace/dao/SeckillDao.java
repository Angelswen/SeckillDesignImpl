package com.vechace.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
* description:数据层,请求数据库并更新记录
* @author vechace
* date 2018/6/9
*
*/

@Repository
public class SeckillDao {
	
	//使用Spring jdbc模板
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	//事务回滚则抛出异常
	@Transactional(rollbackFor = Exception.class)
	public boolean buy(String goodsCode,String userId){
		
		//商品数量减1
		String sql = "update tb_seckill set goods_nums = goods_nums-1 where goods_code = '"+ goodsCode +"' and goods_nums-1 >=0 ";
		int count = jdbcTemplate.update(sql);
		if(count !=1){
			return false;
		}
		
		//秒杀成功则，添加购买记录
		String insertSql = "insert into tb_records(goods_code,user_id) value('" + goodsCode + "','" + userId + "')";
		int insertCount = jdbcTemplate.update(insertSql);
		if(insertCount !=1){
			return false;
		}
		
		return true;
	}

}
