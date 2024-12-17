package com.sky.service.impl;

import com.google.common.collect.Lists;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.*;
import io.swagger.models.auth.In;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放begin到end范围内每天的日期
        List<LocalDate> dateList = new ArrayList();
        dateList.add(begin);
        while (!begin.equals(end)){
            //日期计算，计算当前日期后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //查询当天范围内状态为”已完成“的订单金额总数，即营业额
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("status", Orders.COMPLETED);
            map.put("beginTime",beginTime);
            map.put("endTime",endTime);

            Double turnover = orderMapper.sumByMap(map);
            if (turnover == null)turnover = 0.0;
            turnoverList.add(turnover);
        }
        //封装返回结果
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放begin到end范围内每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> newUserList  = new ArrayList<>(); //新用户集合
        List<Integer> totalUserList = new ArrayList<>(); // 总用户集合

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("endTime",endTime);

            Integer totalUser = userMapper.countUser(map);
            totalUserList.add(totalUser);

            map.put("beginTime", beginTime);
            Integer newUser = userMapper.countUser(map);
            newUserList.add(newUser);

        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .build();

    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放begin到end范围内每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //当前时间区间内的订单总数
        List<Integer> orderCountList = new ArrayList<>();
        //当前时间区间内的有效（已完成）订单总数
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 当天的订单数量
            Integer orderCount = getOrderCount(beginTime,endTime,null);
            orderCountList.add(orderCount);
            // 当天的有效订单数量
            Integer validCount = getOrderCount(beginTime,endTime,Orders.COMPLETED);
            validOrderCountList.add(validCount);
        }
        //计算时间区间内订单总数
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        //计算时间区间内有效订单总数
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        Double orderCompletionRate = 0.0 ;
        // 计算订单完成率
        if(totalOrderCount != 0 ){
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount ;
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status){
        Map map = new HashMap();
        map.put("beginTime",beginTime);
        map.put("endTime",endTime);
        map.put("status",status);

        return orderMapper.countByMap(beginTime,endTime,status);
    }


    /**
     * top10菜品统计
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getTop10Sales(LocalDate begin, LocalDate end) {

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getTop10Sales(beginTime,endTime);

        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numbers = salesTop10.stream().map((GoodsSalesDTO::getNumber)).collect(Collectors.toList());

        String nameList = StringUtils.join(names, ',');
        String numberList = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO
                .builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }
}
