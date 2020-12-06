package com.simpson.sqssample.domain.service;

import com.amazon.sqs.javamessaging.AmazonSQSMessagingClientWrapper;
import com.amazon.sqs.javamessaging.message.SQSTextMessage;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.simpson.sqssample.domain.model.SNSPushMsg;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

@Slf4j
@Service
public class SNSPushMsgService {
    @Value("${aws.sqs.queue-name:test-fifo-queue}")
    private String queueName;
    
    @Autowired
    private QueueMessagingTemplate queueMessagingTemplate;
    
    @Autowired
    private AmazonSQSMessagingClientWrapper clientWrapper;
    
    @Autowired
    public MessageProducer producer;
    
    @Autowired
    public Session sqsSession;
    
    @Autowired
    private boolean isSQSFifo;
    
    @Autowired
    private MessageConsumer consumer;
    
    public void send(SNSPushMsg pushMsg, String targetIdx, int targetType , long msgIdx) {
        send((new Gson()).toJson(pushMsg), targetIdx, targetType, msgIdx);
    }
    
    public void send(String message, String targetIdx, int targetType , long msgIdx) {
        if (isSQSFifo)
            sendFifo(message, targetIdx, targetType, msgIdx);
        else
            sendAsync(message);
    }
    
    public void sendAsync(String message) {
        this.queueMessagingTemplate.send(queueName, MessageBuilder.withPayload(message).build());
    }
    
    public void sendFifo(String message, String targetIdx, int targetType , long msgIdx) {
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withMessageBody(message)
                .withMessageGroupId("simpson")
                .withMessageDeduplicationId(
                        targetIdx + ":" + targetType + ":"  +msgIdx);
        try {
            clientWrapper.sendMessage(sendMessageRequest);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
    
    @Scheduled(initialDelay = 3000, fixedRate = 1000)
    public void checkRedisQueueBySecond1() {
        try {
            SQSTextMessage message = (SQSTextMessage)consumer.receive(1000);
            if (message != null) {
                log.info("read data");
                log.info(message.getText());
                log.info(message.getReceiptHandle());
                DeleteMessageRequest request = new DeleteMessageRequest().withReceiptHandle(message.getReceiptHandle());
                clientWrapper.deleteMessage(request);
            }
        } catch (Exception e) {
            log.error("Exception: ", e);
        }
    }
}
