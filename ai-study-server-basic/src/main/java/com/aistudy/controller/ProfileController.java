package com.aistudy.controller;

import com.aistudy.common.result.R;
import com.aistudy.service.ProfileService;
import com.aistudy.vo.HistoryVO;
import com.aistudy.vo.ProfileStatsVO;
import com.aistudy.vo.WrongBookVO;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "个人中心接口")
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "获取学习统计")
    @GetMapping("/stats")
    public R<ProfileStatsVO> getStats() {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(profileService.getStats(userId));
    }

    @Operation(summary = "获取历史记录")
    @GetMapping("/history")
    public R<Page<HistoryVO>> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String filter) {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(profileService.getHistory(userId, page, size, filter));
    }

    @Operation(summary = "获取错题本")
    @GetMapping("/wrong-questions")
    public R<Page<WrongBookVO>> getWrongQuestions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(profileService.getWrongQuestions(userId, page, size));
    }

    @Operation(summary = "移除错题")
    @DeleteMapping("/wrong-questions/{answerId}")
    public R<Void> removeWrongQuestion(@PathVariable Long answerId) {
        Long userId = StpUtil.getLoginIdAsLong();
        profileService.removeWrongQuestion(userId, answerId);
        return R.ok();
    }
}
