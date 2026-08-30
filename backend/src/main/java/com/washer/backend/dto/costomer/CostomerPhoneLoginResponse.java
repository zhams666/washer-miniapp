package com.washer.backend.dto.costomer;

import com.washer.backend.entity.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CostomerPhoneLoginResponse {

    private Long userId;
    private String openId;
    private String mobile;
    private Long mergedUserId;
    private UserInfo profile;
}
