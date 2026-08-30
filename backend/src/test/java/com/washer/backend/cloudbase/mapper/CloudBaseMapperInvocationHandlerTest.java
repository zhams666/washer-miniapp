package com.washer.backend.cloudbase.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.washer.backend.cloudbase.CloudBasePgClient;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.mapper.UserInfoMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
}
