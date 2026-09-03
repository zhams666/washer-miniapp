package com.washer.backend.cloudbase.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.washer.backend.cloudbase.CloudBasePgClient;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.entity.WalletTransaction;
import com.washer.backend.mapper.UserInfoMapper;
import com.washer.backend.mapper.WalletTransactionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudBaseMapperInvocationHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void selectOneTranslatesWrapperFiltersAndSortOrder() throws Exception {
        CloudBasePgClient client = mock(CloudBasePgClient.class);
        when(client.select(eq("user_info"), any())).thenReturn(objectMapper.readTree("[{\"id\":1,\"openid\":\"openid-1\",\"user_no\":\"U-1\"}]"));
        UserInfoMapper mapper = CloudBaseMapperFactory.create(UserInfoMapper.class, client, objectMapper);

        UserInfo result = mapper.selectOne(
            new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getOpenid, "openid-1").orderByDesc(UserInfo::getId)
        );

        ArgumentCaptor<Map<String, List<String>>> query = ArgumentCaptor.forClass(Map.class);
        verify(client).select(eq("user_info"), query.capture());
        assertEquals(List.of("eq.openid-1"), query.getValue().get("openid"));
        assertEquals(List.of("id.desc"), query.getValue().get("order"));
        assertEquals(List.of("2"), query.getValue().get("limit"));
        assertEquals("U-1", result.getUserNo());
    }

    @Test
    void selectListTranslatesAnOrderOnlyWrapper() throws Exception {
        CloudBasePgClient client = mock(CloudBasePgClient.class);
        when(client.select(eq("user_info"), any())).thenReturn(objectMapper.readTree("[]"));
        UserInfoMapper mapper = CloudBaseMapperFactory.create(UserInfoMapper.class, client, objectMapper);

        mapper.selectList(new LambdaQueryWrapper<UserInfo>().orderByDesc(UserInfo::getId));

        ArgumentCaptor<Map<String, List<String>>> query = ArgumentCaptor.forClass(Map.class);
        verify(client).select(eq("user_info"), query.capture());
        assertEquals(List.of("id.desc"), query.getValue().get("order"));
    }

    @Test
    void updateTranslatesSafeSetAndFilterExpressions() throws Exception {
        CloudBasePgClient client = mock(CloudBasePgClient.class);
        when(client.update(eq("user_info"), any(), any())).thenReturn(objectMapper.readTree("[{\"id\":1}]"));
        UserInfoMapper mapper = CloudBaseMapperFactory.create(UserInfoMapper.class, client, objectMapper);

        int affected = mapper.update(null, new LambdaUpdateWrapper<UserInfo>()
            .eq(UserInfo::getId, 1L)
            .set(UserInfo::getNickname, "new-name"));

        ArgumentCaptor<Map<String, List<String>>> filter = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Object>> body = ArgumentCaptor.forClass(Map.class);
        verify(client).update(eq("user_info"), filter.capture(), body.capture());
        assertEquals(List.of("eq.1"), filter.getValue().get("id"));
        assertEquals("new-name", body.getValue().get("nickname"));
        assertEquals(1, affected);
    }

    @Test
    void updateSerializesTimestampAssignmentsAsIsoStrings() throws Exception {
        CloudBasePgClient client = mock(CloudBasePgClient.class);
        when(client.update(eq("user_info"), any(), any())).thenReturn(objectMapper.readTree("[{\"id\":1}]"));
        UserInfoMapper mapper = CloudBaseMapperFactory.create(UserInfoMapper.class, client, objectMapper);
        LocalDateTime timestamp = LocalDateTime.of(2026, 9, 3, 9, 43, 23, 858_741_358);

        mapper.update(null, new LambdaUpdateWrapper<UserInfo>()
            .eq(UserInfo::getId, 1L)
            .set(UserInfo::getLastLoginTime, timestamp));

        ArgumentCaptor<Map<String, Object>> body = ArgumentCaptor.forClass(Map.class);
        verify(client).update(eq("user_info"), any(), body.capture());
        assertEquals("2026-09-03T09:43:23.858741358", body.getValue().get("last_login_time"));
    }

    @Test
    void insertSerializesLocalDateTimesAsPostgrestTimestampStrings() throws Exception {
        CloudBasePgClient client = mock(CloudBasePgClient.class);
        when(client.insert(eq("user_info"), any())).thenReturn(objectMapper.readTree("[{\"id\":1}]"));
        UserInfoMapper mapper = CloudBaseMapperFactory.create(UserInfoMapper.class, client, objectMapper);
        UserInfo user = new UserInfo();
        user.setUserNo("U-1");
        user.setLastLoginTime(LocalDateTime.of(2026, 8, 31, 9, 21, 41, 469_244_838));

        mapper.insert(user);

        ArgumentCaptor<Map<String, Object>> body = ArgumentCaptor.forClass(Map.class);
        verify(client).insert(eq("user_info"), body.capture());
        assertEquals("2026-08-31T09:21:41.469244838", body.getValue().get("last_login_time"));
        assertEquals(1L, user.getId());
    }

    @Test
    void selectTranslatesCamelCaseColumnsToPostgresNames() throws Exception {
        CloudBasePgClient client = mock(CloudBasePgClient.class);
        when(client.select(eq("wallet_transaction"), any())).thenReturn(objectMapper.readTree("[]"));
        WalletTransactionMapper mapper = CloudBaseMapperFactory.create(WalletTransactionMapper.class, client, objectMapper);

        mapper.selectList(new LambdaQueryWrapper<WalletTransaction>()
            .eq(WalletTransaction::getUserId, 1L)
            .eq(WalletTransaction::getBizType, "recharge")
            .eq(WalletTransaction::getChangeType, "in")
            .orderByDesc(WalletTransaction::getCreatedAt));

        ArgumentCaptor<Map<String, List<String>>> query = ArgumentCaptor.forClass(Map.class);
        verify(client).select(eq("wallet_transaction"), query.capture());
        assertEquals(List.of("eq.1"), query.getValue().get("user_id"));
        assertEquals(List.of("eq.recharge"), query.getValue().get("biz_type"));
        assertEquals(List.of("eq.in"), query.getValue().get("change_type"));
        assertEquals(List.of("created_at.desc"), query.getValue().get("order"));
    }
}
