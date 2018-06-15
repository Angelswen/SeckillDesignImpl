package com.vechace.utils;
import org.springframework.stereotype.Component;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
* Redis���ӳع���
* @author vechace
* date 2018/6/9
*/
@Component
public class RedisCacheUtils {
	
	private JedisPool pool;
	
	/**
	 * �������ӳ�
	 */
	public RedisCacheUtils(String host,int port){
		JedisPoolConfig config = new JedisPoolConfig();
		//�������������
		config.setMaxTotal(10000);
		//�����������ʱ�䣬��λ�Ǻ�����
		config.setMaxWaitMillis(10000);
		//���ÿռ�����
		config.setMaxIdle(100);
		//�������ӳ�
		pool = new JedisPool(config,host,port);
		
	}
	
	/**
	 * ��ȡһ��jedis����
	 * @return
	 */
	public Jedis getJedis(){
		return pool.getResource();
	}

}
