# Day1

完成登录 登出 jwt校验

重点内容

- jwt

  header+payload+signature  （signature由header+payload+secretKey计算得到）

  header中存放算法，类型    payload中存放用户相关信息等   signature用于检测token是否被篡改

  interceptor拿到token以后用header和payload和本地secretKey计算得到signature  如果前两个被更改  signature会改变

- 拦截器interceptor





**挂号功能**：

首先：挂号要并发修改数据库-> 引入并发问题 -> 第一版用条件update修改库存->新问题  ：能解决并发问题但是大量请求打到数据库吞吐量太低

然后： 为了优化，引入redis缓存库存，每次挂号先查redis，然后预扣（这里采用lua脚本原子预扣），成功后才访问mysql真正扣减库存，这样可以保证没号的请求不会大量打到数据库

接着：由于引入了缓存，必然会带来一系列问题：比如-数据一致性：何时更新redis缓存？redis与mysql票数不一致怎么办？
紧接着：发现可能出现以下问题，1，管理员放号，缓存中没及时更新，2.缓存中票数大于mysql  3.票数小于mysql

为了解决：

1->采用懒加载，缓存中没有数据会再查一遍mysql，另外，增量同步管理员调整 total_capacity→ MySQL 更新→ 提交后计算 delta = 新容量 - 旧容量→ Redis Lua INCRBY delta，

2.-预扣减成功，到mysql真实扣减失败，（这里采用的是判断mysql条件update的影响行数），发现为0，数据库事务回滚，会restore ，因为真实扣减失败有可能只是因为mysql错误，然后触发延迟三秒的库存校验，比较缓存与mysql，发现不一致就直接删除缓存，等待下一次懒加载  

3.缓存票数为零，当前请求直接返回票数不足，启动异步任务3秒左右后访问mysql，同时在redis setnx一个ttl略大于三秒的键值（相当于锁），发现mysql还有库存，则删除缓存，依旧等待懒加载

**注意**：如果没有这个锁，所有发现缓存中没有票的缓存都会去访问mysql  redis一定程度上就会失去抗并发的作用！！
TODO：引入消息队列（之后再做）
**注意**：Redis 用于高并发快速失败和削峰，MySQL 用于最终防超卖，延迟校验机制用于修复 Redis 与 MySQL 的短暂不一致。