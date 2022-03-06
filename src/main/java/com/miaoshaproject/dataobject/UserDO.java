package com.miaoshaproject.dataobject;

import lombok.Data;

@Data
public class UserDO {

    private Integer id;

    private String name;

    private Byte gender;

    private Integer age;

    private String telphone;

    private String regisitMode;

    private Integer thirdPartyId;

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public void setTelphone(String telphone) {
        this.telphone = telphone == null ? null : telphone.trim();
    }

    public void setRegisitMode(String regisitMode) {
        this.regisitMode = regisitMode == null ? null : regisitMode.trim();
    }

}