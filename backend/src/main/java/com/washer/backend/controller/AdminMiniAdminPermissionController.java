package com.washer.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.admin.AdminMiniAdminPermissionItem;
import com.washer.backend.dto.admin.AdminMiniAdminPermissionOptions;
import com.washer.backend.dto.admin.AdminMiniAdminPermissionRequest;
import com.washer.backend.dto.admin.AdminPermissionOption;
import com.washer.backend.dto.admin.AdminStoreOption;
import com.washer.backend.dto.miniadmin.MiniAdminStoreOption;
import com.washer.backend.entity.MiniAdminStaff;
import com.washer.backend.entity.MiniAdminStaffSession;
import com.washer.backend.entity.MiniAdminStaffStore;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.mapper.MiniAdminStaffMapper;
import com.washer.backend.mapper.MiniAdminStaffSessionMapper;
import com.washer.backend.mapper.MiniAdminStaffStoreMapper;
import com.washer.backend.mapper.StoreMapper;
import com.washer.backend.mapper.UserInfoMapper;
import com.washer.backend.service.StoreService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/mini-admin-permissions")
public class AdminMiniAdminPermissionController {

    private static final String ROLE_PLATFORM_ADMIN = "platform_admin";
    private static final String ROLE_FRANCHISEE_OWNER = "franchisee_owner";
    private static final String ROLE_STORE_MANAGER = "store_manager";
    private static final String ROLE_STORE_STAFF = "store_staff";
    private static final String ROLE_FINANCE = "finance";
    private static final String ROLE_OPERATOR = "operator";
    private static final String SCOPE_PLATFORM = "platform";
    private static final String SCOPE_FRANCHISEE = "franchisee";
    private static final String SCOPE_STORE = "store";

    private final UserInfoMapper userInfoMapper;
    private final MiniAdminStaffMapper staffMapper;
    private final MiniAdminStaffStoreMapper staffStoreMapper;
    private final MiniAdminStaffSessionMapper sessionMapper;
    private final StoreMapper storeMapper;
    private final StoreService storeService;

    public AdminMiniAdminPermissionController(
        UserInfoMapper userInfoMapper,
        MiniAdminStaffMapper staffMapper,
        MiniAdminStaffStoreMapper staffStoreMapper,
        MiniAdminStaffSessionMapper sessionMapper,
        StoreMapper storeMapper,
        StoreService storeService
    ) {
        this.userInfoMapper = userInfoMapper;
        this.staffMapper = staffMapper;
        this.staffStoreMapper = staffStoreMapper;
        this.sessionMapper = sessionMapper;
        this.storeMapper = storeMapper;
        this.storeService = storeService;
    }

    @GetMapping("/options")
    public ApiResponse<AdminMiniAdminPermissionOptions> options() {
        List<AdminPermissionOption> roles = List.of(
            new AdminPermissionOption(ROLE_PLATFORM_ADMIN, "总部管理员", "可查看全部门店和全部经营数据"),
            new AdminPermissionOption(ROLE_FRANCHISEE_OWNER, "加盟商老板", "可查看所属加盟主体下的门店"),
            new AdminPermissionOption(ROLE_STORE_MANAGER, "店长", "可管理绑定门店的设备、订单和用户资产"),
            new AdminPermissionOption(ROLE_STORE_STAFF, "门店员工", "可处理绑定门店的现场运营功能"),
            new AdminPermissionOption(ROLE_FINANCE, "财务", "可查看财务、结算和用户资产信息"),
            new AdminPermissionOption(ROLE_OPERATOR, "运维", "可查看设备并执行设备控制")
        );
        List<AdminPermissionOption> scopes = List.of(
            new AdminPermissionOption(SCOPE_PLATFORM, "全部门店", "不限制门店范围"),
            new AdminPermissionOption(SCOPE_FRANCHISEE, "加盟主体", "限制在指定加盟主体下的门店"),
            new AdminPermissionOption(SCOPE_STORE, "指定门店", "只允许访问勾选的门店")
        );
        List<AdminPermissionOption> permissions = List.of(
            new AdminPermissionOption("dashboard:view", "经营看板", "查看手机端管理首页数据"),
            new AdminPermissionOption("activity:view", "业务动态", "查看近期运营动态"),
            new AdminPermissionOption("device:view", "设备查看", "查看设备状态"),
            new AdminPermissionOption("device:control", "设备控制", "启动或停止设备"),
            new AdminPermissionOption("order:view", "订单查看", "查看订单列表"),
            new AdminPermissionOption("user:view", "用户查询", "查询用户资产"),
            new AdminPermissionOption("wallet:adjust", "余额调整", "调整用户钱包余额"),
            new AdminPermissionOption("card:adjust", "次卡调整", "调整用户次卡"),
            new AdminPermissionOption("finance:view", "财务查看", "查看经营流水"),
            new AdminPermissionOption("settlement:view", "结算查看", "查看结算数据"),
            new AdminPermissionOption("store:edit", "门店维护", "维护门店运营信息"),
            new AdminPermissionOption("staff:manage", "权限管理", "管理人员权限")
        );
        return ApiResponse.success(new AdminMiniAdminPermissionOptions(
            roles,
            scopes,
            permissions,
            storeService.getAdminStoreOptions()
        ));
    }

    @GetMapping
    public ApiResponse<AdminMiniAdminPermissionItem> detail(@RequestParam String userNo) {
        UserInfo user = requireUserByUserNo(userNo);
        MiniAdminStaff staff = findStaff(user);
        return ApiResponse.success(toItem(user, staff));
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<AdminMiniAdminPermissionItem> save(@RequestBody AdminMiniAdminPermissionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求内容不能为空");
        }
        UserInfo user = requireUserByUserNo(request.getUserNo());
        if (!StringUtils.hasText(user.getOpenid())) {
            throw new IllegalArgumentException("该用户尚未绑定微信身份，需先在小程序完成登录");
        }

        MiniAdminStaff staffByOpenId = findStaffByOpenId(user.getOpenid());
        MiniAdminStaff staffByNo = findStaffByStaffNo(user.getUserNo());
        if (staffByOpenId != null && staffByNo != null && !staffByOpenId.getId().equals(staffByNo.getId())) {
            throw new IllegalArgumentException("该用户编号和微信身份已绑定不同管理账号，请先删除其中一个权限");
        }

        MiniAdminStaff staff = staffByOpenId != null ? staffByOpenId : staffByNo;
        boolean isCreate = staff == null || staff.getId() == null;
        if (isCreate) {
            staff = new MiniAdminStaff();
            staff.setCreatedAt(LocalDateTime.now());
        }

        String roleCode = normalizeRole(request.getRoleCode());
        String dataScope = normalizeDataScope(request.getDataScope(), roleCode);
        List<Long> storeIds = normalizeStoreIds(request.getStoreIds());
        if (SCOPE_STORE.equals(dataScope) && storeIds.isEmpty()) {
            throw new IllegalArgumentException("指定门店权限至少需要选择一个门店");
        }

        staff.setFranchiseeId(request.getFranchiseeId() != null ? request.getFranchiseeId() : 1L);
        staff.setOpenid(user.getOpenid().trim());
        staff.setStaffNo(user.getUserNo().trim());
        staff.setStaffName(resolveStaffName(request.getStaffName(), user));
        staff.setMobile(StringUtils.hasText(user.getMobile()) ? user.getMobile().trim() : null);
        staff.setRoleCode(roleCode);
        staff.setDataScope(dataScope);
        staff.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        staff.setRemark(normalizeNullable(request.getRemark()));
        staff.setUpdatedAt(LocalDateTime.now());

        if (isCreate) {
            staffMapper.insert(staff);
        } else {
            staffMapper.updateById(staff);
        }

        replaceStaffStores(staff.getId(), storeIds);
        clearStaffSessions(staff.getId());
        return ApiResponse.success("管理权限已保存", toItem(user, staffMapper.selectById(staff.getId())));
    }

    @DeleteMapping
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<AdminMiniAdminPermissionItem> delete(@RequestParam String userNo) {
        UserInfo user = requireUserByUserNo(userNo);
        MiniAdminStaff staff = findStaff(user);
        if (staff != null && staff.getId() != null) {
            clearStaffStores(staff.getId());
            clearStaffSessions(staff.getId());
            staffMapper.deleteById(staff.getId());
        }
        return ApiResponse.success("管理权限已删除", toItem(user, null));
    }

    private UserInfo requireUserByUserNo(String userNo) {
        String normalizedUserNo = normalizeRequired(userNo, "用户编号不能为空");
        UserInfo user = userInfoMapper.selectOne(
            new LambdaQueryWrapper<UserInfo>()
                .eq(UserInfo::getUserNo, normalizedUserNo)
                .last("limit 1")
        );
        if (user == null) {
            throw new IllegalArgumentException("未找到该用户编号");
        }
        return user;
    }

    private MiniAdminStaff findStaff(UserInfo user) {
        if (user == null) {
            return null;
        }
        MiniAdminStaff staff = null;
        if (StringUtils.hasText(user.getOpenid())) {
            staff = findStaffByOpenId(user.getOpenid());
        }
        if (staff == null && StringUtils.hasText(user.getUserNo())) {
            staff = findStaffByStaffNo(user.getUserNo());
        }
        return staff;
    }

    private MiniAdminStaff findStaffByOpenId(String openId) {
        if (!StringUtils.hasText(openId)) {
            return null;
        }
        return staffMapper.selectOne(
            new LambdaQueryWrapper<MiniAdminStaff>()
                .eq(MiniAdminStaff::getOpenid, openId.trim())
                .last("limit 1")
        );
    }

    private MiniAdminStaff findStaffByStaffNo(String staffNo) {
        if (!StringUtils.hasText(staffNo)) {
            return null;
        }
        return staffMapper.selectOne(
            new LambdaQueryWrapper<MiniAdminStaff>()
                .eq(MiniAdminStaff::getStaffNo, staffNo.trim())
                .last("limit 1")
        );
    }

    private AdminMiniAdminPermissionItem toItem(UserInfo user, MiniAdminStaff staff) {
        boolean exists = staff != null && staff.getId() != null;
        List<Long> storeIds = exists ? loadStaffStoreIds(staff.getId()) : List.of();
        List<MiniAdminStoreOption> stores = exists ? loadStaffStores(staff, storeIds) : List.of();
        String roleCode = exists ? staff.getRoleCode() : ROLE_STORE_MANAGER;
        String dataScope = exists ? staff.getDataScope() : SCOPE_STORE;

        return new AdminMiniAdminPermissionItem(
            exists,
            user.getId(),
            user.getUserNo(),
            user.getNickname(),
            user.getMobile(),
            user.getOpenid(),
            exists ? staff.getId() : null,
            exists ? staff.getStaffNo() : user.getUserNo(),
            exists ? staff.getStaffName() : resolveStaffName(null, user),
            roleCode,
            resolveRoleName(roleCode),
            dataScope,
            resolveDataScopeName(dataScope),
            exists ? staff.getStatus() : 0,
            exists ? staff.getRemark() : "",
            storeIds,
            stores,
            resolvePermissions(roleCode),
            exists ? staff.getUpdatedAt() : null
        );
    }

    private List<Long> loadStaffStoreIds(Long staffId) {
        return staffStoreMapper.selectList(
            new LambdaQueryWrapper<MiniAdminStaffStore>()
                .eq(MiniAdminStaffStore::getStaffId, staffId)
                .orderByDesc(MiniAdminStaffStore::getIsPrimary)
                .orderByAsc(MiniAdminStaffStore::getId)
        ).stream().map(MiniAdminStaffStore::getStoreId).distinct().toList();
    }

    private List<MiniAdminStoreOption> loadStaffStores(MiniAdminStaff staff, List<Long> storeIds) {
        if (staff == null) {
            return List.of();
        }
        String dataScope = normalize(staff.getDataScope());
        List<Store> stores;
        if (SCOPE_PLATFORM.equals(dataScope)) {
            stores = storeMapper.selectList(new LambdaQueryWrapper<Store>().orderByAsc(Store::getId));
        } else if (SCOPE_FRANCHISEE.equals(dataScope)) {
            stores = storeMapper.selectList(
                new LambdaQueryWrapper<Store>()
                    .eq(Store::getFranchiseeId, staff.getFranchiseeId())
                    .orderByAsc(Store::getId)
            );
        } else if (!storeIds.isEmpty()) {
            stores = storeMapper.selectBatchIds(storeIds);
        } else {
            stores = List.of();
        }
        return stores.stream()
            .map(store -> new MiniAdminStoreOption(store.getId(), store.getFranchiseeId(), store.getStoreName()))
            .toList();
    }

    private void replaceStaffStores(Long staffId, List<Long> storeIds) {
        clearStaffStores(staffId);
        int index = 0;
        for (Long storeId : storeIds) {
            MiniAdminStaffStore item = new MiniAdminStaffStore();
            item.setStaffId(staffId);
            item.setStoreId(storeId);
            item.setIsPrimary(index == 0 ? 1 : 0);
            item.setCreatedAt(LocalDateTime.now());
            staffStoreMapper.insert(item);
            index++;
        }
    }

    private void clearStaffStores(Long staffId) {
        staffStoreMapper.delete(
            new LambdaQueryWrapper<MiniAdminStaffStore>()
                .eq(MiniAdminStaffStore::getStaffId, staffId)
        );
    }

    private void clearStaffSessions(Long staffId) {
        sessionMapper.delete(
            new LambdaQueryWrapper<MiniAdminStaffSession>()
                .eq(MiniAdminStaffSession::getStaffId, staffId)
        );
    }

    private List<Long> normalizeStoreIds(List<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        return storeIds.stream()
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();
    }

    private String resolveStaffName(String staffName, UserInfo user) {
        String text = normalizeNullable(staffName);
        if (StringUtils.hasText(text)) {
            return text;
        }
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName().trim();
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (StringUtils.hasText(user.getMobile())) {
            return user.getMobile().trim();
        }
        return user.getUserNo();
    }

    private String normalizeRole(String roleCode) {
        String role = normalize(roleCode);
        return switch (role) {
            case ROLE_PLATFORM_ADMIN, ROLE_FRANCHISEE_OWNER, ROLE_STORE_MANAGER, ROLE_STORE_STAFF, ROLE_FINANCE, ROLE_OPERATOR -> role;
            default -> ROLE_STORE_MANAGER;
        };
    }

    private String normalizeDataScope(String dataScope, String roleCode) {
        String scope = normalize(dataScope);
        if (SCOPE_PLATFORM.equals(scope) || SCOPE_FRANCHISEE.equals(scope) || SCOPE_STORE.equals(scope)) {
            return scope;
        }
        if (ROLE_PLATFORM_ADMIN.equals(roleCode)) {
            return SCOPE_PLATFORM;
        }
        if (ROLE_FRANCHISEE_OWNER.equals(roleCode)) {
            return SCOPE_FRANCHISEE;
        }
        return SCOPE_STORE;
    }

    private String resolveRoleName(String roleCode) {
        return switch (normalize(roleCode)) {
            case ROLE_PLATFORM_ADMIN -> "总部管理员";
            case ROLE_FRANCHISEE_OWNER -> "加盟商老板";
            case ROLE_STORE_MANAGER -> "店长";
            case ROLE_FINANCE -> "财务";
            case ROLE_OPERATOR -> "运维";
            default -> "门店员工";
        };
    }

    private String resolveDataScopeName(String dataScope) {
        return switch (normalize(dataScope)) {
            case SCOPE_PLATFORM -> "全部门店";
            case SCOPE_FRANCHISEE -> "加盟主体";
            default -> "指定门店";
        };
    }

    private List<String> resolvePermissions(String roleCode) {
        String role = normalize(roleCode);
        Set<String> permissions = new LinkedHashSet<>();
        permissions.add("dashboard:view");
        permissions.add("device:view");
        permissions.add("order:view");
        permissions.add("activity:view");

        if (ROLE_PLATFORM_ADMIN.equals(role) || ROLE_FRANCHISEE_OWNER.equals(role)) {
            permissions.addAll(List.of(
                "device:control",
                "user:view",
                "wallet:adjust",
                "card:adjust",
                "finance:view",
                "settlement:view",
                "store:edit",
                "staff:manage"
            ));
        } else if (ROLE_STORE_MANAGER.equals(role)) {
            permissions.addAll(List.of(
                "device:control",
                "user:view",
                "wallet:adjust",
                "card:adjust",
                "finance:view",
                "store:edit"
            ));
        } else if (ROLE_STORE_STAFF.equals(role)) {
            permissions.addAll(List.of(
                "device:control",
                "user:view",
                "wallet:adjust",
                "card:adjust"
            ));
        } else if (ROLE_FINANCE.equals(role)) {
            permissions.addAll(List.of("user:view", "finance:view", "settlement:view"));
        } else if (ROLE_OPERATOR.equals(role)) {
            permissions.add("device:control");
            permissions.add("user:view");
        }
        return new ArrayList<>(permissions);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeNullable(value);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : "";
    }
}
