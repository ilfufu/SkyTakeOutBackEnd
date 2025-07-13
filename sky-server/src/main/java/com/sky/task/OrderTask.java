package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    @Scheduled(cron = "0 * * * * ? ") // 每分钟触发一次
    public void processTimeoutOrder(){
        LocalDateTime now = LocalDateTime.now();
        log.info("处理超时订单：{}", now);

        LocalDateTime timeout = now.plusMinutes(-15);

        List<Orders> orders = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, timeout);

        if(orders != null && orders.size() > 0){
            for(Orders order : orders){
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时，自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }

    }

    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨一点触发一次
    public void processDeliveryOrder(){
        LocalDateTime now = LocalDateTime.now();
        log.info("定时处理派送中订单：{}", now);
        LocalDateTime timeout = now.plusMinutes(-60);

        List<Orders> orders = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, timeout);
        if(orders != null && orders.size() > 0){
            for(Orders order : orders){
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }

    }

}
