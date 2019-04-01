package com.atguigu;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.LoggerFactory;

import ch.qos.logback.core.rolling.helper.IntegerTokenConverter;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.Transaction;



public class SecKill_redis {
	
	private static final  org.slf4j.Logger logger =LoggerFactory.getLogger(SecKill_redis.class) ;

	public static void main(String[] args) {
 
 
		Jedis jedis =new Jedis("192.168.229.131",6379);
		
		System.out.println(jedis.ping());
	 
		jedis.close();
		
  
		
		
			
	}
	
	
	public static boolean doSecKill(String uid,String prodid) throws IOException {
	    //获取参数拼key
		String  qtKey ="sk:"+ prodid+":qt";
		String  usrKey ="sk:"+ prodid+":usr";
		
		JedisPool jp = JedisPoolUtil.getJedisPoolInstance();
		System.out.println("NumActive="+jp.getNumActive()
		+"NumWaiters="+jp.getNumWaiters());
		Jedis jedis =jp.getResource();
		
		//判断是否已经秒到
		if(jedis.sismember(usrKey, uid)) {
			System.err.println("已秒到，禁止重复秒杀！！");
			jedis.close();
			return false; 
		}
		
		//加乐观锁
		jedis.watch(qtKey);
		
		//判断库存
		String qtStr = jedis.get(qtKey);
		if(qtStr==null) {
			System.err.println("未初始化库存！！");
			jedis.close();
			return false; 	
		}else {
			int qt = Integer.parseInt(qtStr);
			if(qt<=0) {
				System.err.println("已秒光！！");
				jedis.close();
				return false; 	
			}
		}
		Transaction multi = jedis.multi();
		
		//减库存
		multi.decr(qtKey);
		//加人
		multi.sadd(usrKey, uid);
		
		List<Object> exec = multi.exec();
		if(exec==null||exec.size()==0) {
			System.err.println("秒杀失败！！");
			jedis.close();
			return false; 
		}
		
		System.out.println("秒杀成功！！！");
		jedis.close();
	     return true ; 
	}
	

}
