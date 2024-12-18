package com.sky.service;

import com.sky.vo.*;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

public interface ReportService {
    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);

    /**
     * top10菜品统计
     * @param begin
     * @param end
     * @return
     */
    SalesTop10ReportVO getTop10Sales(LocalDate begin, LocalDate end);

    /**
     * 导出近30天运营数据报表
     * @param httpServletResponse
     */
    void exportBusinessData(HttpServletResponse httpServletResponse);
}
