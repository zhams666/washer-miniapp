package com.washer.backend.dto.franchise;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FranchiseContactCreateRequest {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名不能超过50个字符")
    private String contactName;

    @NotBlank(message = "电话不能为空")
    @Size(max = 30, message = "电话不能超过30个字符")
    private String contactPhone;

    @Size(max = 50, message = "来源不能超过50个字符")
    private String source;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
