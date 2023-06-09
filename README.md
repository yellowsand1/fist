# fist

***A new-bee's personal distribute transaction frame for Java .***

## 1st Step

import this jar using maven

```java
    <dependency>
        <groupId>org.chad.notFound</groupId>
        <artifactId>fist4j</artifactId>
        <version>1.0.1-SNAPSHOT</version>
    </dependency> 
```


​        

## 2st Step

config in application.yml  


```yml
fist:
  server:
    addr: 127.0.0.1
    port: 11118
  target:
    database:
      url: jdbc:mysql://127.0.0.1:3306/fist?zeroDateTimeBehavior=CONVERT_TO_NULL&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useOldAliasMetadataBehavior=true&useSSL=false&rewriteBatchedStatements=true
      username: root
      password: @@@@@@
  mode: tcc
```

U can config different database even different type of dataSource in ur distribute project ! Replace tcc to saga to enable saga mode



## 3st Step

Start  fist server and config in toml or not.



## 4st Step

Use  @GlobalTransactional on the mothod u wanna use distribute transactional .

```java
    @GlobalTransactional(rollbackFor = Exception.class)
    @Override
    public void rpcTest() {
        FistDao.RPC_TEST.update();
    }

```

Notice : there's no default rollbackFor exception , U must name one at least !



Now it's all done , try globalTransaction .

## Two mode to enable

### TCC

```java
fist:
  mode: tcc
```

just simple like that !

### SAGA

```java
fist:
  mode: saga
```

Due to some SAGA mode that already exists can't promise data consistency ，U can make that happen using fist .

Due to the lock in SAGA , the TCC mode is more effective , but SAGA maybe more reliable cause SAGA won't hold the connection till the transaction ends , less JDBC connection run out Exception .If there's no such high demand in data consistency , U can use SAGA and implement the lock interface by doing nothing to improve concurrency , or U can use different lock with different group , it's up to u .

To define the group to seperate different service group in one instance , this will lock the instance untill globalTransaction finish . Every group using different lock ,same group in different instance don't relate .

U can implement org.chad.notFound.lock.FistLock to implement ur own lock and announce it as a Bean . By default I use a simple lock, which can only lock single instance. U can use a distribute lock to keep data consistency in distribute and multiple instance situation .

## Contact me :

WeChat : ![IMG_0398](https://user-images.githubusercontent.com/9192351/236183935-0c3149ce-6a5a-4757-897f-161dcb9a914d.jpg)
