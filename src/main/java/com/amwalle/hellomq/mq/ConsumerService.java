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
