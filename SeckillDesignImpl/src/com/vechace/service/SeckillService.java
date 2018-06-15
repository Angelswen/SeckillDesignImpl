package com.vechace.service;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vechace.dao.SeckillDao;
import com.vechace.utils.RedisCacheUtils;

import redis.clients.jedis.Jedis;

/**
* description:ҵ���,������ɱҵ���߼�����
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
	 * ��ɱҵ���ӿ�
	 * @param goodsCode
	 * @param userId
	 * @return
	 */
	public boolean seckill(String goodsCode,String userId){
		
		/**
		 * �����û��Ĳ���Ƶ�ʣ�10s����һ��
		 * ʹ�����ָ�SET key value EX 5 NX
		 * ϸ�ڣ����ܲ�ֳ�����ָ��: SET key EX ;SET key NX,�ᵼ�������½�һ��
		 */
		Jedis jedis = redisPool.getJedis();
		String value = jedis.set(userId, "","NX","EX",10);
		if(!"OK".equals(value)){
			logger.warn("�����Ʋ���Ƶ�ʣ��û��� " +userId);
			return false;
		}
		
		/**
		 * token���ƻ��ƣ�����Redis���б�����ʵ�����ƶ��У�
		 * lpopָ��Ӷ�����ߵ���һ�����ƣ��ȵ��ȵã��õ����ƵĿ����������ݿ�
		 */
		String token = jedis.lpop("token_list");
		if(token == null){
			logger.warn("û������token���ƣ���������ɱ���û�: " + userId);
			return false;
		}
		
		boolean result = seckillDao.buy(goodsCode, userId);
		logger.warn("��ɱ���: " +result);
		return result;
	}

}
