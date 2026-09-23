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

2.-预扣减成功，到mysql真实扣减失败，（这里采用的是判断mysql条件update的影响行数），发现为0，，触发延迟库存核对，原业务异常继续抛出，mysql事务回滚，redis补偿票数回滚，三秒后比较redis，是否不变，mysql是否为零，确认不一致后安全删除redis key

3.缓存票数为零，当前请求直接返回票数不足，同时尝试在redis setnx一个ttl略大于三秒的键值（相当于锁），然后利用taskscheduler启动三秒后的异步延迟修复任务，利用lua脚本，查询redis缓存是否改变，查询mysql是否还有库存，则删除缓存，依旧等待懒加载

**注意**：如果没有这个锁，所有发现缓存中没有票的缓存都会去访问mysql  redis一定程度上就会失去抗并发的作用！！
TODO：引入消息队列（之后再做）
**注意**：Redis 用于高并发快速失败和削峰，MySQL 用于最终防超卖，延迟校验机制用于修复 Redis 与 MySQL 的短暂不一致。



**候补功能**：
基本流程：
一：某患者发现某号源无空缺后可以选择加入候补，后端数据库维护一张appointmentWaitlist表，依旧采用数据库生成唯一约束保证重复候补的幂等性

二：某患者退号，直接去寻找对应号源的候补记录，按FIFO顺序给予候补资格，waiting-offered，并设置过期时间（time），（等待患者确认，患者确认后，offered变为confirmed，加入我的预约相关表中）事务提交成功后（改为offered），启动rabbitmq异步任务（afterCommit），（可以避免出现数据库回滚，消息却已发出的数据不一致问题）通过死信队列加上ttl实现延迟消息，time后去查找对应数据库记录，若已过期则设置成expired（**注意**：，这里设置expired也是采用一条sql保证幂等性，最终通过affectRows判断是否修改成功（0,表示可能已经确认，已经过期，或者消息重复投递；1表示真正完成过期处理）（经典问题：用户候补确认与消费者执行过期检验同时发生）



**注意**：患者取消候补时，以及某患者的候补资格过期时（在候补补位过程中，按照 `created_at, id` 查询最早的 `WAITING` 记录，并通过 `FOR UPDATE` / 条件 UPDATE 控制并发，避免多个号源释放线程同时选中同一个候补患者。）号源不会直接恢复到公共库存，而是先优先尝试分给候补人员（按created_at和id字段排序），只有在没有对应号源的wating记录时才会走之前实现的取消号源流程，恢复redis和mysql库存）

可靠性保证：生产者确认开启，自定义confirmcallback ，每条消息带有唯一CorrelationData，returncallback，消费者确认，消费端通过 Spring Retry 配置有限次数重试，重试耗尽后由 `RepublishMessageRecoverer` 将失败消息重新发布到故障队列。，消费者重试两次后将消息投递到一个队列中，保留原始消息，原交换机，原RoutingKey，异常信息和异常堆栈，便于留痕以及人工跟踪，开启spring兜底扫描定时任务（重试周期稍大）  每隔一段时间扫描数据库中的已过期offered数据，防止出现rabbitmq故障的情况，



![image-20260922105031182](D:\healthy\log\log.mg.assets\image-20260922105031182.png)

幂等判断经典