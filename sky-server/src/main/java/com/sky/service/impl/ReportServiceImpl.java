package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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
    @Autowired
    private WorkspaceService workspaceService;

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

        return orderMapper.countByMap(map);
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

    /**
     * 导出近30天的运营数据报表
     * @param httpServletResponse
     */
    public void exportBusinessData(HttpServletResponse httpServletResponse) {
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);

        //查询概览运营数据，提供给Excel模板文件
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/template.xlsx");

        try {
            //基于提供好的模板文件创建一个新的Excel表对象
            XSSFWorkbook excel = new XSSFWorkbook(inputStream);
            //获取excel表中的一个sheet对象
            XSSFSheet sheet = excel.getSheet("Sheet1");
            //填入时间范围
            sheet.getRow(1).getCell(1).setCellValue(begin + "至" + end);
            //填入概览数据
            sheet.getRow(3).getCell(2).setCellValue(businessDataVO.getTurnover());
            sheet.getRow(3).getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            sheet.getRow(3).getCell(6).setCellValue(businessDataVO.getNewUsers());
            sheet.getRow(4).getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            sheet.getRow(4).getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填入明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                //获取明细数据
                LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
                LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
                BusinessDataVO businessData = workspaceService.getBusinessData(beginTime,endTime);
//                businessData = workspaceService.getBusinessData(LocalDateTime.of(date,LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                sheet.getRow(7+i).getCell(1).setCellValue(date.toString());
                sheet.getRow(7+i).getCell(2).setCellValue(businessData.getTurnover());
                sheet.getRow(7+i).getCell(3).setCellValue(businessData.getValidOrderCount());
                sheet.getRow(7+i).getCell(4).setCellValue(businessData.getOrderCompletionRate());
                sheet.getRow(7+i).getCell(5).setCellValue(businessData.getUnitPrice());
                sheet.getRow(7+i).getCell(6).setCellValue(businessData.getNewUsers());
            }

            //通过输出流将文件下载到客户端浏览器中
            ServletOutputStream outputStream = httpServletResponse.getOutputStream();
            excel.write(outputStream);

            //关闭资源
            outputStream.flush();
            outputStream.close();
            excel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





//    /**导出近30天的运营数据报表
//     * @param httpServletResponse
//     **/
//    public void exportBusinessData(HttpServletResponse httpServletResponse) {
//        LocalDate begin = LocalDate.now().minusDays(30);
//        LocalDate end = LocalDate.now().minusDays(1);
//        //查询概览运营数据，提供给Excel模板文件
//        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(begin,LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));
//        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/template.xlsx");
//        try {
//            //基于提供好的模板文件创建一个新的Excel表格对象
//            XSSFWorkbook excel = new XSSFWorkbook(inputStream);
//            //获得Excel文件中的一个Sheet页
//            XSSFSheet sheet = excel.getSheet("Sheet1");
//
//            sheet.getRow(1).getCell(1).setCellValue(begin + "至" + end);
//            //获得第4行
//            XSSFRow row = sheet.getRow(3);
//            //获取单元格
//            row.getCell(2).setCellValue(businessData.getTurnover());
//            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
//            row.getCell(6).setCellValue(businessData.getNewUsers());
//            row = sheet.getRow(4);
//            row.getCell(2).setCellValue(businessData.getValidOrderCount());
//            row.getCell(4).setCellValue(businessData.getUnitPrice());
//            for (int i = 0; i < 30; i++) {
//                LocalDate date = begin.plusDays(i);
//                //准备明细数据
//                businessData = workspaceService.getBusinessData(LocalDateTime.of(date,LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
//                row = sheet.getRow(7 + i);
//                row.getCell(1).setCellValue(date.toString());
//                row.getCell(2).setCellValue(businessData.getTurnover());
//                row.getCell(3).setCellValue(businessData.getValidOrderCount());
//                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
//                row.getCell(5).setCellValue(businessData.getUnitPrice());
//                row.getCell(6).setCellValue(businessData.getNewUsers());
//            }
//            //通过输出流将文件下载到客户端浏览器中
//            ServletOutputStream out = httpServletResponse.getOutputStream();
//            excel.write(out);
//            //关闭资源
//            out.flush();
//            out.close();
//            excel.close();
//
//        }catch (IOException e){
//            e.printStackTrace();
//        }
//    }

















}
