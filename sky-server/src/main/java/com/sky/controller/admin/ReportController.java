package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

@RestController
@RequestMapping("/admin/report")
@Api(tags = "统计相关接口")
@Slf4j
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额统计")
    public Result<TurnoverReportVO> turnoverStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd")
                                                            LocalDate begin,
                                                       @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                            LocalDate end){

        log.info("营业额统计：{}, {}", begin, end);
        TurnoverReportVO turnoverReportVO = reportService.getTurnoverStatistcs(begin, end);
        return Result.success(turnoverReportVO);
    }

    @GetMapping("/userStatistics")
    @ApiOperation("用户统计")
    public Result<UserReportVO> userStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd")
                                                   LocalDate begin,
                                               @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                   LocalDate end){
        log.info("用户统计：{}, {}", begin, end);
        return Result.success(reportService.getUserStatistcs(begin, end));
    }

    @GetMapping("/ordersStatistics")
    @ApiOperation("订单统计")
    public Result<OrderReportVO> ordersStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd")
                                               LocalDate begin,
                                                @DateTimeFormat(pattern = "yyyy-MM-dd")
                                               LocalDate end){
        log.info("订单统计：{}, {}", begin, end);
        return Result.success(reportService.getOrderStatistcs(begin, end));
    }

    @GetMapping("/top10")
    @ApiOperation("销量排名")
    public Result<SalesTop10ReportVO> top10(@DateTimeFormat(pattern = "yyyy-MM-dd")
                                                LocalDate begin,
                                            @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                LocalDate end){
        log.info("销量排名：{}, {}", begin, end);
        return Result.success(reportService.getTop10(begin, end));
    }

    @GetMapping("/export")
    @ApiOperation("导出数据报表")
    public void export(HttpServletResponse response){
        reportService.exportBusinessData(response);
    }
}
