# SeckillDesignImpl
高并发秒杀系统设计与实现

**开发环境**

eclipse + tomcat8 + jdk8

**使用技术**

Spring + Spring MVC + Redis + MySQL

技术选型：为什么选择Redis？
- 数据类型丰富（string,hash,list,set,sorted list），可根据业务选择不同的数据类型；
- 单线程，不存在锁竞争现象，实现相对容易，代码简洁，可读性强；
- 可持久化，redis提供了两种持久化方式RDB,AOF,可防止数据丢失
- 高可用（从后期业务扩展角度考虑），redis支持集群功能，可以实现主从复制，读写分离，提供哨兵机制等等

详细可参考这篇文章：[技术选型：redis与memcache](https://www.jianshu.com/p/774171cd2d5a)

**内容概述**

对高并发业务场景进行分析，以解决秒杀系统核心问题为目的，设计一个秒杀系统，分析秒杀业务场景，提供一种可行的解决方案，达到限流、分流效果，进而提升系统性能：

- 问题1：系统超卖，导致亏本，解决策略为：使用数据库行级锁，实现同步执行效果。
- 问题2：用户重复点击，请求量大于用户量，解决策略为：通过前端控制（按钮禁用，button.disabled），防止重复点击。
- 问题3：单机性能瓶颈：单机服务器处理能力有限，解决策略为：使用Nginx负载均衡，通过upstream模块实现多个Tomcat的负载均衡。
- 问题4：非普通用户（程序员）使用脚本工具进行抢购，解决策略为：使用Redis中的key的有效期（EX）和NX机制，限制单个用户的操作频率。
- 问题5：数据库连接请求量巨大，存在性能瓶颈，解决策略为：使用Redis的队列实现令牌池，令牌数量有限，先到先得，成功获取令牌的允许请求数据库连接。

扩展（未实现）：

- 单台Redis性能问题：用户量多时，并发量会更高，超出单台Redis吞吐量，解决方法是使用Redis集群，实现高可用
- 数据库性能瓶颈：当商品库存很多，采取上述多策略后，数据库请求依然多，压力依然大，解决方法是数据库分库分表、通过消息队列（如：Kafka、RabbitMQ等）异步执行SQL。
- 系统架构问题：秒杀系统与业务系统混叠，互相影响导致不可用，秒杀场景流量突发，需要支持快速扩容，解决方法是使用微服务架构（Spring cloud、dubbo），独立秒杀系统；docker容器，实现快速扩容。

**业务场景**

1、用户界面：用户点击秒杀按钮

2、Web程序服务器：服务器收到http请求，修改数据

3、数据库：修改商品余量，添加秒杀成功记录

**系统设计**


**数据库表设计**

设计两个简单的表：秒杀记录表和秒杀商品表，sql实现如下：

    create table 'tb_records' (
    	'records_id' int(11) not null auto_increment,
    	'goods_code' varchar(11) default null comment '商品唯一编码',
    	'user_id' varchar(255) default null comment '用户唯一ID',
    	primary key ('records_id')
    )engine = InnoDB auto_increment = 1031 default charset = latinl comment = '秒杀记录表';
    create table 'tb_seckil(
    	'goods_code' varchar(11) not null comment '商品唯一编码',
    	'goods_nums' int(255) default null comment '商品余量',
    	primary key ('goods_code')
    )engine = InnoDB default charset = latinl comment = '秒杀商品表';

核心类设计

业务处理接口：

    /**
    * description:业务层,处理秒杀业务逻辑处理
    * @author vechace
    * date 2018/6/15
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

数据更新接口：

    /**
    * description:数据层,请求数据库并更新记录
    * @author vechace
    * date 2018/6/15
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
    		String insertSql = "insert into tb_records(goods_code,user_id) value('" + goodsCode +"','" + userId + "')";
    		int insertCount = jdbcTemplate.update(insertSql);
    		if(insertCount !=1){
    			return false;
    		}
    		return true;
    	}
    }

**系统整体架构**

![架构](https://raw.githubusercontent.com/Angelswen/SeckillDesignImpl/master/SeckillDesignImpl/imge/%E7%A7%92%E6%9D%80%E7%B3%BB%E7%BB%9F%E6%9E%B6%E6%9E%84.png)

**系统分析**


**问题1（核心问题）：超卖问题，导致亏本**

解决策略：借助数据库锁机制，防止超卖：多个请求并发操作同一行记录时，数据库会锁住该行记录（行级锁），实现同步执行的效果。需要注意的是：MySQL存在各种锁（行级锁、表级锁、页级锁等），各个存储引擎的支持不同，如下：
- MyISAM仅支持表级锁
- BDB支持表级锁和页级锁
- InnoDB支持行级锁和表级锁

其他可行策略：

- MySQL添加字段version（版本号），实现MVCC乐观锁
- 基于AtomicInteger的CAS机制，即使用JVM锁机制
- 使用Redis作为原子计数器（watch事务，decr操作），kafka作为消息队列记录用户抢购行为

**问题2：用户重复点击，请求量大于用户量**

解决策略：针对普通用户，可以通过前端控制（按钮置灰，butter.disable），防止重复点击，可减少90%的重复请求。

    //控制访问频率，disable按钮置灰，禁用
    $("#test").attr('disabled',true);
    //5秒后自动启用
    setTimeout(function(){
    	$("#test").removeAttr("disabled");
    },5000);

**问题3：单机性能瓶颈：单机服务器能处理的并发量有限**

解决策略：负载均衡（软件方式），使用Nginx是最常用的高性能web服务器，通过upstream模块实现多个Tomcat的负载均衡。

其他实现方式：LVS（第四层负载均衡）、F5、Radware（硬件负载均衡）

![架构](https://raw.githubusercontent.com/Angelswen/SeckillDesignImpl/master/SeckillDesignImpl/imge/Nginx%E8%B4%9F%E8%BD%BD%E5%9D%87%E8%A1%A1.png)

Nginx配置

    events{
    	#并发连接数
        worker_connections 1024;
    }
    http{
        #Tomcat服务器集群，这里假设有两个tomcat服务器实现分流
        upstream tomcat_servers{
            server 127.0.0.1:8081;
            server 127.0.0.1:8080;
        }
        server{
            #监听80端口
            listen 80;
            #将所有请求交给Tomcat集群去处理
            location / {
                proxy_pass http://tomcat_servers;
            }
        }
    }

集群存在问题：

- 分布式session：使用Redis缓存用户session，实现分布式session一致性
- 分布式事务：分布式锁

**问题4：非普通用户（程序员）使用脚本工具进行抢购**

解决策略：针对非普通用户，限制单个用户的操作频率，通过Redis中的key的有效期（EX）和NX机制实现，组合命令：

    set key value EX 5 NX

原理：如果Redis中没有这个key，则设置成功，过期时间10秒，如果Redis已经有这个key，代表10秒内已经操作过1次

java实现：

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

细节分析：推荐使用组合指令，而不使用两条指令，因为根据Redis压力测试报告可知，当使用2条set指令时，系统性能可能会下降一半（高并发场景下）。

    $ redis-benchmark -t set,lpush -n 100000 -q
    SET: 74239.05 requests per second
    LPUSH: 79239.30 requests per second

redis压力测试报告：[how fast is redis?](https://redis.io/topics/benchmarks)

**问题5：数据库请求量巨大**

解决策略：数据库操作成功的量大于或等于商品数量：用户请求量大于等于用户数，而数据库操作成功次数小于等于秒杀商品数量。商品数量有限，能秒杀成功的请求极少。可采用令牌机制解决问题

令牌机制：通过预先（异步）初始化一个和商品数量相当的令牌池放在内存中，用户请求到来时，去内存中取令牌，令牌数量有限，先到先得，拿到令牌的可以请求数据库，从而减少数据库的压力。先到先得，拿到令牌的去秒杀，没拿到的回绝。

使用Redis实现令牌池

    @Before
    public void start(){
    	System.out.println("开始测试");
    	//初始化token令牌池，使用lpush指令，从token_list左边push，队列值为0-100
    	Jedis jedis = new Jedis();
    	jedis.del("token_list");
    	for(int i = 0;i<100;i++){
    		jedis.lpush("token_list", String.valueOf(i));
    	}
    	jedis.close();
    	System.out.println("100个令牌池初始化完成");
    	System.out.println("继续测试");
    }

秒杀方法：

    /**
     * token令牌机制：采用Redis的列表类型实现令牌队列，
     * lpop指令从队列左边弹出一个令牌，先到先得，拿到令牌的可以请求数据库
     */
    String token = jedis.lpop("token_list");
    if(token == null){
    	logger.warn("没有抢到token令牌，不参与秒杀，用户: " + userId);
    	return false;
    }

**高并发系统**
- **限流**：前端限制（按钮置灰）、服务端限流（限制用户请求次数）、令牌池
- **分流**：负载均衡、分表、分库、消息队列

**方案总结**

策略思路：分析业务场景，从业务角度去解决存在问题或提升系统性能，可以适当跳出常规的思维（从技术角度去想），实际过程中，通过分析秒杀系统可能存在的场景，得出方案，从前端控制，到服务端限流分流，再到数据库限流，逐步设计出具体方案，总结如下：
- 策略1：数据库乐观锁机制，防止超卖（核心问题）
- 策略2：前端防止重复提交
- 策略3：服务端做负载均衡
- 策略4：后台限制用户操作频率
- 策略5：令牌发放机制

拓展策略：当业务量持续增大时，可采用如下方案：
- 单台Redis不够用：用户量多，并发量更高，超出单台Redis吞吐量，解决方法是使用Redis集群、高可用。
- 数据库压力大：当商品库存很多，采取上述多策略后，数据库请求依然多，压力依然大，解决方法是数据库分库分表、通过消息队列异步执行SQL。
- 系统架构：与业务系统混合在一起，互相影响，导致不可用，而秒杀场景的突发流量，要支持快速扩容，解决方法是微服务架构，独立秒杀系统；docker容器化技术，快速扩容。

----------------------------
备注：学习源自网络，并加以总结。

参考资料：《深入理解Nginx》、《Redis 开发与运维》、Redis官方文档、Redis压测报告、Java8开发文档（多线程）等等

