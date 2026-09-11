# Day1

完成登录 登出 jwt校验

重点内容

- jwt

  header+payload+signature  （signature由header+payload+secretKey计算得到）

  header中存放算法，类型    payload中存放用户相关信息等   signature用于检测token是否被篡改

  interceptor拿到token以后用header和payload和本地secretKey计算得到signature  如果前两个被更改  signature会改变

- 拦截器interceptor