package com.washer.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.franchise.FranchiseContactCreateRequest;
import com.washer.backend.entity.FranchiseContact;
import com.washer.backend.mapper.FranchiseContactMapper;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/franchise-contacts")
public class FranchiseContactController {

    private final FranchiseContactMapper franchiseContactMapper;

    public FranchiseContactController(FranchiseContactMapper franchiseContactMapper) {
        this.franchiseContactMapper = franchiseContactMapper;
    }

    @PostMapping
    public ApiResponse<FranchiseContact> create(@Valid @RequestBody FranchiseContactCreateRequest request) {
        String contactName = request.getContactName().trim();
        String contactPhone = normalizePhone(request.getContactPhone());
        if (!StringUtils.hasText(contactPhone)) {
            return ApiResponse.fail("电话不能为空");
        }

        FranchiseContact contact = new FranchiseContact();
        contact.setContactName(contactName);
        contact.setContactPhone(contactPhone);
        contact.setSource(StringUtils.hasText(request.getSource()) ? request.getSource().trim() : "miniapp");
        contact.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : "");
        contact.setCreatedAt(LocalDateTime.now());
        contact.setUpdatedAt(LocalDateTime.now());
        franchiseContactMapper.insert(contact);
        return ApiResponse.success(contact);
    }

    @GetMapping("/admin")
    public ApiResponse<Page<FranchiseContact>> adminPage(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) String keyword
    ) {
        LambdaQueryWrapper<FranchiseContact> wrapper = new LambdaQueryWrapper<FranchiseContact>()
            .orderByDesc(FranchiseContact::getId);

        if (StringUtils.hasText(keyword)) {
            String safeKeyword = keyword.trim();
            wrapper.and(item -> item
                .like(FranchiseContact::getContactName, safeKeyword)
                .or()
                .like(FranchiseContact::getContactPhone, safeKeyword));
        }

        return ApiResponse.success(franchiseContactMapper.selectPage(new Page<>(page, size), wrapper));
    }

    private String normalizePhone(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String phone = value.replaceAll("[\\s-]", "").trim();
        if (phone.startsWith("+86")) {
            return phone.substring(3);
        }
        return phone;
    }
}
