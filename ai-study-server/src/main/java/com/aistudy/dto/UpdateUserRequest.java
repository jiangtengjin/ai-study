package com.aistudy.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;

    @Size(max = 500, message = "头像URL过长")
    private String avatar;
}
