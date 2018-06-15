package com.vechace.utils;
import org.springframework.stereotype.Component;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
* Redis连接池工具
* @author vechace
* date 2018/6/9
*/
@Component
public class RedisCacheUtils {
	
	private JedisPool pool;
	
	/**
	 * 建立连接池
	 */
	public RedisCacheUtils(String host,int port){
		JedisPoolConfig config = new JedisPoolConfig();
		//设置最大连接数
		config.setMaxTotal(10000);
		//设置最大阻塞时间，单位是毫秒数
		config.setMaxWaitMillis(10000);
		//设置空间连接
		config.setMaxIdle(100);
		//创建连接池
		pool = new JedisPool(config,host,port);
		
	}
	
	/**
	 * 获取一个jedis对象
	 * @return
	 */
	public Jedis getJedis(){
		return pool.getResource();
	}

}
