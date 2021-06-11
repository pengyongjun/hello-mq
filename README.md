# 基本概念
![rocket mq](https://symbols.getvecta.com/stencil_74/28_apache-rocketmq.5d1c2e98a3.svg)

[Apache RocketMQ](https://rocketmq.apache.org/) is a distributed messaging and streaming platform with low latency, high performance and reliability, trillion-level capacity and flexible scalability.

RocketMQ主要由 Producer、Broker、Consumer 三部分组成，其中Producer 负责生产消息，Consumer 负责消费消息，Broker 负责存储消息。Broker 在实际部署过程中对应一台服务器，每个 Broker 可以存储多个Topic的消息，每个Topic的消息也可以分片存储于不同的 Broker。Message Queue 用于存储消息的物理地址，每个Topic中的消息地址存储于多个 Message Queue 中。ConsumerGroup 由多个Consumer 实例构成。

更多详细的概念可以参考: [Apache RocketMQ开发者指南](https://github.com/apache/rocketmq/tree/master/docs/cn)

# 安装RocketMQ
可以从 [Apache RocketMQ Quick Start](https://rocketmq.apache.org/docs/quick-start/) 进入下载页获取下载链接，如：[https://mirrors.tuna.tsinghua.edu.cn/apache/rocketmq/4.8.0/rocketmq-all-4.8.0-bin-release.zip](https://mirrors.tuna.tsinghua.edu.cn/apache/rocketmq/4.8.0/rocketmq-all-4.8.0-bin-release.zip)

我这里安装的时候，是直接下载的“binary release”包，所以安装的时候只需要解压即可。

安装之前先确认满足如下依赖：

- 64bit OS, Linux/Unix/Mac is recommended;(Windows user see guide below)
- 64bit JDK 1.8+;
- Maven 3.2.x;
- Git;
- 4g+ free disk for Broker server

具体安装步骤如下，完成之后rocketmq就被安装到了 `/usr/local/rocketmq-4.8.0` 目录：

```shell
wget https://www.apache.org/dyn/closer.cgi?path=rocketmq/4.8.0/rocketmq-all-4.8.0-bin-release.zip
unzip rocketmq-all-4.8.0-bin-release.zip
mv rocketmq-all-4.8.0-bin-release /usr/local/rocketmq-4.8.0
```

# 配置启动环境
我这里配置主要是因为机器上的内存不足，如果运行的机器有16G内存，那这里完全不用修改。

- 如果内存确实不够，可以考虑配置一个swap分区，操作如下：

```shell
# 查看内存使用情况
free -hm

# 查看文件系统容量是否足以创建所需的分区
df -h

# 创建分区，如下是创建一个8GB的分区
# if=文件名：表示指定源文件
# of=文件名：表示指定目的文件，可以自己去设定目标文件路径。
# bs=xx：同时设置读入/写出的“块”大小
# count=xx：表示拷贝多少个“块”
# bs * count 为拷贝的文件大小，即swap分区大小
dd if=/dev/zero of=/data/swap bs=1024 count=8388616

# 格式化分区文件
mkswap /data/swap

# 将新建的分区文件设为swap分区
swapon /data/swap

# 设置开机自动挂载swap分区
echo "/data/swap swap swap defaults 0 0" >> /etc/fstab

# 设置swappiness权重
vim /etc/sysctl.conf

# 将vm.swappiness设置为30或者60 ，然后保存退出（swappiness=0 的时候表示最大限度使用物理内存，然后才是 Swap 空间，swappiness＝100 的时候表示积极的使用 Swap 分区，并且把内存上的数据及时的搬运到 Swap 空间里面）
vm.swappiness = 60

# 最后可以再执行 free -h 检查swap分区设置和使用情况
free -h
```

- 配置broker内存

```shell
cd /usr/local/rocketmq-4.8.0/bin/
vim runbroker.sh

# 将 -server -Xms8g -Xmx8g -Xmn4g 配置修改为与自己机器匹配的大小
# 例如，我改成了 -server -Xms1g -Xmx4g -Xmn512m
# 改完之后保存并退出
```
- 配置name server内存

```shell
# 同样的方式，编辑 runserver.sh
vim runserver.sh

# 将 -server -Xms4g -Xmx4g -Xmn2g -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=320m 配置修改为与自己机器匹配的大小
# 例如，我改成了 -server -Xms512m -Xmx1g -Xmn256m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m
# 改完之后保存并退出
```

# 启动/停止RocketMQ
- 首先进入rocket mq bin目录 `cd /usr/local/rocketmq-4.8.0/bin/`
- 启动RocketMQ（name server 和 broker）

```shell
# 启动 name server
nohup sh mqnamesrv &

# 启动 broker（如果只是在本地使用，那么直接IP地址直接用127.0.0.1即可，否则需要执行 ifconfig 命令，查看并使用eth0的IP地址）
# -n 表示设置 Name server addres，可以使用 sh mqbroker -h 查看使用帮助
nohup sh mqbroker -n 172.17.12.66:9876 -c ../conf/broker.conf &
```

注：如果RockerMQ是安装在云上，例如我安装在阿里云，那么要想在本地进行远程连接使用，那么还需要配置broker ip地址，操作如下：

1. 进入rocket mq配置目录： `cd /usr/local/rocketmq-4.8.0/conf`
2. 编辑runbroker.conf：`vim runbroker.conf`
3. 在文件末尾加上： `brokerIP1 = 127.0.0.1` （这里IP地址要根据自己的云外网IP进行配置）
4. 保存并退出
5. 回到rocket mq bin目录，然后再启动 broker： `nohup sh mqbroker -n 172.17.12.66:9876 -c ../conf/broker.conf &`

- 新建 Topic（Topic新建一次即可）

```shell
sh mqadmin updateTopic -b 172.17.12.66:10911 -t TopicA
```
- 新建 subscription group （subscription group新建一次即可）

```shell
sh mqadmin updateSubGroup -b 172.17.12.66:10911 -g SubGroupA
```
- 停止Rocket MQ

```shell
# 停止 broker
sh mqshutdown broker

# 停止 name server
sh mqshutdown namesrv
```

# 验证生产/消费消息
消息发送和接收，可以用 mqadmin 命令，如果不清楚命令使用方式，可以执行 `sh mqadmin` 查看可以使用的命令

- 生产消息，如果看到 Send Result 是 “SEND_OK”，则说明发送成功

```shell
sh mqadmin sendMessage -b 172.17.12.66:10911 -t TopicA -n 172.17.12.66:9876 -p "hello rocket mq"
```

- 消费消息，如果能看到刚发送的消息详情，则说明可以消费成功

```shell
sh mqadmin consumeMessage -b 172.17.12.66:10911 -t TopicA -n 172.17.12.66:9876
```

# SpringBoot发送/消费消息
这里主要贴一下核心代码，详细的可以参考GitHub：[https://github.com/pengyongjun/hello-mq](https://github.com/pengyongjun/hello-mq)

- 工程目录：

```plaintext
hello-mq
├── hello-mq.iml
├── pom.xml
├── src
│   └── main
│       ├── java
│       │   └── com
│       │       └── amwalle
│       │           └── hellomq
│       │               ├── MqApplication.java
│       │               ├── controller
│       │               │   └── HelloRocketMqController.java
│       │               ├── dto
│       │               │   └── ResponseDTO.java
│       │               └── mq
│       │                   ├── ConsumerService.java
│       │                   └── ProducerService.java
│       └── resources
│           ├── application.properties
│           └── logback-spring.xml
└── target
```
- 生产消息（ProducerService.java）：

```java
package com.amwalle.hellomq.mq;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ProducerService {
    private final static Logger logger = LoggerFactory.getLogger(ProducerService.class);

    private static String nameServer;

    private static DefaultMQProducer producer;

    private static String producerGroup;

    @Value("${mq.nameserver}")
    public void setNameSerer(String server) {
        nameServer = server;
    }

    @Value("${mq.consumer.group}")
    public void setProducerGroup(String group) {
        producerGroup = group;
    }

    /**
     * 初始化producer
     */
    @PostConstruct
    public static synchronized void initProducer() {
        try {
            if (producer != null) {
                producer.shutdown();
            }

            producer = new DefaultMQProducer(producerGroup);
            producer.setNamesrvAddr(nameServer);
            producer.start();
            logger.info("Init producer success!");
        } catch (MQClientException e) {
            logger.error("Init producer error: " + e.getErrorMessage());
        }
    }

    /**
     * 发送消息
     *
     * @param topic   发送消息的Topic
     * @param tags    发送消息的Tag
     * @param content 发送的消息内容
     *
     * @return true-发送成功，false-发送失败
     */
    public static boolean sendMessage(String topic, String tags, String content) {
        Message message = new Message(topic, tags, content.getBytes());
        SendResult sendResult = null;
        try {
            sendResult = producer.send(message);
            logger.info("Message send result: " + sendResult);
        } catch (Exception e) {
            logger.error("Send message failed, " + e.getMessage());
        }

        return sendResult != null && sendResult.getSendStatus() == SendStatus.SEND_OK;
    }
}

```
- 消费消息（ConsumerService.java）：

```java
package com.amwalle.hellomq.mq;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static java.lang.String.format;

@Component
public class ConsumerService {
    private final static Logger logger = LoggerFactory.getLogger(ConsumerService.class);

    private static String nameServer;

    private static String consumerGroup;

    private static String topic;

    private static DefaultMQPushConsumer consumer;

    @Value("${mq.nameserver}")
    public void setNameSerer(String server) {
        nameServer = server;
    }

    @Value("${mq.consumer.group}")
    public void setConsumerGroup(String group) {
        consumerGroup = group;
    }

    @Value("${mq.topic}")
    public void setTopic(String topic1) {
        topic = topic1;
    }

    /**
     * 初始化消费消息
     */
    @PostConstruct
    public static synchronized void initConsumer() {
        if (consumer != null) {
            consumer.shutdown();
        }

        consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        try {
            // subExpression可以限制为生产消息的tags，也可以用*表示消费所有该topic下的消息
            consumer.subscribe(topic, "*");
            consumer.registerMessageListener((MessageListenerConcurrently) (list, consumeConcurrentlyContext) -> {
                logger.info(format("%s Receive New Messages: %s, %n",
                        Thread.currentThread().getName(),
                        list));
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            consumer.start();
            logger.info("Consumer started.");
        } catch (MQClientException e) {
            logger.error("Consumer subscribe error, " + e.getErrorMessage());
        }
    }
}
```
- 配置文件（application.properties）：

```plaintext
server.port=1234

# IP换成自己的name server ip
mq.nameserver=127.0.0.1:9876
mq.consumer.group=SubGroupA
mq.topic=TopicA
```
- controller

```java
package com.amwalle.hellomq.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.amwalle.hellomq.dto.ResponseDTO;
import com.amwalle.hellomq.mq.ProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@RestController
public class HelloRocketMqController {
    private final static Logger logger = LoggerFactory.getLogger(HelloRocketMqController.class);

    @RequestMapping(method = RequestMethod.POST, path = "/send-message")
    public void sendMessage(HttpServletRequest request, HttpServletResponse response) {
        Object data = getRequestData(request);
        JSONObject input = JSON.parseObject(data.toString());
        boolean sendStatus = ProducerService.sendMessage(input.getString("topic"), input.getString("tags"), input.getString("message"));
        if (!sendStatus) {
            sendJSONPResponse(response, new ResponseDTO("100", "failed to send message"));
        } else {
            sendJSONPResponse(response, new ResponseDTO("200", "success"));
        }
    }

    public Object getRequestData(HttpServletRequest request) {
        Object data = null;
        try {
            InputStream inputStream = request.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuffer.append(line);
            }
            data = JSON.parse(stringBuffer.toString());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        return data;
    }

    public void sendJSONPResponse(HttpServletResponse response, Object content) {
        try {
            String result = JSON.toJSONString(content);
            response.setHeader("Content-Type", "application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(result);
        } catch (Exception e) {
            logger.error("Send JSONP response error", e);
        }
    }
}

```
