package com.example.server.controllers;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.server.entity.MiDayPower;
import com.example.server.entity.PlantBasicInfo;
import com.example.server.entity.PlantPower;
import com.example.server.entity.PlantPowerHistory;
import com.example.server.exception.PoiException;
import com.example.server.service.IPlantBasicInfoService;
import com.example.server.service.IPlantPowerService;
import com.example.server.service.PlantPowerHistoryService;
import com.example.server.vo.PlantList;
import com.example.server.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/plant")
public class PlantController {
    @Resource
    private IPlantPowerService plantPowerService;
    @Resource
    private IPlantBasicInfoService plantBasicInfoService;
    @Resource
    private PlantPowerHistoryService plantPowerHistoryService;
    @GetMapping("/list") //分页待修改
    public Result list(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "1000")int pageSize){
        Page<PlantPower> page = new Page<>(pageNum, pageSize);
        IPage<PlantPower> pageResult = plantPowerService.page(page);

        List voList = pageResult.getRecords().stream().map( poi->{
            PlantList poiVo = new PlantList();
            PlantBasicInfo plantBasicInfo = plantBasicInfoService.getById(poi.getId());

            BeanUtils.copyProperties(poi,poiVo); //将poi中的数据复制给poiVo(名称相同的数据)，前提是必须要有getter和setter
            poiVo.setName(plantBasicInfo.getName());
            poiVo.setCapacity(plantBasicInfo.getCapacity());
            poiVo.setBuildDate(plantBasicInfo.getBuildDate());
            return poiVo;
        }).collect(Collectors.toList());

        pageResult.setRecords(voList);
        return Result.success(pageResult);
    }

    @GetMapping("/detail/info/{id}") // 获得电站基本信息
    // 通过访问路径获得参数
    public Result detailInfo(@PathVariable int id){
        PlantBasicInfo poi = plantBasicInfoService.getById(id);
        if(poi == null){
            throw PoiException.NotFound(); //查找失败抛出异常
        }
        return Result.success(poi);
    }

    @GetMapping("/detail/plantList/{id}") // 获得电站List信息
    // 通过访问路径获得参数
    public Result detailPlantList(@PathVariable int id){
        PlantBasicInfo plantBasicInfo = plantBasicInfoService.getById(id);
        PlantPower plantPower = plantPowerService.getById(id);
        if(plantBasicInfo == null || plantPower== null){
            throw PoiException.NotFound(); //查找失败抛出异常
        }
        PlantList plantList = new PlantList();
        BeanUtils.copyProperties(plantBasicInfo,plantList);
        BeanUtils.copyProperties(plantPower,plantList);
        return Result.success(plantList);
    }

    @GetMapping("/detail/history/{id}") // 获得电站历史信息
    // 通过访问路径获得参数
    public Result detailPlantHistory(@PathVariable int id){
        QueryWrapper<PlantPowerHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        List<PlantPowerHistory> plantPowerHistoryList = plantPowerHistoryService.list(queryWrapper);
        return Result.success(plantPowerHistoryList);
    }

    @PostMapping("/add") // 添加电站
    // body请求中raw json参数
    public Result add(@RequestBody PlantBasicInfo poiForm){ //@RequestBody获取前端的json数据给poiForm对象
        plantBasicInfoService.saveNewPlant(poiForm);
        return detailInfo(poiForm.getId());
    }

    @PutMapping ("/edit/{id}") // 编辑电站
    public Result edit(@RequestBody PlantBasicInfo poiForm, @PathVariable int id){
        log.info("poi:{}",poiForm);
        plantBasicInfoService.updatePlant(poiForm);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}") //删除电站
    public Result delete(@PathVariable int id){
        plantPowerService.deletePlant(id);
        return Result.success();
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
        // 从数据库查询出所有的数据
        List<PlantPower> plantPowerList = plantPowerService.list();
        List voList = new ArrayList();
        for(PlantPower poi: plantPowerList){
            PlantList poiVo = new PlantList();
            BeanUtils.copyProperties(poi,poiVo);
            PlantBasicInfo plantBasicInfo = plantBasicInfoService.getById(poi.getId());
            poiVo.setName(plantBasicInfo.getName());
            poiVo.setBuildDate(plantBasicInfo.getBuildDate());
            voList.add(poiVo);
        }
        // 通过工具类创建writer 写出到磁盘路径
//        ExcelWriter writer = ExcelUtil.getWriter(filesUploadPath + "/用户信息.xlsx");
        // 在内存操作，写出到浏览器
        ExcelWriter writer = ExcelUtil.getWriter(true);

        // 一次性写出list内的对象到excel，使用默认样式，强制输出标题
        writer.write(voList, true);

        // 设置浏览器响应的格式
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        String fileName = URLEncoder.encode("电站发电信息表", "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        ServletOutputStream out = response.getOutputStream();
        writer.flush(out, true);
        out.close();
        writer.close();
    }
}
