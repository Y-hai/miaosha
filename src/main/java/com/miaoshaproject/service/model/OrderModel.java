package com.miaoshaproject.service.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class OrderModel implements Serializable {
    //交易单号，例如2019052100001212，使用string类型
    private String id;

    //购买的用户id
    private Integer userId;

    //购买的商品id
    private Integer itemId;

    //若非空，则表示是以秒杀商品方式下单
    private Integer promoId;

    //购买时商品的单价,若promoId非空，则表示是以秒杀商品方式下单
    private BigDecimal itemPrice;

    //购买数量
    private Integer amount;

    //购买金额
    private BigDecimal orderPrice;

}
