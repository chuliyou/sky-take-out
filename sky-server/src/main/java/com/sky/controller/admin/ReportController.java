package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/report")
@Slf4j
@Api(tags = "报表统计")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额统计")
    public Result<TurnoverReportVO> getTurnoverStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                                          @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("报表统计：{}，{}",begin,end);
        TurnoverReportVO turnoverReportVO = reportService.getTurnoverStatistics(begin,end);
        return Result.success(turnoverReportVO);
    }


    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/userStatistics")
    @ApiOperation("用户统计")
    public Result<UserReportVO> getUserReport(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                              @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("用户统计：{},{}",begin,end);
        UserReportVO userReportVO = reportService.getUserStatistics(begin,end);
        return  Result.success(userReportVO);
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/ordersStatistics")
    @ApiOperation("订单统计")
    public Result<OrderReportVO> ordersStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                                  @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("订单统计：{},{}",begin,end);
        OrderReportVO orderReportVO = reportService.getOrderStatistics(begin,end);
        return Result.success(orderReportVO);
    }

    /**
     * top10菜品统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/top10")
    @ApiOperation("top10菜品统计")
    public Result<SalesTop10ReportVO> getTop10Sales(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                                    @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("top10菜品统计：{},{}",begin,end);
        SalesTop10ReportVO salesTop10ReportVO = reportService.getTop10Sales(begin,end);
        return Result.success(salesTop10ReportVO);
    }











}
