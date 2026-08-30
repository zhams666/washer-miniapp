package com.washer.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.washer.backend.cloudbase.CloudBasePgClient;
import com.washer.backend.cloudbase.mapper.CloudBaseMapperFactory;
import com.washer.backend.mapper.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("cloudbase")
@Configuration
public class CloudBaseMapperConfiguration {

    @Bean
    CardProductMapper cardProductMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(CardProductMapper.class, client, objectMapper);
    }

    @Bean
    CardUsageRecordMapper cardUsageRecordMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(CardUsageRecordMapper.class, client, objectMapper);
    }

    @Bean
    CardPurchaseOrderMapper cardPurchaseOrderMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(CardPurchaseOrderMapper.class, client, objectMapper);
    }

    @Bean
    DeviceMapper deviceMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(DeviceMapper.class, client, objectMapper);
    }

    @Bean
    FranchiseContactMapper franchiseContactMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(FranchiseContactMapper.class, client, objectMapper);
    }

    @Bean
    FranchiseeMapper franchiseeMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(FranchiseeMapper.class, client, objectMapper);
    }

    @Bean
    MembershipOrderMapper membershipOrderMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(MembershipOrderMapper.class, client, objectMapper);
    }

    @Bean
    MembershipPlanMapper membershipPlanMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(MembershipPlanMapper.class, client, objectMapper);
    }

    @Bean
    MembershipSettingMapper membershipSettingMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(MembershipSettingMapper.class, client, objectMapper);
    }

    @Bean
    MiniAdminAssetOperationMapper miniAdminAssetOperationMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(MiniAdminAssetOperationMapper.class, client, objectMapper);
    }

    @Bean
    MiniAdminStaffMapper miniAdminStaffMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(MiniAdminStaffMapper.class, client, objectMapper);
    }

    @Bean
    MiniAdminStaffSessionMapper miniAdminStaffSessionMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(MiniAdminStaffSessionMapper.class, client, objectMapper);
    }

    @Bean
    MiniAdminStaffStoreMapper miniAdminStaffStoreMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(MiniAdminStaffStoreMapper.class, client, objectMapper);
    }

    @Bean
    PaymentCallbackLogMapper paymentCallbackLogMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(PaymentCallbackLogMapper.class, client, objectMapper);
    }

    @Bean
    PaymentTransactionMapper paymentTransactionMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(PaymentTransactionMapper.class, client, objectMapper);
    }

    @Bean
    PointMallProductMapper pointMallProductMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(PointMallProductMapper.class, client, objectMapper);
    }

    @Bean
    PointRedemptionOrderMapper pointRedemptionOrderMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(PointRedemptionOrderMapper.class, client, objectMapper);
    }

    @Bean
    PricingRuleMapper pricingRuleMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(PricingRuleMapper.class, client, objectMapper);
    }

    @Bean
    RechargeOrderMapper rechargeOrderMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(RechargeOrderMapper.class, client, objectMapper);
    }

    @Bean
    StoreMapper storeMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(StoreMapper.class, client, objectMapper);
    }

    @Bean
    StoreSettlementBillMapper storeSettlementBillMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(StoreSettlementBillMapper.class, client, objectMapper);
    }

    @Bean
    StoreSettlementDetailMapper storeSettlementDetailMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(StoreSettlementDetailMapper.class, client, objectMapper);
    }

    @Bean
    UserCardMapper userCardMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(UserCardMapper.class, client, objectMapper);
    }

    @Bean
    UserDailyDiscountRecordMapper userDailyDiscountRecordMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(UserDailyDiscountRecordMapper.class, client, objectMapper);
    }

    @Bean
    UserInfoMapper userInfoMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(UserInfoMapper.class, client, objectMapper);
    }

    @Bean
    UserStoreWalletMapper userStoreWalletMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(UserStoreWalletMapper.class, client, objectMapper);
    }

    @Bean
    UserVehicleMapper userVehicleMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(UserVehicleMapper.class, client, objectMapper);
    }

    @Bean
    WalletRechargeProductMapper walletRechargeProductMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(WalletRechargeProductMapper.class, client, objectMapper);
    }

    @Bean
    WalletTransactionMapper walletTransactionMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(WalletTransactionMapper.class, client, objectMapper);
    }

    @Bean
    WashOrderMapper washOrderMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(WashOrderMapper.class, client, objectMapper);
    }

    @Bean
    WashOrderPaymentDetailMapper washOrderPaymentDetailMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(WashOrderPaymentDetailMapper.class, client, objectMapper);
    }

    @Bean
    WashOrderStatusLogMapper washOrderStatusLogMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(WashOrderStatusLogMapper.class, client, objectMapper);
    }

    @Bean
    WashQueueMapper washQueueMapper(CloudBasePgClient client, ObjectMapper objectMapper) {
        return CloudBaseMapperFactory.create(WashQueueMapper.class, client, objectMapper);
    }
}

