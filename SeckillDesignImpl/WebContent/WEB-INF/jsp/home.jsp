<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<title>Java 秒杀系统</title>
<script type="text/javascript" src="resource/jquery-3.2.1.js"></script>
<script type="text/javascript">
	$(function(){
		//获取token令牌
		$("#test").click(function(){
			//控制访问频率，disable按钮置灰，禁用
			$("#test").attr('disabled',true);
			//5秒后自动启用
			setTimeout(function(){
				$("#test").removeAttr("disabled");
			},5000);
			$.ajax({
				type : "get",
				url : "home?userId=user&goodsCode=bike",
				complete : function(XMLHttpRequest,textStatus){
					if('true' == XMLHttpRequest.responseText){
						alert("秒杀成功");
					}else{
						alert("######秒杀失败######");
					}
				}
			});
		});
	});

</script>

</head>
<body>
	<center>
		<h1>当前服务器端口：<%=request.getLocalAddr() + ":" + request.getLocalPort() %></h1>
		<h1><button id="test" style="width: 100px;height: 50px;font-size: 30px">抢购</button></h1>
		<img src="resources/seckill.png" width="1100px"/>
	</center>

</body>
</html>