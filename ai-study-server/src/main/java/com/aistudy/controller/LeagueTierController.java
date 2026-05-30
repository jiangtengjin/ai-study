package com.aistudy.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.aistudy.common.result.R;
import com.aistudy.service.TierService;
import com.aistudy.vo.LeagueTierVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/league")
@RequiredArgsConstructor
public class LeagueTierController {

    private final TierService tierService;

    @GetMapping("/tier")
    public R<LeagueTierVO> getTier() {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(tierService.getTierInfo(userId));
    }
}
