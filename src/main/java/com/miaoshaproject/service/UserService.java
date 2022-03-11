package com.miaoshaproject.service;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.service.model.UserModel;

public interface UserService {

    UserModel getUserById(Integer id);

    // 通过缓存获取用户对象
    UserModel getUserByIdInCache(Integer id);

    // 用户注册登记（创建用户）
    void register(UserModel userModel) throws BusinessException;

    UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException;
}
