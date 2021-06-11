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
