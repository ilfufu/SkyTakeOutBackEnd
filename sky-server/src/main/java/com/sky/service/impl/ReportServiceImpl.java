package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    @Lazy
    private WorkspaceService workspaceService;

    public TurnoverReportVO getTurnoverStatistcs(LocalDate begin, LocalDate end){
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();
        for(LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);

            Double turnover = orderMapper.sumByMap(map);
            if(turnover == null){
                turnover = 0.0;
            }
            turnoverList.add(turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    public UserReportVO getUserStatistcs(LocalDate begin, LocalDate end){
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();

        for(LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();

            map.put("end", endTime);
            totalUserList.add(userMapper.countByMap(map));

            map.put("begin", beginTime);
            newUserList.add(userMapper.countByMap(map));
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    public OrderReportVO getOrderStatistcs(LocalDate begin, LocalDate end){
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        Integer orderCount = 0;
        Integer validOrderCount = 0;

        for(LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            Integer oc = orderMapper.countByMap(map);
            orderCountList.add(oc);
            orderCount = orderCount + oc;

            map.put("status", Orders.COMPLETED);
            Integer voc = orderMapper.countByMap(map);
            validOrderCountList.add(voc);
            validOrderCount = validOrderCount + voc;
        }


        Double orderCompletionRate = orderCount == 0 ?
                0.0 : validOrderCount.doubleValue() / orderCount;

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(orderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end){
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> goodsSalesDTOS = orderMapper.getSalesTop(beginTime, endTime);
        List<String> names = goodsSalesDTOS.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numbers = goodsSalesDTOS.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(names,","))
                .numberList(StringUtils.join(numbers,","))
                .build();
    }

    public void exportBusinessData(HttpServletResponse response){
        LocalDate dataBegin = LocalDate.now().minusDays(30);
        LocalDate dataEnd = LocalDate.now().minusDays(1);

        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dataBegin, LocalTime.MIN), LocalDateTime.of(dataEnd, LocalTime.MAX));

        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据统计报表.xlsx");
        try {
            XSSFWorkbook excel = new XSSFWorkbook(in);
            XSSFSheet sheet = excel.getSheet("Sheet1");

            sheet.getRow(1).getCell(1).setCellValue(
                    "时间" + dataBegin + "至" + dataEnd
            );

            sheet.getRow(3).getCell(2).setCellValue(
                    businessDataVO.getTurnover()
            );

            sheet.getRow(3).getCell(4).setCellValue(
                    businessDataVO.getOrderCompletionRate()
            );

            sheet.getRow(3).getCell(6).setCellValue(
                    businessDataVO.getNewUsers()
            );

            sheet.getRow(4).getCell(2).setCellValue(
                    businessDataVO.getValidOrderCount()
            );

            sheet.getRow(4).getCell(4).setCellValue(
                    businessDataVO.getUnitPrice()
            );

            for(int i = 0; i < 30; i++){
                LocalDate date = dataBegin.plusDays(i);
                BusinessDataVO bd = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                XSSFRow row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(bd.getTurnover());
                row.getCell(3).setCellValue(bd.getValidOrderCount());
                row.getCell(4).setCellValue(bd.getOrderCompletionRate());
                row.getCell(5).setCellValue(bd.getUnitPrice());
                row.getCell(6).setCellValue(bd.getNewUsers());
            }

            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            out.close();
            excel.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

}
