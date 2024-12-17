package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务类，定时处理订单状态
 */
@Slf4j
@Component
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeOutOrder(){
        log.info("定时处理超时订单：{}", LocalDateTime.now());

        //处理状态为未支付，且下单时间为15分钟之前的订单
        LocalDateTime time = LocalDateTime.now().plusMinutes(-1);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeOutLT(Orders.PENDING_PAYMENT, time);

        if (ordersList != null && ordersList.size() > 0){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时,自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 定时处理派送中的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliverOrder(){
        log.info("定时处理派送中的订单：{}",LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().plusHours(-1);

        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeOutLT(Orders.DELIVERY_IN_PROGRESS, time);

        if (ordersList != null && ordersList.size() > 0){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }















}
