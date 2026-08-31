package com.washer.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.config.CommerceProperties;
import com.washer.backend.integration.commerce.CardPaymentGateway;
import com.washer.backend.integration.commerce.CardPaymentResult;
import com.washer.backend.integration.commerce.VoucherVerificationGateway;
import com.washer.backend.integration.commerce.VoucherVerificationResult;
import com.washer.backend.entity.CardProduct;
import com.washer.backend.entity.CardPurchaseOrder;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.UserCard;
import com.washer.backend.mapper.CardProductMapper;
import com.washer.backend.mapper.CardPurchaseOrderMapper;
import com.washer.backend.mapper.UserCardMapper;
import com.washer.backend.service.StoreService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards")
public class MiniCardController {

    private static final String NEW_USER_TRIAL_CARD_TYPE = "new_user_trial";
    private static final String MONTHLY_CARD_TYPE = "monthly";
    private static final BigDecimal VIP_CARD_DISCOUNT_RATE = new BigDecimal("0.85");
    private static final Pattern STORE_ID_PATTERN = Pattern.compile("(?i)(?:STORE|S)(\\d+)");
    private static final Pattern TIMES_PATTERN = Pattern.compile("(?i)(\\d+)(?:C|T|次)");
    private static final List<DefaultCardProductTemplate> DEFAULT_STORE_CARD_PRODUCTS = List.of(
        new DefaultCardProductTemplate("新人体验次卡", NEW_USER_TRIAL_CARD_TYPE, 1, new BigDecimal("5.00"), 30, "新人专享", 1, 1),
        new DefaultCardProductTemplate("VIP月卡", MONTHLY_CARD_TYPE, 1, new BigDecimal("19.90"), 30, "VIP", 0, 0),
        new DefaultCardProductTemplate("单次体验卡", "store", 1, new BigDecimal("16.80"), 180, "体验", 0, 0),
        new DefaultCardProductTemplate("三次畅洗卡", "store", 3, new BigDecimal("45.00"), 180, "热卖", 0, 0),
        new DefaultCardProductTemplate("五次省心卡", "store", 5, new BigDecimal("68.00"), 180, "划算", 0, 0)
    );

    private final UserCardMapper userCardMapper;
    private final CardProductMapper cardProductMapper;
    private final CardPurchaseOrderMapper cardPurchaseOrderMapper;
    private final StoreService storeService;
    private final CommerceProperties commerceProperties;
    private final CardPaymentGateway cardPaymentGateway;
    private final VoucherVerificationGateway voucherVerificationGateway;

    public MiniCardController(
        UserCardMapper userCardMapper,
        CardProductMapper cardProductMapper,
        CardPurchaseOrderMapper cardPurchaseOrderMapper,
        StoreService storeService,
        CommerceProperties commerceProperties,
        CardPaymentGateway cardPaymentGateway,
        VoucherVerificationGateway voucherVerificationGateway
    ) {
        this.userCardMapper = userCardMapper;
        this.cardProductMapper = cardProductMapper;
        this.cardPurchaseOrderMapper = cardPurchaseOrderMapper;
        this.storeService = storeService;
        this.commerceProperties = commerceProperties;
        this.cardPaymentGateway = cardPaymentGateway;
        this.voucherVerificationGateway = voucherVerificationGateway;
    }

    @GetMapping("/products")
    public ApiResponse<List<Map<String, Object>>> products(
        @RequestParam Long storeId,
        @RequestParam(required = false) Long userId
    ) {
        Store store = requireStore(storeId);
        List<CardProduct> products = ensureDefaultStoreProducts(store.getId());
        return ApiResponse.success(products.stream().map(product -> toProductResult(product, userId)).toList());
    }

    @GetMapping("/my")
    public ApiResponse<List<Map<String, Object>>> myCards(@RequestParam Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        List<UserCard> cards = userCardMapper.selectList(
            new LambdaQueryWrapper<UserCard>()
                .eq(UserCard::getUserId, userId)
                .orderByDesc(UserCard::getId)
        );
        if (cards.isEmpty()) {
            return ApiResponse.success(List.of());
        }

        Set<Long> storeIds = cards.stream()
            .map(UserCard::getStoreId)
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toSet());
        Map<Long, Store> storeMap = storeIds.isEmpty()
            ? Map.of()
            : storeService.listByIds(storeIds).stream()
                .collect(Collectors.toMap(Store::getId, store -> store, (left, right) -> left));

        LocalDateTime now = LocalDateTime.now();
        return ApiResponse.success(cards.stream()
            .map(card -> toCardListItem(card, storeMap.get(card.getStoreId()), now))
            .toList());
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(@RequestParam Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        Long totalCount = userCardMapper.selectCount(
            new LambdaQueryWrapper<UserCard>()
                .eq(UserCard::getUserId, userId)
        );

        LocalDateTime now = LocalDateTime.now();
        List<UserCard> availableCandidates = userCardMapper.selectList(
            new LambdaQueryWrapper<UserCard>()
            .eq(UserCard::getUserId, userId)
            .eq(UserCard::getStatus, "active")
                .gt(UserCard::getRemainingTimes, 0)
        );
        List<UserCard> availableCards = availableCandidates.stream()
            .filter(card -> isAvailableCard(card, now))
            .toList();
        long availableRows = availableCards.size();
        Integer remainingTimes = availableCards.stream()
            .map(UserCard::getRemainingTimes)
            .filter(times -> times != null && times > 0)
            .reduce(0, Integer::sum);

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", totalCount != null ? totalCount : 0);
        result.put("availableCount", remainingTimes);
        result.put("availableCardRows", availableRows);
        result.put("remainingTimes", remainingTimes);
        return ApiResponse.success(result);
    }

    @PostMapping("/purchase")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Map<String, Object>> purchase(@RequestBody Map<String, Object> payload) {
        if (!commerceProperties.isSimulationEnabled()) {
            return ApiResponse.success("payment pending", createProviderPurchase(payload));
        }
        Long userId = parseLong(payload.get("userId"));
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        Long storeId = parseLong(payload.get("storeId"));
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }
        Store store = requireStore(storeId);

        Long productId = parseLong(payload.get("productId"));
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }
        CardProduct product = getRequiredActiveProduct(productId, storeId);
        ensureProductPurchaseAllowed(product, userId);

        BigDecimal effectiveSalePrice = resolveEffectiveSalePrice(product, userId);
        CardPurchaseOrder purchaseOrder = createMockPurchaseOrder(userId, storeId, product, effectiveSalePrice);
        return ApiResponse.success("simulation card purchased", issuePaidPurchase(purchaseOrder, product, store, effectiveSalePrice));
    }

    @PostMapping("/purchase-orders/{purchaseOrderNo}/confirm")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Map<String, Object>> confirmProviderPurchase(
        @PathVariable String purchaseOrderNo, @RequestBody Map<String, Object> payload
    ) {
        CardPurchaseOrder order = cardPurchaseOrderMapper.selectOne(new LambdaQueryWrapper<CardPurchaseOrder>()
            .eq(CardPurchaseOrder::getPurchaseOrderNo, purchaseOrderNo).last("limit 1"));
        if (order == null) throw new IllegalArgumentException("card purchase order not found");
        if ("paid".equals(order.getPayStatus())) return ApiResponse.success("already paid", Map.of("purchaseOrderNo", order.getPurchaseOrderNo(), "payStatus", "paid"));
        CardPaymentResult verified = cardPaymentGateway.verifyCallback(order, payload);
        if (!"paid".equalsIgnoreCase(verified.status())) throw new IllegalArgumentException(verified.message());
        CardProduct product = getRequiredActiveProduct(order.getCardProductId(), order.getStoreId());
        Store store = requireStore(order.getStoreId());
        order.setPayStatus("paid");
        order.setExternalOrderNo(verified.providerOrderNo());
        order.setPurchaseTime(LocalDateTime.now());
        order.setRemark(verified.message());
        cardPurchaseOrderMapper.updateById(order);
        return ApiResponse.success("paid", issuePaidPurchase(order, product, store, order.getPayAmount()));
    }

    @PostMapping("/voucher-redeem")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Map<String, Object>> redeemVoucher(@RequestBody Map<String, Object> payload) {
        if (commerceProperties.isSimulationEnabled()) {
            return redeemVerifiedVoucher(payload);
        }
        String voucherCode = normalizeVoucherCode(getString(payload, "voucherCode", "code"));
        VoucherVerificationResult verified = voucherVerificationGateway.verify(
            voucherCode, parseLong(payload.get("storeId")), getString(payload, "sourceChannel", "platform")
        );
        if (!verified.verified()) {
            throw new IllegalArgumentException(verified.message());
        }
        Map<String, Object> verifiedPayload = new HashMap<>(payload);
        verifiedPayload.put("storeId", verified.storeId());
        verifiedPayload.put("totalTimes", verified.totalTimes());
        verifiedPayload.put("sourceChannel", verified.sourceChannel());
        verifiedPayload.put("voucherCode", verified.externalOrderNo());
        return redeemVerifiedVoucher(verifiedPayload);
    }

    /** Retained only for local test scripts; never expose this route outside a simulation environment. */
    @PostMapping("/internal/simulated-redeem")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Map<String, Object>> redeemVoucherInSimulation(@RequestBody Map<String, Object> payload) {
        requireSimulation("券码核销");
        return redeemVerifiedVoucher(payload);
    }

    private ApiResponse<Map<String, Object>> redeemVerifiedVoucher(Map<String, Object> payload) {
        Long userId = parseLong(payload.get("userId"));
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        String voucherCode = normalizeVoucherCode(getString(payload, "voucherCode", "code"));
        if (!StringUtils.hasText(voucherCode)) {
            throw new IllegalArgumentException("voucherCode is required");
        }

        UserCard redeemed = userCardMapper.selectOne(
            new LambdaQueryWrapper<UserCard>()
                .eq(UserCard::getExternalOrderNo, voucherCode)
                .last("limit 1")
        );
        if (redeemed != null) {
            throw new IllegalArgumentException("voucher code already redeemed");
        }

        Long storeId = parseLong(payload.get("storeId"));
        if (storeId == null) {
            storeId = resolveStoreIdFromVoucherCode(voucherCode);
        }
        Store store = requireStore(storeId);

        String sourceChannel = normalizeSourceChannel(getString(payload, "sourceChannel", "platform"));
        if (!StringUtils.hasText(sourceChannel)) {
            sourceChannel = resolveVoucherSource(voucherCode);
        }
        Integer totalTimes = parseInteger(payload.get("totalTimes"));
        if (totalTimes == null || totalTimes <= 0) {
            totalTimes = resolveTimesFromVoucherCode(voucherCode);
        }

        List<UserCard> cards = createSingleUseCards(
            userId,
            storeId,
            900000L + totalTimes,
            sourceChannel,
            sourceChannel,
            totalTimes,
            180,
            voucherCode,
            sourceChannel + " mock voucher redeem"
        );

        return ApiResponse.success("voucher redeemed", toBatchCardResult(cards, store, sourceChannel));
    }

    private Map<String, Object> createProviderPurchase(Map<String, Object> payload) {
        Long userId = parseLong(payload.get("userId"));
        Long storeId = parseLong(payload.get("storeId"));
        Long productId = parseLong(payload.get("productId"));
        if (userId == null || storeId == null || productId == null) {
            throw new IllegalArgumentException("userId, storeId and productId are required");
        }
        requireStore(storeId);
        CardProduct product = getRequiredActiveProduct(productId, storeId);
        ensureProductPurchaseAllowed(product, userId);
        BigDecimal price = resolveEffectiveSalePrice(product, userId);
        CardPurchaseOrder order = new CardPurchaseOrder();
        order.setPurchaseOrderNo("CP" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        order.setUserId(userId);
        order.setStoreId(storeId);
        order.setCardProductId(productId);
        order.setCardType(normalizeCardType(product.getCardType()));
        order.setSourceChannel("store");
        order.setBuyCount(resolvePurchaseCardCount(product));
        order.setPayAmount(price);
        order.setPayStatus("pending");
        order.setRemark("waiting for provider payment confirmation");
        cardPurchaseOrderMapper.insert(order);
        CardPaymentResult payment = cardPaymentGateway.createPayment(order);
        order.setExternalOrderNo(payment.providerOrderNo());
        order.setRemark(payment.message());
        cardPurchaseOrderMapper.updateById(order);
        return Map.of(
            "purchaseOrderNo", order.getPurchaseOrderNo(), "payStatus", payment.status(),
            "providerOrderNo", payment.providerOrderNo() == null ? "" : payment.providerOrderNo(),
            "message", payment.message(), "salePrice", price
        );
    }

    private Map<String, Object> issuePaidPurchase(
        CardPurchaseOrder purchaseOrder, CardProduct product, Store store, BigDecimal salePrice
    ) {
        boolean monthlyProduct = isMonthlyCardProduct(product);
        List<UserCard> cards = monthlyProduct
            ? List.of(createMonthlyCard(purchaseOrder.getUserId(), purchaseOrder.getStoreId(), product.getId(), product.getValidDays(),
                buildRemark(product.getCardName(), salePrice, "card purchase")))
            : createSingleUseCards(purchaseOrder.getUserId(), purchaseOrder.getStoreId(), product.getId(),
                normalizeCardType(product.getCardType()), "store", product.getTotalTimes(), product.getValidDays(),
                purchaseOrder.getPurchaseOrderNo(), buildRemark(product.getCardName(), salePrice, "card purchase"));
        Map<String, Object> result = monthlyProduct
            ? toMonthlyCardPurchaseResult(cards.get(0), store)
            : toBatchCardResult(cards, store, "store");
        result.put("purchaseOrderId", purchaseOrder.getId());
        result.put("purchaseOrderNo", purchaseOrder.getPurchaseOrderNo());
        result.put("payStatus", "paid");
        result.put("productId", product.getId());
        result.put("cardName", product.getCardName());
        result.put("salePrice", salePrice);
        return result;
    }

    private void requireSimulation(String feature) {
        if (!commerceProperties.isSimulationEnabled()) {
            throw new IllegalStateException(feature + "支付/核验适配器未配置；请实现 CardPaymentGateway 或 VoucherVerificationGateway 后再启用 provider 模式");
        }
    }

    private List<CardProduct> findActiveProducts(Long storeId) {
        if (storeId == null) {
            return List.of();
        }
        return cardProductMapper.selectList(
            new LambdaQueryWrapper<CardProduct>()
                .eq(CardProduct::getStoreId, storeId)
                .eq(CardProduct::getStatus, 1)
                .orderByAsc(CardProduct::getSalePrice)
                .orderByAsc(CardProduct::getId)
        );
    }

    private List<CardProduct> ensureDefaultStoreProducts(Long storeId) {
        List<CardProduct> existing = cardProductMapper.selectList(
            new LambdaQueryWrapper<CardProduct>()
                .eq(CardProduct::getStoreId, storeId)
        );
        List<CardProduct> products = new ArrayList<>();
        boolean hasExistingProducts = !existing.isEmpty();
        for (DefaultCardProductTemplate template : DEFAULT_STORE_CARD_PRODUCTS) {
            if (hasExistingProducts && !shouldEnsureDefaultProduct(template)) {
                continue;
            }
            if (hasDefaultProduct(existing, template)) {
                continue;
            }
            CardProduct product = new CardProduct();
            product.setStoreId(storeId);
            product.setCardName(template.cardName());
            product.setCardType(template.cardType());
            product.setTotalTimes(template.totalTimes());
            product.setSalePrice(template.salePrice());
            product.setValidDays(template.validDays());
            product.setIsNewUserOnly(template.isNewUserOnly());
            product.setPurchaseLimit(template.purchaseLimit());
            product.setStatus(1);
            product.setRemark(template.tag());
            cardProductMapper.insert(product);
            products.add(product);
        }
        return findActiveProducts(storeId);
    }

    private boolean shouldEnsureDefaultProduct(DefaultCardProductTemplate template) {
        String cardType = normalizeCardType(template.cardType());
        return NEW_USER_TRIAL_CARD_TYPE.equals(cardType) || MONTHLY_CARD_TYPE.equals(cardType);
    }

    private boolean hasDefaultProduct(List<CardProduct> products, DefaultCardProductTemplate template) {
        if (products == null || products.isEmpty()) {
            return false;
        }
        return products.stream().anyMatch(product -> {
            String templateType = normalizeCardType(template.cardType());
            if (NEW_USER_TRIAL_CARD_TYPE.equals(templateType) || MONTHLY_CARD_TYPE.equals(templateType)) {
                return templateType.equals(normalizeCardType(product.getCardType()));
            }
            return template.cardName().equals(product.getCardName());
        });
    }

    private CardProduct getRequiredActiveProduct(Long productId, Long storeId) {
        CardProduct product = cardProductMapper.selectOne(
            new LambdaQueryWrapper<CardProduct>()
                .eq(CardProduct::getId, productId)
                .eq(CardProduct::getStoreId, storeId)
                .eq(CardProduct::getStatus, 1)
                .last("limit 1")
        );
        if (product == null) {
            throw new IllegalArgumentException("card product not found or off shelf");
        }
        if (!StringUtils.hasText(product.getCardType())) {
            throw new IllegalArgumentException("card product cardType is invalid");
        }
        if (product.getTotalTimes() == null || product.getTotalTimes() <= 0) {
            throw new IllegalArgumentException("card product totalTimes is invalid");
        }
        if (product.getSalePrice() == null || product.getSalePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("card product salePrice is invalid");
        }
        if (product.getValidDays() != null && product.getValidDays() <= 0) {
            throw new IllegalArgumentException("card product validDays is invalid");
        }
        return product;
    }

    private void ensureProductPurchaseAllowed(CardProduct product, Long userId) {
        if (isNewUserTrialProduct(product)) {
            ensureNewUserTrialPurchaseAllowed(product, userId);
            return;
        }

        if (isMonthlyCardProduct(product)) {
            if (hasActiveMonthlyCard(userId, product.getStoreId())) {
                throw new IllegalArgumentException("已有有效VIP月卡");
            }
            return;
        }

        if (Integer.valueOf(1).equals(product.getIsNewUserOnly())) {
            if (hasAnyCardHistory(userId)) {
                throw new IllegalArgumentException("card product is new user only");
            }
        }

        Integer purchaseLimit = product.getPurchaseLimit();
        if (purchaseLimit == null || purchaseLimit <= 0) {
            return;
        }

        Long paidCount = cardPurchaseOrderMapper.selectCount(
            new LambdaQueryWrapper<CardPurchaseOrder>()
                .eq(CardPurchaseOrder::getUserId, userId)
                .eq(CardPurchaseOrder::getStoreId, product.getStoreId())
                .eq(CardPurchaseOrder::getCardProductId, product.getId())
                .eq(CardPurchaseOrder::getPayStatus, "paid")
        );
        if (paidCount != null && paidCount >= purchaseLimit) {
            throw new IllegalArgumentException("card product purchase limit exceeded");
        }
    }

    private void ensureNewUserTrialPurchaseAllowed(CardProduct product, Long userId) {
        if (hasPurchasedCardType(userId, NEW_USER_TRIAL_CARD_TYPE) || hasUserCardType(userId, NEW_USER_TRIAL_CARD_TYPE)) {
            throw new IllegalArgumentException("新用户体验次卡全平台限购 1 次");
        }
        if (hasAnyCardHistory(userId)) {
            throw new IllegalArgumentException("新用户体验次卡仅限未购卡用户购买");
        }

        Integer purchaseLimit = product.getPurchaseLimit();
        if (purchaseLimit != null && purchaseLimit > 0) {
            Long paidCount = cardPurchaseOrderMapper.selectCount(
                new LambdaQueryWrapper<CardPurchaseOrder>()
                    .eq(CardPurchaseOrder::getUserId, userId)
                    .eq(CardPurchaseOrder::getCardType, NEW_USER_TRIAL_CARD_TYPE)
                    .eq(CardPurchaseOrder::getPayStatus, "paid")
            );
            if (paidCount != null && paidCount >= purchaseLimit) {
                throw new IllegalArgumentException("新用户体验次卡全平台限购 1 次");
            }
        }
    }

    private boolean hasAnyCardHistory(Long userId) {
        Long cardCount = userCardMapper.selectCount(
            new LambdaQueryWrapper<UserCard>()
                .eq(UserCard::getUserId, userId)
        );
        if (cardCount != null && cardCount > 0) {
            return true;
        }

        Long paidOrderCount = cardPurchaseOrderMapper.selectCount(
            new LambdaQueryWrapper<CardPurchaseOrder>()
                .eq(CardPurchaseOrder::getUserId, userId)
                .eq(CardPurchaseOrder::getPayStatus, "paid")
        );
        return paidOrderCount != null && paidOrderCount > 0;
    }

    private boolean hasPurchasedCardType(Long userId, String cardType) {
        Long count = cardPurchaseOrderMapper.selectCount(
            new LambdaQueryWrapper<CardPurchaseOrder>()
                .eq(CardPurchaseOrder::getUserId, userId)
                .eq(CardPurchaseOrder::getCardType, cardType)
                .eq(CardPurchaseOrder::getPayStatus, "paid")
        );
        return count != null && count > 0;
    }

    private boolean hasUserCardType(Long userId, String cardType) {
        Long count = userCardMapper.selectCount(
            new LambdaQueryWrapper<UserCard>()
                .eq(UserCard::getUserId, userId)
                .eq(UserCard::getCardType, cardType)
        );
        return count != null && count > 0;
    }

    private CardPurchaseOrder createMockPurchaseOrder(Long userId, Long storeId, CardProduct product, BigDecimal payAmount) {
        CardPurchaseOrder order = new CardPurchaseOrder();
        order.setPurchaseOrderNo("CP" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        order.setUserId(userId);
        order.setStoreId(storeId);
        order.setCardProductId(product.getId());
        order.setCardType(normalizeCardType(product.getCardType()));
        order.setSourceChannel("store");
        order.setBuyCount(resolvePurchaseCardCount(product));
        order.setPayAmount(normalizeAmount(payAmount));
        order.setPayStatus("paid");
        order.setPurchaseTime(LocalDateTime.now());
        order.setRemark("miniapp mock card purchase");
        cardPurchaseOrderMapper.insert(order);
        return order;
    }

    private Map<String, Object> toProductResult(CardProduct product, Long userId) {
        boolean newUserTrial = isNewUserTrialProduct(product);
        boolean monthlyProduct = isMonthlyCardProduct(product);
        boolean vipDiscounted = isVipTimesCardDiscounted(product, userId);
        BigDecimal originalSalePrice = normalizeAmount(product.getSalePrice());
        BigDecimal effectiveSalePrice = resolveEffectiveSalePrice(product, userId);
        String disabledReason = resolveProductDisabledReason(product, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("id", product.getId());
        result.put("productId", product.getId());
        result.put("storeId", product.getStoreId());
        result.put("cardName", product.getCardName());
        result.put("title", product.getCardName());
        result.put("cardType", normalizeCardType(product.getCardType()));
        result.put("isMonthlyCard", monthlyProduct);
        result.put("isNewUserOnly", Integer.valueOf(1).equals(product.getIsNewUserOnly()));
        result.put("isNewUserTrial", newUserTrial);
        result.put("totalTimes", product.getTotalTimes());
        result.put("salePrice", effectiveSalePrice);
        result.put("originalSalePrice", originalSalePrice);
        result.put("vipDiscounted", vipDiscounted);
        result.put("vipDiscountRate", VIP_CARD_DISCOUNT_RATE);
        result.put("hasVipMonthlyCard", hasActiveMonthlyCard(userId, product.getStoreId()));
        result.put("validDays", product.getValidDays());
        result.put("purchaseLimit", product.getPurchaseLimit());
        result.put("status", product.getStatus());
        result.put("tag", resolveProductTag(product));
        result.put("limitText", monthlyProduct ? "有效期30天" : (newUserTrial ? "全平台限购 1 次" : ""));
        result.put("purchasable", !StringUtils.hasText(disabledReason));
        result.put("disabledReason", disabledReason);
        result.put("desc", resolveProductDescription(product, newUserTrial, monthlyProduct, vipDiscounted));
        return result;
    }

    private String resolveProductDisabledReason(CardProduct product, Long userId) {
        if (userId == null || userId <= 0) {
            return "";
        }
        if (isNewUserTrialProduct(product)) {
            if (hasPurchasedCardType(userId, NEW_USER_TRIAL_CARD_TYPE) || hasUserCardType(userId, NEW_USER_TRIAL_CARD_TYPE)) {
                return "已购买";
            }
            if (hasAnyCardHistory(userId)) {
                return "仅未购卡新用户可买";
            }
        }
        if (isMonthlyCardProduct(product) && hasActiveMonthlyCard(userId, product.getStoreId())) {
            return "生效中";
        }
        return "";
    }

    private boolean isNewUserTrialProduct(CardProduct product) {
        if (product == null) {
            return false;
        }
        return NEW_USER_TRIAL_CARD_TYPE.equals(normalizeCardType(product.getCardType()));
    }

    private boolean isMonthlyCardProduct(CardProduct product) {
        if (product == null) {
            return false;
        }
        return isMonthlyCardType(product.getCardType());
    }

    private boolean isMonthlyCardType(String value) {
        return MONTHLY_CARD_TYPE.equals(normalizeCardType(value));
    }

    private String normalizeCardType(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : "store";
    }

    private String resolveProductTag(CardProduct product) {
        if (isMonthlyCardProduct(product)) {
            return "VIP";
        }
        String remark = product != null ? product.getRemark() : null;
        if (StringUtils.hasText(remark)) {
            return remark.trim();
        }
        Integer totalTimes = product != null ? product.getTotalTimes() : null;
        if (totalTimes != null && totalTimes >= 5) {
            return "划算";
        }
        if (totalTimes != null && totalTimes >= 3) {
            return "热卖";
        }
        return "体验";
    }

    private String resolveProductDescription(
        CardProduct product,
        boolean newUserTrial,
        boolean monthlyProduct,
        boolean vipDiscounted
    ) {
        if (monthlyProduct) {
            return "购买后享本店VIP洗车价，次卡购买同步享优惠";
        }
        if (newUserTrial) {
            return "新人专享低价体验，仅限本店使用";
        }
        String suffix = vipDiscounted ? "，VIP已优惠" : "";
        return "发放 " + product.getTotalTimes() + " 张本店单次卡" + suffix;
    }

    private boolean isVipTimesCardDiscounted(CardProduct product, Long userId) {
        if (product == null || userId == null || userId <= 0) {
            return false;
        }
        if (isMonthlyCardProduct(product) || isNewUserTrialProduct(product)) {
            return false;
        }
        BigDecimal originalPrice = normalizeAmount(product.getSalePrice());
        return hasActiveMonthlyCard(userId, product.getStoreId())
            && originalPrice.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal resolveEffectiveSalePrice(CardProduct product, Long userId) {
        BigDecimal salePrice = normalizeAmount(product != null ? product.getSalePrice() : null);
        if (!isVipTimesCardDiscounted(product, userId)) {
            return salePrice;
        }
        return salePrice.multiply(VIP_CARD_DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private int resolvePurchaseCardCount(CardProduct product) {
        if (isMonthlyCardProduct(product)) {
            return 1;
        }
        Integer totalTimes = product != null ? product.getTotalTimes() : null;
        return totalTimes != null && totalTimes > 0 ? totalTimes : 1;
    }

    private boolean hasActiveMonthlyCard(Long userId, Long storeId) {
        if (userId == null || userId <= 0 || storeId == null || storeId <= 0) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        Long count = userCardMapper.selectCount(
            new LambdaQueryWrapper<UserCard>()
                .eq(UserCard::getUserId, userId)
                .eq(UserCard::getStoreId, storeId)
                .eq(UserCard::getCardType, MONTHLY_CARD_TYPE)
                .eq(UserCard::getStatus, "active")
                .and(wrapper -> wrapper.isNull(UserCard::getEffectiveTime).or().le(UserCard::getEffectiveTime, now))
                .and(wrapper -> wrapper.isNull(UserCard::getExpireTime).or().gt(UserCard::getExpireTime, now))
        );
        return count != null && count > 0;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private List<UserCard> createSingleUseCards(
        Long userId,
        Long storeId,
        Long productId,
        String cardType,
        String sourceChannel,
        Integer count,
        Integer validDays,
        String externalOrderNo,
        String remark
    ) {
        int safeCount = count != null ? count : 0;
        List<UserCard> cards = new ArrayList<>(safeCount);
        for (int i = 0; i < safeCount; i += 1) {
            cards.add(createUserCard(
                userId,
                storeId,
                productId,
                cardType,
                sourceChannel,
                1,
                validDays,
                externalOrderNo,
                remark
            ));
        }
        return cards;
    }

    private UserCard createMonthlyCard(
        Long userId,
        Long storeId,
        Long productId,
        Integer validDays,
        String remark
    ) {
        return createUserCard(
            userId,
            storeId,
            productId,
            MONTHLY_CARD_TYPE,
            MONTHLY_CARD_TYPE,
            0,
            validDays != null && validDays > 0 ? validDays : 30,
            null,
            remark
        );
    }

    private UserCard createUserCard(
        Long userId,
        Long storeId,
        Long productId,
        String cardType,
        String sourceChannel,
        Integer totalTimes,
        Integer validDays,
        String externalOrderNo,
        String remark
    ) {
        LocalDateTime now = LocalDateTime.now();
        UserCard userCard = new UserCard();
        userCard.setUserId(userId);
        userCard.setStoreId(storeId);
        userCard.setCardProductId(productId);
        userCard.setCardType(normalizeCardType(cardType));
        userCard.setSourceChannel(sourceChannel);
        userCard.setCardNo(buildCardNo(sourceChannel));
        userCard.setTotalTimes(totalTimes);
        userCard.setUsedTimes(0);
        userCard.setRemainingTimes(totalTimes);
        userCard.setPurchaseTime(now);
        userCard.setEffectiveTime(now);
        userCard.setExpireTime(validDays != null && validDays > 0 ? now.plusDays(validDays) : null);
        userCard.setStatus("active");
        userCard.setExternalOrderNo(externalOrderNo);
        userCard.setRemark(remark);
        userCardMapper.insert(userCard);
        return userCard;
    }

    private Store requireStore(Long storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }
        Store store = storeService.getById(storeId);
        if (store == null) {
            throw new IllegalArgumentException("store not found");
        }
        return store;
    }

    private Map<String, Object> toCardResult(UserCard userCard, Store store) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", userCard.getId());
        result.put("cardNo", userCard.getCardNo());
        result.put("userId", userCard.getUserId());
        result.put("storeId", userCard.getStoreId());
        result.put("storeName", store != null ? store.getStoreName() : "");
        result.put("cardType", userCard.getCardType());
        result.put("isMonthlyCard", isMonthlyCardType(userCard.getCardType()));
        result.put("sourceChannel", userCard.getSourceChannel());
        result.put("totalTimes", userCard.getTotalTimes());
        result.put("remainingTimes", userCard.getRemainingTimes());
        result.put("status", userCard.getStatus());
        result.put("expireTime", userCard.getExpireTime());
        return result;
    }

    private Map<String, Object> toMonthlyCardPurchaseResult(UserCard userCard, Store store) {
        Map<String, Object> result = toCardResult(userCard, store);
        result.put("createdCount", 1);
        result.put("totalTimes", 0);
        result.put("remainingTimes", 0);
        result.put("unitTimes", 0);
        result.put("sourceChannel", MONTHLY_CARD_TYPE);
        result.put("isMonthlyCard", true);
        result.put("cards", List.of(toCardResult(userCard, store)));
        return result;
    }

    private Map<String, Object> toBatchCardResult(List<UserCard> cards, Store store, String sourceChannel) {
        Map<String, Object> result = new HashMap<>();
        int createdCount = cards != null ? cards.size() : 0;
        if (cards != null && !cards.isEmpty()) {
            result.putAll(toCardResult(cards.get(0), store));
        }
        result.put("createdCount", createdCount);
        result.put("totalTimes", createdCount);
        result.put("remainingTimes", createdCount);
        result.put("unitTimes", 1);
        result.put("sourceChannel", sourceChannel);
        result.put(
            "cards",
            cards == null
                ? List.of()
                : cards.stream().map(card -> toCardResult(card, store)).toList()
        );
        return result;
    }

    private Map<String, Object> toCardListItem(UserCard userCard, Store store, LocalDateTime now) {
        Map<String, Object> result = toCardResult(userCard, store);
        result.put("usedTimes", userCard.getUsedTimes());
        result.put("purchaseTime", userCard.getPurchaseTime());
        result.put("effectiveTime", userCard.getEffectiveTime());
        result.put("externalOrderNo", userCard.getExternalOrderNo());
        result.put("remark", userCard.getRemark());
        result.put("available", isAvailableCard(userCard, now));
        return result;
    }

    private boolean isAvailableCard(UserCard userCard, LocalDateTime now) {
        if (userCard == null) {
            return false;
        }
        if (!"active".equals(userCard.getStatus())) {
            return false;
        }
        if (isMonthlyCardType(userCard.getCardType())) {
            LocalDateTime effectiveTime = userCard.getEffectiveTime();
            if (effectiveTime != null && effectiveTime.isAfter(now)) {
                return false;
            }
            LocalDateTime expireTime = userCard.getExpireTime();
            return expireTime == null || expireTime.isAfter(now);
        }
        Integer remainingTimes = userCard.getRemainingTimes();
        if (remainingTimes == null || remainingTimes <= 0) {
            return false;
        }
        LocalDateTime effectiveTime = userCard.getEffectiveTime();
        if (effectiveTime != null && effectiveTime.isAfter(now)) {
            return false;
        }
        LocalDateTime expireTime = userCard.getExpireTime();
        return expireTime == null || expireTime.isAfter(now);
    }

    private String buildCardNo(String sourceChannel) {
        String prefix = StringUtils.hasText(sourceChannel)
            ? sourceChannel.trim().toUpperCase()
            : "CARD";
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    }

    private String buildRemark(String cardName, BigDecimal salePrice, String fallback) {
        if (!StringUtils.hasText(cardName) && salePrice == null) {
            return fallback;
        }
        String name = StringUtils.hasText(cardName) ? cardName.trim() : "mock card product";
        String price = salePrice != null ? salePrice.stripTrailingZeros().toPlainString() : "0";
        return name + " mock paid " + price;
    }

    private String resolveVoucherSource(String voucherCode) {
        String text = voucherCode.toUpperCase();
        if (text.startsWith("MT") || text.contains("MEITUAN") || text.contains("美团")) {
            return "meituan";
        }
        return "douyin";
    }

    private String normalizeSourceChannel(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim().toLowerCase();
        if ("mt".equals(text) || "meituan".equals(text) || "美团".equals(text)) {
            return "meituan";
        }
        if ("dz".equals(text) || "dazhong".equals(text) || "大众点评".equals(text)) {
            return "dazhong";
        }
        return "douyin";
    }

    private Long resolveStoreIdFromVoucherCode(String voucherCode) {
        Matcher matcher = STORE_ID_PATTERN.matcher(voucherCode);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return 1L;
    }

    private Integer resolveTimesFromVoucherCode(String voucherCode) {
        Matcher matcher = TIMES_PATTERN.matcher(voucherCode);
        if (matcher.find()) {
            try {
                int value = Integer.parseInt(matcher.group(1));
                if (value > 0 && value <= 99) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return 1;
    }

    private String normalizeVoucherCode(String value) {
        return StringUtils.hasText(value) ? value.trim().replace(" ", "").toUpperCase() : "";
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            String text = value.toString();
            if (!StringUtils.hasText(text)) {
                return null;
            }
            return Long.parseLong(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseInteger(Object value) {
        Long parsed = parseLong(value);
        if (parsed == null) {
            return null;
        }
        if (parsed > Integer.MAX_VALUE || parsed < Integer.MIN_VALUE) {
            return null;
        }
        return parsed.intValue();
    }

    private BigDecimal parseAmount(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            String text = value.toString();
            if (StringUtils.hasText(text)) {
                return new BigDecimal(text.trim());
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private String getString(Map<String, Object> payload, String... keys) {
        if (payload == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = payload.get(key);
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value);
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
    }

    private record DefaultCardProductTemplate(
        String cardName,
        String cardType,
        Integer totalTimes,
        BigDecimal salePrice,
        Integer validDays,
        String tag,
        Integer isNewUserOnly,
        Integer purchaseLimit
    ) {
    }
}
