package com.aistudy.task;

import com.aistudy.service.LeagueGroupService;
import com.aistudy.service.LeagueSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeagueScheduledTask {

    private final LeagueGroupService leagueGroupService;
    private final LeagueSettlementService leagueSettlementService;

    /**
     * 周一 00:10 自动分组（在周日 23:50 结算之后，留出缓冲时间）
     */
    @Scheduled(cron = "0 10 0 * * MON")
    public void weeklyGrouping() {
        log.info("定时任务：开始周联赛分组");
        try {
            leagueGroupService.executeWeeklyGrouping();
        } catch (Exception e) {
            log.error("周联赛分组失败", e);
        }
    }

    /**
     * 周日 23:50 自动结算
     */
    @Scheduled(cron = "0 50 23 * * SUN")
    public void weeklySettlement() {
        log.info("定时任务：开始周联赛结算");
        try {
            leagueSettlementService.executeSettlement();
        } catch (Exception e) {
            log.error("周联赛结算失败", e);
        }
    }
}
