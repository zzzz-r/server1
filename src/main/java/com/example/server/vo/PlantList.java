package com.example.server.vo;

import lombok.Data;

@Data
public class PlantList {
    private Integer id;
    private String name;
    private Integer state;
    private Boolean alarm;
    private Float capacity;
    private Float power;
    private Float dayGen;
    private String buildDate;
    private String cover;

}
