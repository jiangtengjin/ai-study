package com.aistudy.controller;

import com.aistudy.common.result.R;
import com.aistudy.service.ReportService;
import com.aistudy.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 获取学习报告
     */
    @GetMapping("/{sessionId}")
    public R<ReportVO> getReport(@PathVariable Long sessionId) {
        ReportVO report = reportService.generateReport(sessionId);
        return R.ok(report);
    }
}
