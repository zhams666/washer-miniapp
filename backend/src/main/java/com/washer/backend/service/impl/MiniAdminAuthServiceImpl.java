package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.washer.backend.dto.miniadmin.MiniAdminLoginRequest;
import com.washer.backend.dto.miniadmin.MiniAdminLoginResponse;
import com.washer.backend.dto.miniadmin.MiniAdminSessionContext;
import com.washer.backend.dto.miniadmin.MiniAdminStaffProfile;
import com.washer.backend.dto.miniadmin.MiniAdminStoreOption;
import com.washer.backend.entity.Franchisee;
import com.washer.backend.entity.MiniAdminStaff;
import com.washer.backend.entity.MiniAdminStaffSession;
import com.washer.backend.entity.MiniAdminStaffStore;
import com.washer.backend.entity.Store;
import com.washer.backend.mapper.FranchiseeMapper;
import com.washer.backend.mapper.MiniAdminStaffMapper;
import com.washer.backend.mapper.MiniAdminStaffSessionMapper;
import com.washer.backend.mapper.MiniAdminStaffStoreMapper;
import com.washer.backend.mapper.StoreMapper;
import com.washer.backend.service.MiniAdminAuthService;
import com.washer.backend.service.WechatMiniappAuthService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MiniAdminAuthServiceImpl implements MiniAdminAuthService {

    private static final String ROLE_PLATFORM_ADMIN = "platform_admin";
    private static final String ROLE_FRANCHISEE_OWNER = "franchisee_owner";
    private static final String ROLE_STORE_MANAGER = "store_manager";
    private static final String ROLE_STORE_STAFF = "store_staff";
    private static final String ROLE_FINANCE = "finance";
    private static final String ROLE_OPERATOR = "operator";
    private static final String SCOPE_PLATFORM = "platform";
    private static final String SCOPE_FRANCHISEE = "franchisee";

    private final WechatMiniappAuthService wechatMiniappAuthService;
    private final MiniAdminStaffMapper staffMapper;
    private final MiniAdminStaffStoreMapper staffStoreMapper;
    private final MiniAdminStaffSessionMapper sessionMapper;
    private final StoreMapper storeMapper;
    private final FranchiseeMapper franchiseeMapper;

    public MiniAdminAuthServiceImpl(
        WechatMiniappAuthService wechatMiniappAuthService,
        MiniAdminStaffMapper staffMapper,
        MiniAdminStaffStoreMapper staffStoreMapper,
        MiniAdminStaffSessionMapper sessionMapper,
        StoreMapper storeMapper,
        FranchiseeMapper franchiseeMapper
    ) {
        this.wechatMiniappAuthService = wechatMiniappAuthService;
        this.staffMapper = staffMapper;
        this.staffStoreMapper = staffStoreMapper;
        this.sessionMapper = sessionMapper;
        this.storeMapper = storeMapper;
        this.franchiseeMapper = franchiseeMapper;
    }

    @Override
    public MiniAdminLoginResponse login(MiniAdminLoginRequest request) {
        String openId = resolveOpenId(request);
        MiniAdminStaff staff = findStaffByOpenId(openId);
        if (staff == null) {
            return new MiniAdminLoginResponse(false, null, openId, "当前微信尚未绑定管理端员工账号", null);
        }
        if (!Integer.valueOf(1).equals(staff.getStatus())) {
            return new MiniAdminLoginResponse(false, null, openId, "员工账号已停用", null);
        }

        staff.setLastLoginTime(LocalDateTime.now());
        staffMapper.updateById(staff);

        String token = createSession(staff, openId);
        MiniAdminSessionContext context = buildContext(staff);
        return new MiniAdminLoginResponse(true, token, openId, "ok", toProfile(context));
    }

    @Override
    public MiniAdminStaffProfile current(String token) {
        return toProfile(requireContext(token));
    }

    @Override
    public MiniAdminSessionContext requireContext(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("管理端登录已失效，请重新登录");
        }
        MiniAdminStaffSession session = sessionMapper.selectOne(
            new LambdaQueryWrapper<MiniAdminStaffSession>()
                .eq(MiniAdminStaffSession::getToken, token.trim())
                .last("limit 1")
        );
        if (session == null || session.getExpireTime() == null || session.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("管理端登录已失效，请重新登录");
        }
        MiniAdminStaff staff = staffMapper.selectById(session.getStaffId());
        if (staff == null || !Integer.valueOf(1).equals(staff.getStatus())) {
            throw new IllegalArgumentException("员工账号不可用");
        }
        return buildContext(staff);
    }

    @Override
    public List<MiniAdminStoreOption> listAccessibleStores(String token) {
        return requireContext(token).getStores();
    }

    @Override
    public boolean canAccessStore(MiniAdminSessionContext context, Long storeId) {
        if (context == null || storeId == null) {
            return false;
        }
        if (context.isPlatformScope()) {
            return true;
        }
        return context.getStores().stream().anyMatch(store -> storeId.equals(store.getId()));
    }

    @Override
    public boolean hasPermission(MiniAdminSessionContext context, String permission) {
        if (context == null || !StringUtils.hasText(permission)) {
            return false;
        }
        return context.getPermissions().contains(permission.trim());
    }

    private String resolveOpenId(MiniAdminLoginRequest request) {
        if (request != null && StringUtils.hasText(request.getOpenId())) {
            return request.getOpenId().trim();
        }
        if (request != null && StringUtils.hasText(request.getCode())) {
            return wechatMiniappAuthService.exchangeOpenId(request.getCode());
        }
        throw new IllegalArgumentException("code is required");
    }

    private MiniAdminStaff findStaffByOpenId(String openId) {
        return staffMapper.selectOne(
            new LambdaQueryWrapper<MiniAdminStaff>()
                .eq(MiniAdminStaff::getOpenid, openId)
                .last("limit 1")
        );
    }

    private String createSession(MiniAdminStaff staff, String openId) {
        String token = "MA" + UUID.randomUUID().toString().replace("-", "");
        MiniAdminStaffSession session = new MiniAdminStaffSession();
        session.setStaffId(staff.getId());
        session.setOpenid(openId);
        session.setToken(token);
        session.setExpireTime(LocalDateTime.now().plusDays(7));
        sessionMapper.insert(session);
        return token;
    }

    private MiniAdminSessionContext buildContext(MiniAdminStaff staff) {
        boolean platformScope = isPlatformScope(staff);
        List<MiniAdminStoreOption> stores = resolveStores(staff, platformScope);
        List<String> permissions = resolvePermissions(staff.getRoleCode());
        return new MiniAdminSessionContext(staff, platformScope, stores, permissions);
    }

    private boolean isPlatformScope(MiniAdminStaff staff) {
        return ROLE_PLATFORM_ADMIN.equals(normalize(staff.getRoleCode()))
            || SCOPE_PLATFORM.equals(normalize(staff.getDataScope()));
    }

    private List<MiniAdminStoreOption> resolveStores(MiniAdminStaff staff, boolean platformScope) {
        List<Store> stores;
        if (platformScope) {
            stores = storeMapper.selectList(
                new LambdaQueryWrapper<Store>()
                    .orderByAsc(Store::getId)
            );
        } else if (SCOPE_FRANCHISEE.equals(normalize(staff.getDataScope()))
            || ROLE_FRANCHISEE_OWNER.equals(normalize(staff.getRoleCode()))) {
            stores = storeMapper.selectList(
                new LambdaQueryWrapper<Store>()
                    .eq(Store::getFranchiseeId, staff.getFranchiseeId())
                    .orderByAsc(Store::getId)
            );
        } else {
            List<Long> storeIds = staffStoreMapper.selectList(
                new LambdaQueryWrapper<MiniAdminStaffStore>()
                    .eq(MiniAdminStaffStore::getStaffId, staff.getId())
                    .orderByDesc(MiniAdminStaffStore::getIsPrimary)
                    .orderByAsc(MiniAdminStaffStore::getId)
            ).stream().map(MiniAdminStaffStore::getStoreId).distinct().toList();
            if (storeIds.isEmpty()) {
                return List.of();
            }
            stores = storeMapper.selectBatchIds(storeIds);
        }

        return stores.stream()
            .map(store -> new MiniAdminStoreOption(store.getId(), store.getFranchiseeId(), store.getStoreName()))
            .toList();
    }

    private MiniAdminStaffProfile toProfile(MiniAdminSessionContext context) {
        MiniAdminStaff staff = context.getStaff();
        Franchisee franchisee = staff.getFranchiseeId() != null ? franchiseeMapper.selectById(staff.getFranchiseeId()) : null;
        return new MiniAdminStaffProfile(
            staff.getId(),
            staff.getFranchiseeId(),
            franchisee != null ? franchisee.getFranchiseeName() : "",
            staff.getStaffNo(),
            staff.getStaffName(),
            staff.getMobile(),
            staff.getRoleCode(),
            resolveRoleName(staff.getRoleCode()),
            staff.getDataScope(),
            context.isPlatformScope(),
            context.getPermissions(),
            context.getStores()
        );
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

    private String resolveRoleName(String roleCode) {
        return switch (normalize(roleCode)) {
            case ROLE_PLATFORM_ADMIN -> "总部管理员";
            case ROLE_FRANCHISEE_OWNER -> "加盟商老板";
            case ROLE_STORE_MANAGER -> "店长";
            case ROLE_FINANCE -> "财务";
            case ROLE_OPERATOR -> "运维";
            default -> "员工";
        };
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : "";
    }
}
