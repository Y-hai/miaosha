package com.miaoshaproject.dataobject;

import lombok.Data;

@Data
public class SequenceDO {

    private String name;

    private Integer currentValue;

    private Integer step;

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

}