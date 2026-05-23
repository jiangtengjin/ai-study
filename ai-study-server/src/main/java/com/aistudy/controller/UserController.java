package com.aistudy.controller;

import com.aistudy.common.result.R;
import com.aistudy.dto.ChangePasswordRequest;
import com.aistudy.dto.UpdateUserRequest;
import com.aistudy.entity.User;
import com.aistudy.service.UserService;
import com.aistudy.vo.UserVO;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户接口")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public R<UserVO> getUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.findById(userId);
        if (user == null) {
            return R.fail(404, "用户不存在");
        }

        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return R.ok(vo);
    }

    @Operation(summary = "更新用户昵称")
    @PutMapping("/info")
    public R<Void> updateUserInfo(@Valid @RequestBody UpdateUserRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.findById(userId);
        if (user == null) {
            return R.fail(404, "用户不存在");
        }

        if (request.getNickname() != null && !request.getNickname().isEmpty()) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            user.setAvatar(request.getAvatar());
        }

        userService.update(user);
        return R.ok();
    }

    @Operation(summary = "修改密码")
    @PostMapping("/change-password")
    public R<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (request.getOldPassword() == null || request.getOldPassword().isEmpty()) {
            return R.fail(400, "请输入旧密码");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            return R.fail(400, "新密码长度不能少于6位");
        }
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return R.ok();
    }
}
