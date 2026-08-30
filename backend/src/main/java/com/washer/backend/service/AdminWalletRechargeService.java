package com.washer.backend.service;

import com.washer.backend.dto.admin.AdminWalletRechargeRequest;
import com.washer.backend.dto.admin.AdminWalletRechargeResult;
import java.util.Map;

public interface AdminWalletRechargeService {

    AdminWalletRechargeResult manualRecharge(AdminWalletRechargeRequest request);

    AdminWalletRechargeResult createMiniappRechargeOrder(AdminWalletRechargeRequest request);

    AdminWalletRechargeResult getRechargeOrderResult(String rechargeOrderNo);

    AdminWalletRechargeResult syncRechargeOrder(String rechargeOrderNo);

    Map<String, String> handleWechatPayNotify(Map<String, String> headers, String body);
}
