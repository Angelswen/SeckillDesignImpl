package com.vechace.test;

import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vechace.service.SeckillService;

import redis.clients.jedis.Jedis;

/**
 * 压力测试:
 * 1.采用多线程模拟多用户参与秒杀
 * 2.使用并发工具CountDownLatch，模拟并发场景
 * @author vechace 
 * date:2018/6/9
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class BenchmarkTest {
	
	@Autowired
	SeckillService seckillService;
	
	//参与秒杀的商品编号
	private static final String GOODS_CODE = "bike";
	
	//模拟的请求数量
	private static final int threadNum = 2000;
	
	//倒计数器，用于模拟高并发
	private CountDownLatch counter = new CountDownLatch(threadNum);
	
	long timed = 0L;
	
	@Before
	public void start(){
		System.out.println("开始测试");

		//初始化token令牌池
		Jedis jedis = new Jedis();
		jedis.del("token_list");
		for(int i = 0;i<100;i++){
			jedis.lpush("token_list", String.valueOf(i));
		}
		jedis.close();
		System.out.println("100个令牌池初始化完成");
		System.out.println("继续测试");
		
	}
	
	@After
	public void end(){
		System.out.println("测试结束，执行时长： " +(System.currentTimeMillis()-timed));
	}
	
	public void benchmark() throws InterruptedException{
		Thread[] threads = new Thread[threadNum];
		for(int i = 0 ;i<threadNum;i++){
			String userId = "user";
			
			Thread thread = new Thread(new UserRequest(userId));
			threads[i] = thread;
			thread.start();
			//计数器减一
			counter.countDown();
		}
		
		for(Thread thread:threads){
			thread.join();
		}
	}
	
	private class UserRequest implements Runnable{
		String userId;
		
		public UserRequest(String userId){
			this.userId = userId;
		}

		@Override
		public void run() {
			
			try{
				//等待其他线程就绪后，再运行后续代码
				counter.await();
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			
			//多线程发起http请求，模拟高并发
			seckillService.seckill(GOODS_CODE, userId);
			
		}
		
	}
	
}
