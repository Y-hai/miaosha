package com.miaoshaproject.dataobject;

import lombok.Data;

@Data
public class UserPasswordDO {

    private Integer id;

    private String encrptPassword;

    private Integer userId;

    public void setEncrptPassword(String encrptPassword) {
        this.encrptPassword = encrptPassword == null ? null : encrptPassword.trim();
    }

    @Override
    public String toString() {
        return "UserPasswordDO{" +
                "id=" + id +
                ", encrptPassword='" + encrptPassword + '\'' +
                ", userId=" + userId +
                '}';
    }
}