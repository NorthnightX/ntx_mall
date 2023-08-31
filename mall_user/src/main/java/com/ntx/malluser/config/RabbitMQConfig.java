package com.ntx.malluser.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfig {
    //邮件
    public static final String EMAIL_EXCHANGE = "email_exchange";
    public static final String EMAIL_QUEUE = "email_queue";
    public static final String EMAIL_KEY = "email_key";

    //邮件

    /**
     * 邮箱交换机
     * @return
     */
    @Bean
    public TopicExchange emailExchange(){
        return new TopicExchange(EMAIL_EXCHANGE);
    }

    /**
     * 队列
     * @return
     */
    @Bean
    public Queue emailQueue(){
        return new Queue(EMAIL_QUEUE);
    }

    /**
     * 绑定关系，将队列绑定到交换机
     * @return
     */
    @Bean
    public Binding emailBindingBuilder(){
        return BindingBuilder.bind(emailQueue()).to(emailExchange()).with(EMAIL_KEY);
    }



}
