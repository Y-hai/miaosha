package com.miaoshaproject.dataobject;

import lombok.Data;

@Data
public class StockLogDO {

    private String stockLogId;

    private Integer itemId;

    private Integer amount;

    // 1代表初始状态，2代表下单扣减库存成功，3代表下单回滚
    private Integer status;

}