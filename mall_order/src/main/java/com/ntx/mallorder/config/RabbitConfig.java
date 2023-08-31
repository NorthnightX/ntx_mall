package com.ntx.mallorder.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {
    public static final String ORDER_EXCHANGE = "order_exchange";
    public static final String ORDER_QUEUE = "order_queue";
    public static final String ORDER_KEY = "order_key";

    public static final String ORDER_TTL_EXCHANGE = "order_ttl_exchange";
    public static final String ORDER_TTL_QUEUE = "order_ttl_queue";
    public static final String ORDER_TTL_KEY = "order_ttl_key";

    public static final String ORDER_CHECK_EXCHANGE = "order_check_exchange";
    public static final String ORDER_CHECK_QUEUE = "order_check_queue";
    public static final String ORDER_CHECK_KEY = "order_check_key";

    //订单主队列
    @Bean
    public TopicExchange orderExchange(){
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue orderQueue(){
        return new Queue(ORDER_QUEUE);
    }

    @Bean
    public Binding orderBindingBuilder(){
        return BindingBuilder.bind(orderQueue()).to(orderExchange()).with(ORDER_KEY);
    }
    //死信队列
    @Bean
    public TopicExchange orderTTLExchange(){
        return new TopicExchange(ORDER_TTL_EXCHANGE);
    }

    @Bean
    public Queue orderTTLQueue(){
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", ORDER_CHECK_EXCHANGE); // 设置死信交换机
        arguments.put("x-dead-letter-routing-key", ORDER_CHECK_KEY);   // 设置死信路由键
        //设置过期时间：1分钟
        arguments.put("x-message-ttl", 60000);
        return new Queue(ORDER_TTL_QUEUE, true, false, false, arguments);
    }

    @Bean
    public Binding orderTTLBindingBuilder(){
        return BindingBuilder.bind(orderTTLQueue()).to(orderTTLExchange()).with(ORDER_TTL_KEY);
    }

    //订单检查队列
    @Bean
    public TopicExchange orderCheckExchange(){
        return new TopicExchange(ORDER_CHECK_EXCHANGE);
    }

    @Bean
    public Queue orderCheckQueue(){
        return new Queue(ORDER_CHECK_QUEUE);
    }

    @Bean
    public Binding orderCheckBindingBuilder(){
        return BindingBuilder.bind(orderCheckQueue()).to(orderCheckExchange()).with(ORDER_CHECK_KEY);
    }
}
