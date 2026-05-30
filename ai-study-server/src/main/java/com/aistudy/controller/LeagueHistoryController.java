package com.aistudy.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.aistudy.common.result.R;
import com.aistudy.service.LeagueSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/league")
@RequiredArgsConstructor
public class LeagueHistoryController {

    private final LeagueSettlementService leagueSettlementService;

    @GetMapping("/history")
    public R<List<LeagueSettlementService.LeagueHistoryVO>> getHistory(
            @RequestParam(defaultValue = "12") int weeks) {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(leagueSettlementService.getHistory(userId, weeks));
    }
}
