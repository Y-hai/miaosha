package com.miaoshaproject.dataobject;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ItemDO {

    private Integer id;

    private String title;

    private BigDecimal price;

    private String description;

    private Integer sales;

    private String imgUrl;

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl == null ? null : imgUrl.trim();
    }
}