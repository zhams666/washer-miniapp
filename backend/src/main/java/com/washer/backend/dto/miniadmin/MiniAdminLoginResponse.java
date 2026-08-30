package com.washer.backend.dto.miniadmin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAdminLoginResponse {

    private Boolean bound;
    private String token;
    private String openId;
    private String message;
    private MiniAdminStaffProfile profile;
}
