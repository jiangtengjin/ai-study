package com.aistudy.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.aistudy.common.result.R;
import com.aistudy.service.LeagueRankingService;
import com.aistudy.vo.LeagueRankingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/league")
@RequiredArgsConstructor
public class LeagueRankingController {

    private final LeagueRankingService leagueRankingService;

    @GetMapping("/ranking")
    public R<LeagueRankingVO> getRanking() {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(leagueRankingService.getRanking(userId));
    }
}
