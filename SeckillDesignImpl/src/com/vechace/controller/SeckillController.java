package com.vechace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.vechace.service.SeckillService;

/**
* description:控制器,处理秒杀请求
* @author vechace
* date 2018/6/9
*
*/
@Controller
public class SeckillController {
	
	@Autowired
	SeckillService seckillService;
	
	@RequestMapping("/home")
	public Object getUserInfo(String goodsCode,String userId)throws Exception{
		
		return seckillService.seckill(goodsCode, userId);
		
	}

}
