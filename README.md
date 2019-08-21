# damaiapi
大麦抢票

MtopRequest.java java端抢票代码

MyLocalProxyServer.java android端使用xposed注入大麦app启动local server,用于获取getMtopApiSign getSecBodyDataEx getAvmpSign，
该签名参数使用阿里系聚安全sdk,难以正面逆向，故使用黑盒调用方式
