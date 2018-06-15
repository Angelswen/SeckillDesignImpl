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
 * ѹ������:
 * 1.���ö��߳�ģ����û�������ɱ
 * 2.ʹ�ò�������CountDownLatch��ģ�Ⲣ������
 * @author vechace 
 * date:2018/6/9
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class BenchmarkTest {
	
	@Autowired
	SeckillService seckillService;
	
	//������ɱ����Ʒ���
	private static final String GOODS_CODE = "bike";
	
	//ģ�����������
	private static final int threadNum = 2000;
	
	//��������������ģ��߲���
	private CountDownLatch counter = new CountDownLatch(threadNum);
	
	long timed = 0L;
	
	@Before
	public void start(){
		System.out.println("��ʼ����");

		//��ʼ��token���Ƴ�
		Jedis jedis = new Jedis();
		jedis.del("token_list");
		for(int i = 0;i<100;i++){
			jedis.lpush("token_list", String.valueOf(i));
		}
		jedis.close();
		System.out.println("100�����Ƴس�ʼ�����");
		System.out.println("��������");
		
	}
	
	@After
	public void end(){
		System.out.println("���Խ�����ִ��ʱ���� " +(System.currentTimeMillis()-timed));
	}
	
	public void benchmark() throws InterruptedException{
		Thread[] threads = new Thread[threadNum];
		for(int i = 0 ;i<threadNum;i++){
			String userId = "user";
			
			Thread thread = new Thread(new UserRequest(userId));
			threads[i] = thread;
			thread.start();
			//��������һ
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
				//�ȴ������߳̾����������к�������
				counter.await();
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			
			//���̷߳���http����ģ��߲���
			seckillService.seckill(GOODS_CODE, userId);
			
		}
		
	}
	
}
