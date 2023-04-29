package com.example.server.vo;

import lombok.Data;

import java.util.Date;

@Data
public class MiAlarmVo {
    private Object id;
    private Integer state;
    private Object plantId;
    private Integer ifAlarm;
    private Integer temperature;
    private Integer freq;
    private Integer dcCurrent;
    private Integer dcVoltage;
    private Integer acCurrent;
    private Integer acVoltage;
    private Integer gridVoltage;
    private Date updateTime;
}
