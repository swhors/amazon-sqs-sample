package com.simpson.sqssample.domain.configuration;

import com.amazon.sqs.javamessaging.AmazonSQSMessagingClientWrapper;
import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

@Configuration
@EnableConfigurationProperties
public class SNSPushMsgConfig {
    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;
    
    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;
    
    @Value("${cloud.aws.region.static}")
    private String region;
    
    @Value("${aws.sqs.endpoint}")
    private String snsEndpointUrl;
    
    @Value("${aws.sqs.queue-name}")
    private String queueName;
    
    @Getter
    private boolean isFifo;
    
    @PostConstruct
    private void setUp() {
        isFifo = queueName.endsWith("fifo");
    }
    
    @Bean
    public boolean isSQSFifo() {
        return isFifo;
    }
    
    @Bean
    public ProviderConfiguration providerConfiguration() {
        return new ProviderConfiguration().withNumberOfMessagesToPrefetch(10);
    }
    
    @Bean(destroyMethod = "shutdown")
    public AmazonSQSAsync amazonSQSAsync() {
        if(isFifo) {
            return AmazonSQSAsyncClientBuilder.defaultClient();
        }
        AmazonSQSAsyncClientBuilder amazonSQSAsyncClientBuilder = AmazonSQSAsyncClientBuilder.standard();
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(accessKey, secretKey);
        return amazonSQSAsyncClientBuilder
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(snsEndpointUrl, region))
                .withCredentials(
                        new AWSStaticCredentialsProvider(basicAWSCredentials))
                .build();
    }
    
    @Bean
    public QueueMessagingTemplate queueMessagingTemplate(AmazonSQSAsync amazonSQSAsync) {
        return new QueueMessagingTemplate(amazonSQSAsync);
    }
    
    @Bean(destroyMethod = "shutdown")
    public AmazonSQS amazonSQS() {
        if(!isFifo) {
            return AmazonSQSClientBuilder.defaultClient();
        }
        AmazonSQSClientBuilder amazonSQSClientBuilder = AmazonSQSClientBuilder.standard();
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(accessKey, secretKey);
        return amazonSQSClientBuilder
                .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(snsEndpointUrl, region)
                ).build();
    }
    
    @Bean
    public SQSConnectionFactory sqsConnectionFactory(AmazonSQS amazonSQS) {
        return new SQSConnectionFactory(providerConfiguration(),
                                        amazonSQS);
    }
    
    @Bean
    public SQSConnection sqsConnection(SQSConnectionFactory factory) throws JMSException {
        return factory.createConnection();
    }
    
    @Bean
    public Session sqsSession(SQSConnection connection) throws JMSException {
        return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }
    
    @Bean
    public MessageProducer producer(Session sqsSession) throws JMSException {
        Queue queue = sqsSession.createQueue(queueName);
        return sqsSession.createProducer(queue);
    }
    
    @Bean
    public AmazonSQSMessagingClientWrapper clientWrapper(SQSConnection sqsConnection) {
        return sqsConnection.getWrappedAmazonSQSClient();
    }
    
    @Bean
    public MessageConsumer consumer(Session sqsSession, SQSConnection connection) throws JMSException {
        Queue queue = sqsSession.createQueue(queueName);
        MessageConsumer consumer = sqsSession.createConsumer(queue);
        connection.start();
        return consumer;
    }
}
