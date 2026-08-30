package com.washer.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.costomer.CostomerPhoneLoginRequest;
import com.washer.backend.dto.costomer.CostomerPhoneLoginResponse;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.entity.WalletTransaction;
import com.washer.backend.mapper.WalletTransactionMapper;
import com.washer.backend.service.UserInfoService;
import com.washer.backend.service.MembershipService;
import com.washer.backend.service.WechatMiniappAuthService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/costomer")
public class CostomerController {

    private static final long MAX_AVATAR_SIZE = 3L * 1024L * 1024L;
    private static final Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final long LOGIN_CODE_EXPIRE_MINUTES = 5L;
    private static final Random LOGIN_CODE_RANDOM = new Random();
    private static final Map<String, LoginCodeRecord> LOGIN_CODE_CACHE = new ConcurrentHashMap<>();

    private final UserInfoService userInfoService;
    private final WechatMiniappAuthService wechatMiniappAuthService;
    private final WalletTransactionMapper walletTransactionMapper;
    private final JdbcTemplate jdbcTemplate;
    private final MembershipService membershipService;

    public CostomerController(
        UserInfoService userInfoService,
        WechatMiniappAuthService wechatMiniappAuthService,
        WalletTransactionMapper walletTransactionMapper,
        JdbcTemplate jdbcTemplate,
        MembershipService membershipService
    ) {
        this.userInfoService = userInfoService;
        this.wechatMiniappAuthService = wechatMiniappAuthService;
        this.walletTransactionMapper = walletTransactionMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.membershipService = membershipService;
    }

    @GetMapping("/getOpenId")
    public ApiResponse<String> getOpenId(@RequestParam String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("code is required");
        }
        return ApiResponse.success(wechatMiniappAuthService.exchangeOpenId(code.trim()));
    }

    @GetMapping("/getPhone")
    public ApiResponse<String> getPhone(
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String openId,
        @RequestParam(required = false) String openid
    ) {
        String resolvedOpenId = normalizeNullable(StringUtils.hasText(openId) ? openId : openid);
        if (!StringUtils.hasText(resolvedOpenId)) {
            return ApiResponse.success("");
        }
        UserInfo user = findUserByOpenid(resolvedOpenId);
        return ApiResponse.success(user != null && StringUtils.hasText(user.getMobile()) ? user.getMobile() : "");
    }

    @PostMapping("/mockNextLoginUser")
    public ApiResponse<Map<String, Object>> mockNextLoginUser(@RequestBody(required = false) Map<String, Object> payload) {
        String openId = getString(payload, "openId", "openid");
        String mobile = getString(payload, "mobile", "phone");
        WechatMiniappAuthService.MockLoginIdentity identity =
            wechatMiniappAuthService.prepareNextMockLogin(openId, mobile);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("openId", identity.openId());
        result.put("mobile", identity.mobile());
        result.put("nickname", identity.nickname());
        result.put("tip", "下一次微信手机号登录会使用这个测试微信身份");
        return ApiResponse.success(result);
    }

    @GetMapping("/mockLoginUsers")
    public ApiResponse<List<Map<String, Object>>> mockLoginUsers() {
        if (!wechatMiniappAuthService.isMockLoginEnabled()) {
            throw new IllegalStateException("wechat mock login is disabled");
        }
        List<Map<String, Object>> users = wechatMiniappAuthService.getBuiltinMockLoginIdentities()
            .values()
            .stream()
            .map(identity -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("openId", identity.openId());
                item.put("mobile", identity.mobile());
                item.put("nickname", identity.nickname());
                return item;
            })
            .toList();
        return ApiResponse.success(users);
    }

    @PostMapping("/phoneLogin")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<CostomerPhoneLoginResponse> phoneLogin(@RequestBody CostomerPhoneLoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        String phoneCode = normalizeNullable(request.getPhoneCode());
        if (!StringUtils.hasText(phoneCode)) {
            throw new IllegalArgumentException("phoneCode is required");
        }

        String openid = resolvePhoneLoginOpenId(request);
        String mobile = normalizeMobile(wechatMiniappAuthService.exchangePhoneNumber(phoneCode));
        UserInfo mobileUser = findUserByMobile(mobile);
        UserInfo openIdUser = findUserByOpenid(openid);

        Long mergedUserId = null;
        UserInfo loginUser;
        if (mobileUser != null && openIdUser != null && !mobileUser.getId().equals(openIdUser.getId())) {
            mergedUserId = openIdUser.getId();
            mergeUserInto(openIdUser.getId(), mobileUser.getId());
            loginUser = userInfoService.getById(mobileUser.getId());
        } else {
            loginUser = mobileUser != null ? mobileUser : openIdUser;
        }

        if (loginUser == null) {
            loginUser = new UserInfo();
            loginUser.setUserNo(buildUserNo());
            loginUser.setRegisterSource("miniapp");
            loginUser.setUserStatus(1);
            loginUser.setIsMember(0);
            loginUser.setMemberLevel("normal");
        }

        applyPhoneLoginProfile(loginUser, request, openid, mobile);
        if (loginUser.getId() == null) {
            userInfoService.save(loginUser);
        } else {
            userInfoService.updateById(loginUser);
        }

        ensureRechargeMember(loginUser);
        UserInfo profile = userInfoService.getById(loginUser.getId());
        membershipService.refreshMembershipStatus(profile);
        profile = userInfoService.getById(loginUser.getId());
        CostomerPhoneLoginResponse response = new CostomerPhoneLoginResponse(
            profile.getId(),
            profile.getOpenid(),
            profile.getMobile(),
            mergedUserId,
            profile
        );
        return ApiResponse.success(response);
    }

    @PostMapping("/sendLoginCode")
    public ApiResponse<Map<String, Object>> sendLoginCode(@RequestBody CostomerPhoneLoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        String mobile = normalizeMobile(request.getMobile());
        String verifyCode = String.format("%06d", LOGIN_CODE_RANDOM.nextInt(1_000_000));
        LOGIN_CODE_CACHE.put(mobile, new LoginCodeRecord(verifyCode, LocalDateTime.now().plusMinutes(LOGIN_CODE_EXPIRE_MINUTES)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mobile", mobile);
        result.put("expireMinutes", LOGIN_CODE_EXPIRE_MINUTES);
        // Local development does not have an SMS provider yet, so return the code for miniapp testing.
        result.put("mockCode", verifyCode);
        return ApiResponse.success("verification code sent", result);
    }

    @PostMapping("/mobileCodeLogin")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<CostomerPhoneLoginResponse> mobileCodeLogin(@RequestBody CostomerPhoneLoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        String mobile = normalizeMobile(request.getMobile());
        String verifyCode = normalizeNullable(request.getVerifyCode());
        if (!StringUtils.hasText(verifyCode)) {
            throw new IllegalArgumentException("verifyCode is required");
        }
        verifyLoginCode(mobile, verifyCode);

        String openid = resolvePhoneLoginOpenId(request);
        UserInfo mobileUser = findUserByMobile(mobile);
        UserInfo openIdUser = findUserByOpenid(openid);

        Long mergedUserId = null;
        UserInfo loginUser;
        if (mobileUser != null && openIdUser != null && !mobileUser.getId().equals(openIdUser.getId())) {
            mergedUserId = openIdUser.getId();
            mergeUserInto(openIdUser.getId(), mobileUser.getId());
            loginUser = userInfoService.getById(mobileUser.getId());
        } else {
            loginUser = mobileUser != null ? mobileUser : openIdUser;
        }

        if (loginUser == null) {
            loginUser = new UserInfo();
            loginUser.setUserNo(buildUserNo());
            loginUser.setRegisterSource("miniapp");
            loginUser.setUserStatus(1);
            loginUser.setIsMember(0);
            loginUser.setMemberLevel("normal");
        }

        applyPhoneLoginProfile(loginUser, request, openid, mobile);
        if (loginUser.getId() == null) {
            userInfoService.save(loginUser);
        } else {
            userInfoService.updateById(loginUser);
        }

        ensureRechargeMember(loginUser);
        UserInfo profile = userInfoService.getById(loginUser.getId());
        membershipService.refreshMembershipStatus(profile);
        profile = userInfoService.getById(loginUser.getId());
        CostomerPhoneLoginResponse response = new CostomerPhoneLoginResponse(
            profile.getId(),
            profile.getOpenid(),
            profile.getMobile(),
            mergedUserId,
            profile
        );
        return ApiResponse.success(response);
    }

    @PostMapping("/mobileLogin")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<CostomerPhoneLoginResponse> mobileLogin(@RequestBody CostomerPhoneLoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        String mobile = normalizeMobile(request.getMobile());
        WechatMiniappAuthService.MockLoginIdentity mockIdentity =
            wechatMiniappAuthService.findBuiltinMockLoginIdentityByMobile(mobile);
        String openid = mockIdentity != null ? mockIdentity.openId() : resolvePhoneLoginOpenId(request);
        if (mockIdentity != null && !StringUtils.hasText(request.getNickname())) {
            request.setNickname(mockIdentity.nickname());
        }
        UserInfo mobileUser = findUserByMobile(mobile);
        UserInfo openIdUser = findUserByOpenid(openid);

        Long mergedUserId = null;
        UserInfo loginUser;
        if (mobileUser != null && openIdUser != null && !mobileUser.getId().equals(openIdUser.getId())) {
            mergedUserId = openIdUser.getId();
            mergeUserInto(openIdUser.getId(), mobileUser.getId());
            loginUser = userInfoService.getById(mobileUser.getId());
        } else {
            loginUser = mobileUser != null ? mobileUser : openIdUser;
        }

        if (loginUser == null) {
            loginUser = new UserInfo();
            loginUser.setUserNo(buildUserNo());
            loginUser.setRegisterSource("miniapp");
            loginUser.setUserStatus(1);
            loginUser.setIsMember(0);
            loginUser.setMemberLevel("normal");
        }

        applyPhoneLoginProfile(loginUser, request, openid, mobile);
        if (loginUser.getId() == null) {
            userInfoService.save(loginUser);
        } else {
            userInfoService.updateById(loginUser);
        }

        ensureRechargeMember(loginUser);
        UserInfo profile = userInfoService.getById(loginUser.getId());
        membershipService.refreshMembershipStatus(profile);
        profile = userInfoService.getById(loginUser.getId());
        CostomerPhoneLoginResponse response = new CostomerPhoneLoginResponse(
            profile.getId(),
            profile.getOpenid(),
            profile.getMobile(),
            mergedUserId,
            profile
        );
        return ApiResponse.success(response);
    }

    @GetMapping("/getUserInfo")
    public ApiResponse<UserInfo> getUserInfo(
        @RequestParam(required = false) Long id,
        @RequestParam(required = false) String openid,
        @RequestParam(required = false) String openId
    ) {
        UserInfo user = null;
        if (id != null) {
            user = userInfoService.getById(id);
        } else {
            String resolvedOpenId = normalizeNullable(StringUtils.hasText(openid) ? openid : openId);
            if (StringUtils.hasText(resolvedOpenId)) {
                user = findUserByOpenid(resolvedOpenId);
            }
        }
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        ensureRechargeMember(user);
        membershipService.refreshMembershipStatus(user);
        return ApiResponse.success(user);
    }

    @GetMapping("/loginUsers")
    public ApiResponse<List<Map<String, Object>>> loginUsers(
        @RequestParam(defaultValue = "8") long size,
        @RequestParam(required = false) String keyword
    ) {
        long pageSize = Math.max(1, Math.min(size, 20));
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<UserInfo>()
            .eq(UserInfo::getUserStatus, 1)
            .orderByDesc(UserInfo::getLastLoginTime)
            .orderByDesc(UserInfo::getId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(UserInfo::getNickname, keyword)
                .or()
                .like(UserInfo::getMobile, keyword)
                .or()
                .like(UserInfo::getUserNo, keyword));
        }

        Page<UserInfo> page = userInfoService.page(new Page<>(1, pageSize), wrapper);
        List<Map<String, Object>> users = page.getRecords().stream()
            .map(this::toLoginUserItem)
            .toList();
        return ApiResponse.success(users);
    }

    @PostMapping("/saveUserInfo")
    public ApiResponse<String> saveUserInfo(@RequestBody Map<String, Object> payload) {
        String openid = normalizeNullable(getString(payload, "openid", "openId"));
        String nickname = getString(payload, "nickname", "nickName");
        String avatarUrl = getString(payload, "avatarUrl");
        String mobile = getString(payload, "mobile", "phone");
        String unionid = getString(payload, "unionid", "unionId");

        if (!StringUtils.hasText(openid)) {
            throw new IllegalArgumentException("openid is required");
        }

        UserInfo user = findUserByOpenid(openid);

        if (user == null) {
            user = new UserInfo();
            user.setUserNo(buildUserNo());
            user.setRegisterSource("miniapp");
            user.setUserStatus(1);
            user.setIsMember(0);
            user.setMemberLevel("normal");
            user.setOpenid(openid);
        }

        if (StringUtils.hasText(nickname)) {
            user.setNickname(nickname);
        } else if (!StringUtils.hasText(user.getNickname())) {
            user.setNickname("WeChat User");
        }

        if (StringUtils.hasText(avatarUrl)) {
            user.setAvatarUrl(avatarUrl);
        }

        String normalizedMobile = normalizeNullable(mobile);
        if (normalizedMobile != null) {
            user.setMobile(normalizedMobile);
        }

        String normalizedUnionId = normalizeNullable(unionid);
        if (normalizedUnionId != null) {
            user.setUnionid(normalizedUnionId);
        }

        user.setLastLoginTime(LocalDateTime.now());

        if (user.getId() == null) {
            try {
                userInfoService.save(user);
            } catch (DuplicateKeyException ex) {
                UserInfo existingUser = findUserByOpenid(openid);
                if (existingUser != null && existingUser.getId() != null) {
                    return ApiResponse.success(String.valueOf(existingUser.getId()));
                }
                throw ex;
            }
        } else {
            userInfoService.updateById(user);
        }

        return ApiResponse.success(String.valueOf(user.getId()));
    }

    @PostMapping("/uploadAvatar")
    public ApiResponse<Map<String, Object>> uploadAvatar(
        @RequestParam("file") MultipartFile file,
        HttpServletRequest request
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("avatar file is required");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("avatar file too large");
        }

        String extension = resolveAvatarExtension(file.getOriginalFilename(), file.getContentType());
        Path uploadDir = Paths.get("uploads", "avatars").toAbsolutePath().normalize();
        String filename = "avatar-" + UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = uploadDir.resolve(filename).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new IllegalArgumentException("invalid avatar file path");
        }

        try {
            Files.createDirectories(uploadDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("save avatar failed", ex);
        }

        String avatarUrl = buildPublicUrl(request, "/uploads/avatars/" + filename);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("avatarUrl", avatarUrl);
        return ApiResponse.success(data);
    }

    private UserInfo findUserByOpenid(String openid) {
        String normalizedOpenid = normalizeNullable(openid);
        if (!StringUtils.hasText(normalizedOpenid)) {
            return null;
        }
        return userInfoService.lambdaQuery()
            .eq(UserInfo::getOpenid, normalizedOpenid)
            .last("limit 1")
            .one();
    }

    private UserInfo findUserByMobile(String mobile) {
        String normalizedMobile = normalizeMobile(mobile);
        if (!StringUtils.hasText(normalizedMobile)) {
            return null;
        }
        return userInfoService.lambdaQuery()
            .eq(UserInfo::getMobile, normalizedMobile)
            .last("limit 1")
            .one();
    }

    private String resolvePhoneLoginOpenId(CostomerPhoneLoginRequest request) {
        String loginCode = normalizeNullable(request.getLoginCode());
        if (StringUtils.hasText(loginCode)) {
            return wechatMiniappAuthService.exchangeOpenId(loginCode);
        }
        String openid = normalizeNullable(
            request.getOpenId()
        );
        if (StringUtils.hasText(openid)) {
            return openid;
        }
        throw new IllegalArgumentException("loginCode is required");
    }

    private void applyPhoneLoginProfile(
        UserInfo user,
        CostomerPhoneLoginRequest request,
        String openid,
        String mobile
    ) {
        user.setOpenid(openid);
        user.setMobile(mobile);
        user.setUserStatus(1);
        if (!StringUtils.hasText(user.getRegisterSource())) {
            user.setRegisterSource("miniapp");
        }
        if (user.getIsMember() == null) {
            user.setIsMember(0);
        }
        if (!StringUtils.hasText(user.getMemberLevel())) {
            user.setMemberLevel("normal");
        }
        if (user.getPoints() == null) {
            user.setPoints(0);
        }

        String nickname = normalizeNullable(
            request.getNickname()
        );
        if (StringUtils.hasText(nickname)) {
            user.setNickname(nickname);
        } else if (!StringUtils.hasText(user.getNickname())) {
            user.setNickname("微信用户");
        }

        String avatarUrl = normalizeNullable(request.getAvatarUrl());
        if (StringUtils.hasText(avatarUrl)) {
            user.setAvatarUrl(avatarUrl);
        }

        String unionid = normalizeNullable(
            request.getUnionId()
        );
        if (StringUtils.hasText(unionid)) {
            user.setUnionid(unionid);
        }

        user.setLastLoginTime(LocalDateTime.now());
    }

    private void mergeUserInto(Long sourceUserId, Long targetUserId) {
        if (sourceUserId == null || targetUserId == null || sourceUserId.equals(targetUserId)) {
            return;
        }

        mergeWallets(sourceUserId, targetUserId);
        mergeDailyDiscountRecords(sourceUserId, targetUserId);
        for (String table : List.of(
            "user_membership_log",
            "user_vehicle",
            "wallet_transaction",
            "recharge_order",
            "card_purchase_order",
            "user_card",
            "card_usage_record",
            "wash_order",
            "wash_order_payment_detail",
            "payment_transaction",
            "store_settlement_detail",
            "mini_admin_asset_operation"
        )) {
            updateUserIdIfTableExists(table, sourceUserId, targetUserId);
        }

        jdbcTemplate.update(
            """
                UPDATE `user_info`
                SET `openid` = NULL,
                    `unionid` = NULL,
                    `mobile` = NULL,
                    `user_status` = 0,
                    `remark` = ?,
                    `updated_at` = NOW()
                WHERE `id` = ?
                """,
            "手机号登录合并至用户 " + targetUserId,
            sourceUserId
        );
    }

    private void mergeWallets(Long sourceUserId, Long targetUserId) {
        if (!tableExists("user_store_wallet")) {
            return;
        }

        jdbcTemplate.update(
            """
                UPDATE `user_store_wallet` target
                JOIN `user_store_wallet` source
                  ON source.`user_id` = ?
                 AND target.`user_id` = ?
                 AND target.`store_id` = source.`store_id`
                SET target.`principal_balance` = COALESCE(target.`principal_balance`, 0) + COALESCE(source.`principal_balance`, 0),
                    target.`available_principal_balance` = COALESCE(target.`available_principal_balance`, 0) + COALESCE(source.`available_principal_balance`, 0),
                    target.`frozen_principal_balance` = COALESCE(target.`frozen_principal_balance`, 0) + COALESCE(source.`frozen_principal_balance`, 0),
                    target.`gift_balance` = COALESCE(target.`gift_balance`, 0) + COALESCE(source.`gift_balance`, 0),
                    target.`available_gift_balance` = COALESCE(target.`available_gift_balance`, 0) + COALESCE(source.`available_gift_balance`, 0),
                    target.`frozen_gift_balance` = COALESCE(target.`frozen_gift_balance`, 0) + COALESCE(source.`frozen_gift_balance`, 0),
                    target.`total_recharge_principal` = COALESCE(target.`total_recharge_principal`, 0) + COALESCE(source.`total_recharge_principal`, 0),
                    target.`total_recharge_gift` = COALESCE(target.`total_recharge_gift`, 0) + COALESCE(source.`total_recharge_gift`, 0),
                    target.`total_consume_principal` = COALESCE(target.`total_consume_principal`, 0) + COALESCE(source.`total_consume_principal`, 0),
                    target.`total_consume_gift` = COALESCE(target.`total_consume_gift`, 0) + COALESCE(source.`total_consume_gift`, 0),
                    target.`total_refund_principal` = COALESCE(target.`total_refund_principal`, 0) + COALESCE(source.`total_refund_principal`, 0),
                    target.`status` = IF(COALESCE(target.`status`, 1) = 1 OR COALESCE(source.`status`, 1) = 1, 1, target.`status`),
                    target.`version` = COALESCE(target.`version`, 0) + 1,
                    target.`updated_at` = NOW()
                """,
            sourceUserId,
            targetUserId
        );
        jdbcTemplate.update(
            """
                DELETE source
                FROM `user_store_wallet` source
                JOIN `user_store_wallet` target
                  ON target.`user_id` = ?
                 AND target.`store_id` = source.`store_id`
                WHERE source.`user_id` = ?
                """,
            targetUserId,
            sourceUserId
        );
        jdbcTemplate.update(
            "UPDATE `user_store_wallet` SET `user_id` = ?, `updated_at` = NOW() WHERE `user_id` = ?",
            targetUserId,
            sourceUserId
        );
    }

    private void mergeDailyDiscountRecords(Long sourceUserId, Long targetUserId) {
        if (!tableExists("user_daily_discount_record")) {
            return;
        }
        jdbcTemplate.update(
            """
                DELETE source
                FROM `user_daily_discount_record` source
                JOIN `user_daily_discount_record` target
                  ON target.`user_id` = ?
                 AND target.`discount_date` = source.`discount_date`
                 AND target.`discount_type` = source.`discount_type`
                 AND COALESCE(target.`discount_scope`, '') = COALESCE(source.`discount_scope`, '')
                 AND COALESCE(target.`scope_store_id`, 0) = COALESCE(source.`scope_store_id`, 0)
                WHERE source.`user_id` = ?
                """,
            targetUserId,
            sourceUserId
        );
        jdbcTemplate.update(
            "UPDATE `user_daily_discount_record` SET `user_id` = ? WHERE `user_id` = ?",
            targetUserId,
            sourceUserId
        );
    }

    private void updateUserIdIfTableExists(String tableName, Long sourceUserId, Long targetUserId) {
        if (!tableExists(tableName)) {
            return;
        }
        jdbcTemplate.update(
            "UPDATE `" + tableName + "` SET `user_id` = ? WHERE `user_id` = ?",
            targetUserId,
            sourceUserId
        );
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """,
            Integer.class,
            tableName
        );
        return count != null && count > 0;
    }

    private void ensureRechargeMember(UserInfo user) {
        if (user == null || user.getId() == null || Integer.valueOf(1).equals(user.getIsMember())) {
            return;
        }
        Long rechargeCount = walletTransactionMapper.selectCount(
            new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getUserId, user.getId())
                .eq(WalletTransaction::getBizType, "recharge")
                .eq(WalletTransaction::getChangeType, "in")
        );
        if (rechargeCount == null || rechargeCount <= 0) {
            return;
        }
        user.setIsMember(1);
        if (!StringUtils.hasText(user.getMemberLevel())) {
            user.setMemberLevel("normal");
        }
        if (user.getMemberSinceTime() == null) {
            user.setMemberSinceTime(LocalDateTime.now());
        }
        userInfoService.updateById(user);
    }

    private String buildUserNo() {
        return "U" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String resolveAvatarExtension(String originalFilename, String contentType) {
        String filename = StringUtils.hasText(originalFilename) ? originalFilename.trim().toLowerCase() : "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0) {
            String extension = filename.substring(dotIndex);
            if (ALLOWED_AVATAR_EXTENSIONS.contains(extension)) {
                return extension;
            }
        }

        String normalizedContentType = StringUtils.hasText(contentType) ? contentType.trim().toLowerCase() : "";
        if ("image/png".equals(normalizedContentType)) {
            return ".png";
        }
        if ("image/webp".equals(normalizedContentType)) {
            return ".webp";
        }
        return ".jpg";
    }

    private String buildPublicUrl(HttpServletRequest request, String path) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String scheme = StringUtils.hasText(forwardedProto) ? forwardedProto.trim() : request.getScheme();
        String host;

        if (StringUtils.hasText(forwardedHost)) {
            host = forwardedHost.trim();
        } else {
            int port = request.getServerPort();
            boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
            host = request.getServerName() + (defaultPort ? "" : ":" + port);
        }

        return scheme + "://" + host + path;
    }

    private String getString(Map<String, Object> payload, String... keys) {
        if (payload == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            Object value = payload.get(key);
            if (value != null) {
                String text = String.valueOf(value);
                if (StringUtils.hasText(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeMobile(String value) {
        String text = normalizeNullable(value);
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("mobile is required");
        }
        String normalized = text.replaceAll("[\\s-]", "");
        if (normalized.startsWith("+86")) {
            normalized = normalized.substring(3);
        } else if (normalized.startsWith("86") && normalized.length() == 13) {
            normalized = normalized.substring(2);
        }
        if (!normalized.matches("\\d{5,20}")) {
            throw new IllegalArgumentException("mobile is invalid");
        }
        return normalized;
    }

    private void verifyLoginCode(String mobile, String verifyCode) {
        LoginCodeRecord record = LOGIN_CODE_CACHE.get(mobile);
        if (record == null) {
            throw new IllegalArgumentException("verification code not found");
        }
        if (record.expireTime().isBefore(LocalDateTime.now())) {
            LOGIN_CODE_CACHE.remove(mobile);
            throw new IllegalArgumentException("verification code expired");
        }
        if (!record.code().equals(verifyCode.trim())) {
            throw new IllegalArgumentException("verification code invalid");
        }
        LOGIN_CODE_CACHE.remove(mobile);
    }

    private Map<String, Object> toLoginUserItem(UserInfo user) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", user.getId());
        item.put("nickname", StringUtils.hasText(user.getNickname()) ? user.getNickname() : "WeChat User");
        item.put("avatarUrl", user.getAvatarUrl());
        item.put("mobile", user.getMobile());
        item.put("openid", user.getOpenid());
        item.put("userNo", user.getUserNo());
        item.put("lastLoginTime", user.getLastLoginTime());
        return item;
    }

    private record LoginCodeRecord(String code, LocalDateTime expireTime) {
    }
}
