package com.aistudy.controller;

import com.aistudy.common.result.R;
import com.aistudy.service.TrendStatsService;
import com.aistudy.vo.TrendStatsVO;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "学习趋势接口")
@RestController
@RequestMapping("/api/v1/trend")
@RequiredArgsConstructor
public class TrendStatsController {

    private final TrendStatsService trendStatsService;

    @Operation(summary = "获取学习趋势数据")
    @GetMapping("/stats")
    public R<List<TrendStatsVO>> getTrendStats(
            @RequestParam(defaultValue = "day") String period,
            @RequestParam(defaultValue = "7") Integer days) {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(trendStatsService.getTrendStats(userId, period, days));
    }
}
