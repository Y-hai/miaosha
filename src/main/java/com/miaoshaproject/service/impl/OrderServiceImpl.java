package com.miaoshaproject.service.impl;

import com.miaoshaproject.dao.ItemStockDOMapper;
import com.miaoshaproject.dao.OrderDOMapper;
import com.miaoshaproject.dao.SequenceDOMapper;
import com.miaoshaproject.dao.StockLogDOMapper;
import com.miaoshaproject.dataobject.OrderDO;
import com.miaoshaproject.dataobject.SequenceDO;
import com.miaoshaproject.dataobject.StockLogDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.OrderModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ItemService itemService;

//    @Autowired
//    private UserService userService;

    @Resource
    private OrderDOMapper orderDOMapper;

    @Resource
    private SequenceDOMapper sequenceDOMapper;

    @Resource
    private ItemStockDOMapper itemStockDOMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Resource
    private StockLogDOMapper stockLogDOMapper;

//    @Resource
//    private CacheService cacheService;

    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId) throws BusinessException {

        //1.校验下单状态，下单的商品是否存在，用户是否合法，购买数量是否正确
//        ItemModel itemModel = itemService.getItemById(itemId);
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "商品信息不存在");
        }

//
//        UserModel userModel = userService.getUserByIdInCache(userId);
//        if (userModel == null) {
//            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "用户信息不存在");
//        }

        if (amount <= 0 || amount > 99) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "数量信息不存在");
        }

        //校验活动信息
        if (promoId != null) {
//            //(1)校验对应活动是否存在这个适用商品
//            if (promoId.intValue() != itemModel.getPromoModel().getId()) {
//                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动信息不正确");
//                //(2)校验活动是否正在进行中
//            } else if (itemModel.getPromoModel().getStatus() != 2) {
//                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动信息不正确");
//            }
            //2.落单减库存，这里只是扣减了Redis，并没有发送消息给broker
            boolean result = itemService.decreaseStock(itemId, amount);
            if (!result)
                throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        } else {
            // 对于非活动商品直接扣减数据库库存，增加商品销量后统一更新Redis
            int affectedRow = itemStockDOMapper.decreaseStock(itemId, amount);
            if (affectedRow <= 0) {
                throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
            }
        }

        //3.订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        orderModel.setPromoId(promoId);

        if (promoId != null) {
            // 如果是活动商品，注入活动商品单价
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        } else {
            // 对于非活动商品，注入商品单价
            orderModel.setItemPrice(itemModel.getPrice());
        }

        // 注入订单总花费金额
        orderModel.setOrderPrice(orderModel.getItemPrice().
                multiply(BigDecimal.valueOf(amount)));

        // 生成交易流水号，订单号
        orderModel.setId(generateOrderNo());
        OrderDO orderDO = this.convertFromOrderModel(orderModel);
        orderDOMapper.insertSelective(orderDO);

        //加上商品的销量
        itemService.increaseSales(itemId, amount);

        if (promoId != null) {
            // 设置库存流水状态为成功，这个地方并不会花费很多时间，因为行锁不会产生锁竞争
            StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
            if (stockLogDO == null) {
                throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
            }
            stockLogDO.setStatus(2);
            stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);
        } else {
            // 设置缓存
            redisTemplate.opsForValue().set("item_" + itemId, itemService.getItemById(itemId));
            // 设置缓存失效时间
            redisTemplate.expire("item_" + itemId, 10, TimeUnit.MINUTES);
        }

        // 清除guava cache缓存
//        cacheService.rmCommonCache("item_" + itemId);
//        redisTemplate.delete("item_" + itemId);

//        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
//            @Override
//            public void afterCommit() {
//                // 发送异步消息给broker，在最近的事务执行成功之后才会执行
//                boolean mqResult = itemService.asyncDecreaseStock(itemId, amount);
////                if (!mqResult) {
////                    itemService.increaseStock(itemId, amount);
////                    throw new BusinessException(EmBusinessError.MQ_SEND_FAIL);
////                }
//            }
//        });

        //4.返回前端
        return orderModel;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
        //不管该方法是否在事务中，都会开启一个新的事务，不管外部事务是否成功
        //最终都会提交掉该事务，为了保证订单号的唯一性，防止下单失败后订单号的回滚
    String generateOrderNo() {
        //订单有16位
        StringBuilder stringBuilder = new StringBuilder();
        //前8位为时间信息，年月日
        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuilder.append(nowDate);

        //中间6位为自增序列
        //获取当前sequence，数据量过大，数据库设计成循环sequence，增加上下阈值字段
        int sequence = 0;
        SequenceDO sequenceDO = sequenceDOMapper.getSequenceByName("order_info");

        sequence = sequenceDO.getCurrentValue();
        sequenceDO.setCurrentValue(sequenceDO.getCurrentValue() + sequenceDO.getStep());
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);
        //拼接
        String sequenceStr = String.valueOf(sequence);
        for (int i = 0; i < 6 - sequenceStr.length(); i++) {
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);

        //最后两位为分库分表位,暂时不考虑
        stringBuilder.append("00");

        return stringBuilder.toString();
    }

    private OrderDO convertFromOrderModel(OrderModel orderModel) {
        if (orderModel == null) {
            return null;
        }
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel, orderDO);
        return orderDO;
    }
}
