package com.washer.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.dto.pay.WechatPayOrderQueryResult;
import com.washer.backend.dto.pay.WechatPayPrepayRequest;
import com.washer.backend.dto.pay.WechatPayPrepayResult;
import com.washer.backend.entity.MembershipOrder;
import com.washer.backend.entity.MembershipPlan;
import com.washer.backend.entity.MembershipSetting;
import com.washer.backend.entity.PaymentTransaction;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.mapper.MembershipOrderMapper;
import com.washer.backend.mapper.MembershipPlanMapper;
import com.washer.backend.mapper.PaymentTransactionMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MembershipService {

    private final MembershipSettingService settingService;
    private final MembershipPlanMapper planMapper;
    private final MembershipOrderMapper orderMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final UserInfoService userInfoService;
    private final WechatPayService wechatPayService;

    public MembershipService(
        MembershipSettingService settingService,
        MembershipPlanMapper planMapper,
        MembershipOrderMapper orderMapper,
        PaymentTransactionMapper paymentTransactionMapper,
        UserInfoService userInfoService,
        WechatPayService wechatPayService
    ) {
        this.settingService = settingService;
        this.planMapper = planMapper;
        this.orderMapper = orderMapper;
        this.paymentTransactionMapper = paymentTransactionMapper;
        this.userInfoService = userInfoService;
        this.wechatPayService = wechatPayService;
    }

    public MembershipSetting getSettings() {
        return settingService.getSettings();
    }

    public MembershipSetting saveSettings(MembershipSetting settings) {
        return settingService.saveSettings(settings);
    }

    public List<MembershipPlan> listActivePlans() {
        ensureDefaultPlans();
        return planMapper.selectList(
            new LambdaQueryWrapper<MembershipPlan>()
                .eq(MembershipPlan::getStatus, 1)
                .orderByAsc(MembershipPlan::getSortOrder)
                .orderByAsc(MembershipPlan::getId)
        );
    }

    public List<MembershipPlan> listAllPlans() {
        ensureDefaultPlans();
        return planMapper.selectList(
            new LambdaQueryWrapper<MembershipPlan>()
                .orderByAsc(MembershipPlan::getSortOrder)
                .orderByAsc(MembershipPlan::getId)
        );
    }

    public MembershipPlan savePlan(Long id, Map<String, Object> payload) {
        MembershipPlan plan = id == null ? new MembershipPlan() : planMapper.selectById(id);
        if (id != null && plan == null) {
            throw new IllegalArgumentException("membership plan not found");
        }
        String planCode = text(payload, "planCode", "code");
        String planName = text(payload, "planName", "name");
        String planType = text(payload, "planType", "type");
        if (id == null && !StringUtils.hasText(planCode)) {
            planCode = planType;
        }
        if (!StringUtils.hasText(planCode) || !StringUtils.hasText(planName)) {
            throw new IllegalArgumentException("plan code and plan name are required");
        }
        plan.setPlanCode(planCode.trim());
        plan.setPlanName(planName.trim());
        plan.setPlanType("yearly".equalsIgnoreCase(planType) ? "yearly" : "monthly");
        plan.setDurationMonths(Math.max(1, integer(payload, "durationMonths", "duration", "yearly".equalsIgnoreCase(planType) ? 12 : 1)));
        BigDecimal price = decimal(payload, "price");
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("plan price must be positive");
        }
        plan.setPrice(price.setScale(2, RoundingMode.HALF_UP));
        plan.setBenefitText(text(payload, "benefitText", "benefit"));
        plan.setStatus(integer(payload, "status", "enabled", 1) == 1 ? 1 : 0);
        plan.setSortOrder(integer(payload, "sortOrder", "sort", 0));
        if (id == null) {
            planMapper.insert(plan);
        } else {
            planMapper.updateById(plan);
        }
        return plan;
    }

    public void disablePlan(Long id) {
        if (id == null || planMapper.selectById(id) == null) {
            throw new IllegalArgumentException("membership plan not found");
        }
        planMapper.update(
            null,
            new LambdaUpdateWrapper<MembershipPlan>()
                .eq(MembershipPlan::getId, id)
                .set(MembershipPlan::getStatus, 0)
        );
    }

    public Map<String, Object> getOverview(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("settings", getSettings());
        result.put("plans", listActivePlans());
        if (userId == null) {
            result.put("user", null);
            return result;
        }
        UserInfo user = userInfoService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        refreshMembershipStatus(user);
        Map<String, Object> userResult = new LinkedHashMap<>();
        userResult.put("userId", user.getId());
        userResult.put("isMember", user.getIsMember());
        userResult.put("memberLevel", user.getMemberLevel());
        userResult.put("memberSinceTime", user.getMemberSinceTime());
        userResult.put("memberExpireTime", user.getMemberExpireTime());
        userResult.put("points", user.getPoints() == null ? 0 : user.getPoints());
        result.put("user", userResult);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createOrder(Long userId, Long planId, String openId) {
        if (userId == null || planId == null) {
            throw new IllegalArgumentException("userId and planId are required");
        }
        UserInfo user = userInfoService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        MembershipPlan plan = requireActivePlan(planId);
        if (wechatPayService.isEnabled() && !StringUtils.hasText(openId)) {
            throw new IllegalArgumentException("wechat payer openid is required");
        }
        MembershipOrder order = new MembershipOrder();
        order.setOrderNo("MB" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        order.setUserId(userId);
        order.setPlanId(plan.getId());
        order.setPayAmount(plan.getPrice());
        order.setPayStatus("pending");
        order.setPayChannel("wxpay");
        order.setRemark("miniapp membership purchase");
        orderMapper.insert(order);

        PaymentTransaction payment = buildPayment(order, openId);
        paymentTransactionMapper.insert(payment);

        if (wechatPayService.isEnabled()) {
            WechatPayPrepayResult prepay = wechatPayService.createJsapiPrepay(
                new WechatPayPrepayRequest(
                    plan.getPlanName(),
                    payment.getOutTradeNo(),
                    plan.getPrice(),
                    openId,
                    "membership:" + order.getOrderNo()
                )
            );
            paymentTransactionMapper.update(
                null,
                new LambdaUpdateWrapper<PaymentTransaction>()
                    .eq(PaymentTransaction::getId, payment.getId())
                    .set(PaymentTransaction::getPrepayId, prepay.getPrepayId())
            );
            return buildResult(order, plan, "pending", prepay.getPayParams());
        }

        markMockPaid(order, payment, plan);
        return buildResult(order, plan, "paid", null);
    }

    public Map<String, Object> getOrderResult(String orderNo) {
        MembershipOrder order = requireOrder(orderNo);
        return buildResult(order, planMapper.selectById(order.getPlanId()), order.getPayStatus(), null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncOrder(String orderNo) {
        MembershipOrder order = requireOrder(orderNo);
        PaymentTransaction payment = findPayment(order.getOrderNo());
        if (payment == null || !"pending".equals(payment.getPayStatus()) || !wechatPayService.isEnabled()) {
            return buildResult(order, planMapper.selectById(order.getPlanId()), order.getPayStatus(), null);
        }
        WechatPayOrderQueryResult query = wechatPayService.queryOrderByOutTradeNo(payment.getOutTradeNo());
        if ("SUCCESS".equals(query.getTradeState())) {
            confirmPaymentSuccess(payment, query.getTransactionId(), query.getPayerTotal(), query.getSuccessTime(), "sync");
            order = requireOrder(orderNo);
        } else if ("CLOSED".equals(query.getTradeState()) || "REVOKED".equals(query.getTradeState()) || "PAYERROR".equals(query.getTradeState())) {
            String status = "PAYERROR".equals(query.getTradeState()) ? "failed" : "closed";
            paymentTransactionMapper.update(
                null,
                new LambdaUpdateWrapper<PaymentTransaction>()
                    .eq(PaymentTransaction::getId, payment.getId())
                    .eq(PaymentTransaction::getPayStatus, "pending")
                    .set(PaymentTransaction::getPayStatus, status)
                    .set(PaymentTransaction::getFailReason, query.getTradeState())
            );
            orderMapper.update(
                null,
                new LambdaUpdateWrapper<MembershipOrder>()
                    .eq(MembershipOrder::getId, order.getId())
                    .eq(MembershipOrder::getPayStatus, "pending")
                    .set(MembershipOrder::getPayStatus, status)
            );
            order = requireOrder(orderNo);
        }
        return buildResult(order, planMapper.selectById(order.getPlanId()), order.getPayStatus(), null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmPaymentSuccess(
        PaymentTransaction payment,
        String tradeNo,
        Integer payerTotal,
        LocalDateTime payTime,
        String source
    ) {
        if (payment == null || payment.getId() == null || !"membership".equals(payment.getBizType())) {
            throw new IllegalArgumentException("membership payment is required");
        }
        if (!amountMatches(payment.getPayAmount(), payerTotal)) {
            throw new IllegalArgumentException("wechat pay amount mismatch");
        }
        int updatedPayment = paymentTransactionMapper.update(
            null,
            new LambdaUpdateWrapper<PaymentTransaction>()
                .eq(PaymentTransaction::getId, payment.getId())
                .eq(PaymentTransaction::getPayStatus, "pending")
                .set(PaymentTransaction::getPayStatus, "paid")
                .set(PaymentTransaction::getChannelTradeNo, tradeNo)
                .set(PaymentTransaction::getPayTime, payTime != null ? payTime : LocalDateTime.now())
                .set(PaymentTransaction::getCallbackTime, LocalDateTime.now())
                .set(PaymentTransaction::getRemark, "membership payment confirmed by " + source)
        );
        if (updatedPayment == 0) {
            return;
        }
        MembershipOrder order = requireOrder(payment.getBizOrderNo());
        MembershipPlan plan = requirePlan(order.getPlanId());
        int updatedOrder = orderMapper.update(
            null,
            new LambdaUpdateWrapper<MembershipOrder>()
                .eq(MembershipOrder::getId, order.getId())
                .eq(MembershipOrder::getPayStatus, "pending")
                .set(MembershipOrder::getPayStatus, "paid")
                .set(MembershipOrder::getPayChannel, "wxpay")
                .set(MembershipOrder::getPaymentNo, payment.getPaymentNo())
                .set(MembershipOrder::getThirdPartyTradeNo, tradeNo)
                .set(MembershipOrder::getPayTime, payTime != null ? payTime : LocalDateTime.now())
        );
        if (updatedOrder > 0) {
            activateMembership(order.getUserId(), plan, order.getId());
        }
    }

    public List<MembershipOrder> listUserOrders(Long userId) {
        return orderMapper.selectList(
            new LambdaQueryWrapper<MembershipOrder>()
                .eq(MembershipOrder::getUserId, userId)
                .eq(MembershipOrder::getPayStatus, "paid")
                .orderByDesc(MembershipOrder::getPayTime)
                .orderByDesc(MembershipOrder::getId)
        );
    }

    public Page<MembershipOrder> pageOrders(long page, long size, Long userId, String status) {
        return orderMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<MembershipOrder>()
                .eq(userId != null, MembershipOrder::getUserId, userId)
                .eq(StringUtils.hasText(status), MembershipOrder::getPayStatus, status)
                .orderByDesc(MembershipOrder::getId)
        );
    }

    public void refreshMembershipStatus(UserInfo user) {
        if (user == null || !Integer.valueOf(1).equals(user.getIsMember())) {
            return;
        }
        LocalDateTime expireTime = user.getMemberExpireTime();
        if (expireTime != null && !expireTime.isAfter(LocalDateTime.now())) {
            user.setIsMember(0);
            user.setMemberLevel("normal");
            userInfoService.updateById(user);
        }
    }

    private void activateMembership(Long userId, MembershipPlan plan, Long orderId) {
        UserInfo user = userInfoService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        refreshMembershipStatus(user);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentExpire = user.getMemberExpireTime();
        LocalDateTime start = currentExpire != null && currentExpire.isAfter(now) ? currentExpire : now;
        LocalDateTime expire = start.plusMonths(Math.max(1, plan.getDurationMonths()));
        if (user.getMemberSinceTime() == null || !Integer.valueOf(1).equals(user.getIsMember())) {
            user.setMemberSinceTime(now);
        }
        user.setIsMember(1);
        user.setMemberLevel(plan.getPlanType());
        user.setMemberExpireTime(expire);
        userInfoService.updateById(user);
        orderMapper.update(
            null,
            new LambdaUpdateWrapper<MembershipOrder>()
                .eq(MembershipOrder::getId, orderId)
                .set(MembershipOrder::getMemberStartTime, start)
                .set(MembershipOrder::getMemberExpireTime, expire)
        );
    }

    private void markMockPaid(MembershipOrder order, PaymentTransaction payment, MembershipPlan plan) {
        paymentTransactionMapper.update(
            null,
            new LambdaUpdateWrapper<PaymentTransaction>()
                .eq(PaymentTransaction::getId, payment.getId())
                .set(PaymentTransaction::getPayStatus, "paid")
                .set(PaymentTransaction::getPayTime, LocalDateTime.now())
                .set(PaymentTransaction::getRemark, "membership mock payment")
        );
        orderMapper.update(
            null,
            new LambdaUpdateWrapper<MembershipOrder>()
                .eq(MembershipOrder::getId, order.getId())
                .set(MembershipOrder::getPayStatus, "paid")
                .set(MembershipOrder::getPayChannel, "miniapp_mock")
                .set(MembershipOrder::getPaymentNo, payment.getPaymentNo())
                .set(MembershipOrder::getPayTime, LocalDateTime.now())
        );
        activateMembership(order.getUserId(), plan, order.getId());
    }

    private PaymentTransaction buildPayment(MembershipOrder order, String openId) {
        PaymentTransaction payment = new PaymentTransaction();
        String paymentNo = "MP" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        payment.setPaymentNo(paymentNo);
        payment.setRequestNo(paymentNo);
        payment.setBizType("membership");
        payment.setBizOrderId(order.getId());
        payment.setBizOrderNo(order.getOrderNo());
        payment.setBizActionNo("MEMBERSHIP_" + order.getOrderNo());
        payment.setUserId(order.getUserId());
        payment.setPayChannel("wxpay");
        payment.setOutTradeNo(paymentNo);
        payment.setIdempotencyKey(paymentNo);
        payment.setPayStatus("pending");
        payment.setPayAmount(order.getPayAmount());
        payment.setCurrencyCode("CNY");
        payment.setCallbackCount(0);
        payment.setRequestTime(LocalDateTime.now());
        payment.setExpireTime(LocalDateTime.now().plusMinutes(15));
        payment.setRemark(StringUtils.hasText(openId) ? "membership payment pending" : "membership mock payment pending");
        return payment;
    }

    private Map<String, Object> buildResult(
        MembershipOrder order,
        MembershipPlan plan,
        String status,
        com.washer.backend.dto.pay.WxPayRequestPaymentParams payParams
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", order != null ? order.getOrderNo() : null);
        result.put("paymentNo", order != null ? order.getPaymentNo() : null);
        result.put("userId", order != null ? order.getUserId() : null);
        result.put("planId", order != null ? order.getPlanId() : null);
        result.put("planName", plan != null ? plan.getPlanName() : null);
        result.put("payAmount", order != null ? order.getPayAmount() : null);
        result.put("payStatus", status);
        result.put("payParams", payParams);
        result.put("memberStartTime", order != null ? order.getMemberStartTime() : null);
        result.put("memberExpireTime", order != null ? order.getMemberExpireTime() : null);
        return result;
    }

    private MembershipPlan requireActivePlan(Long planId) {
        MembershipPlan plan = requirePlan(planId);
        if (!Integer.valueOf(1).equals(plan.getStatus())) {
            throw new IllegalArgumentException("membership plan is unavailable");
        }
        return plan;
    }

    private MembershipPlan requirePlan(Long planId) {
        MembershipPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("membership plan not found");
        }
        return plan;
    }

    private MembershipOrder requireOrder(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new IllegalArgumentException("orderNo is required");
        }
        MembershipOrder order = orderMapper.selectOne(
            new LambdaQueryWrapper<MembershipOrder>()
                .eq(MembershipOrder::getOrderNo, orderNo.trim())
                .last("limit 1")
        );
        if (order == null) {
            throw new IllegalArgumentException("membership order not found");
        }
        return order;
    }

    private PaymentTransaction findPayment(String orderNo) {
        return paymentTransactionMapper.selectOne(
            new LambdaQueryWrapper<PaymentTransaction>()
                .eq(PaymentTransaction::getBizOrderNo, orderNo)
                .eq(PaymentTransaction::getBizType, "membership")
                .last("limit 1")
        );
    }

    private void ensureDefaultPlans() {
        if (planMapper.selectCount(null) > 0) {
            return;
        }
        saveDefaultPlan("monthly", "月会员", "monthly", 1, new BigDecimal("19.90"), "开通后 1 个月享受会员日优惠和会员价", 10);
        saveDefaultPlan("yearly", "年会员", "yearly", 12, new BigDecimal("199.00"), "开通后 12 个月享受会员日优惠和会员价", 20);
    }

    private void saveDefaultPlan(String code, String name, String type, int months, BigDecimal price, String benefit, int sort) {
        MembershipPlan plan = new MembershipPlan();
        plan.setPlanCode(code);
        plan.setPlanName(name);
        plan.setPlanType(type);
        plan.setDurationMonths(months);
        plan.setPrice(price);
        plan.setBenefitText(benefit);
        plan.setStatus(1);
        plan.setSortOrder(sort);
        planMapper.insert(plan);
    }

    private boolean amountMatches(BigDecimal amount, Integer payerTotal) {
        if (amount == null || payerTotal == null) {
            return false;
        }
        return amount.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValueExact() == payerTotal;
    }

    private String text(Map<String, Object> payload, String... keys) {
        if (payload == null) {
            return null;
        }
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    private Integer integer(Map<String, Object> payload, String key, String alternate, int fallback) {
        Object value = payload == null ? null : payload.get(key);
        if (value == null && alternate != null && payload != null) {
            value = payload.get(alternate);
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private BigDecimal decimal(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
