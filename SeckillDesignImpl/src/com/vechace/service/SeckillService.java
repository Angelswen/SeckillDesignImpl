package com.vechace.service;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vechace.dao.SeckillDao;
import com.vechace.utils.RedisCacheUtils;

import redis.clients.jedis.Jedis;

/**
* description:业务层,处理秒杀业务逻辑处理
* @author vechace
* date 2018/6/9
*
*/
@Service
public class SeckillService {
	
	private final Logger logger = Logger.getLogger(SeckillService.class);
	
	@Autowired
	RedisCacheUtils redisPool;
	
	@Autowired
	SeckillDao seckillDao;
	
	/**
	 * 秒杀业务层接口
	 * @param goodsCode
	 * @param userId
	 * @return
	 */
	public boolean seckill(String goodsCode,String userId){
		
		/**
		 * 限制用户的操作频率，10s操作一次
		 * 使用组合指令：SET key value EX 5 NX
		 * 细节：不能拆分成两条指令: SET key EX ;SET key NX,会导致性能下降一半
		 */
		Jedis jedis = redisPool.getJedis();
		String value = jedis.set(userId, "","NX","EX",10);
		if(!"OK".equals(value)){
			logger.warn("被限制操作频率，用户： " +userId);
			return false;
		}
		
		/**
		 * token令牌机制：采用Redis的列表类型实现令牌队列，
		 * lpop指令从队列左边弹出一个令牌，先到先得，拿到令牌的可以请求数据库
		 */
		String token = jedis.lpop("token_list");
		if(token == null){
			logger.warn("没有抢到token令牌，不参与秒杀，用户: " + userId);
			return false;
		}
		
		boolean result = seckillDao.buy(goodsCode, userId);
		logger.warn("秒杀结果: " +result);
		return result;
	}

}
