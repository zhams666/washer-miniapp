package com.washer.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.entity.UserCard;
import com.washer.backend.mapper.UserCardMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MiniCardControllerTest {

    @Test
    void summaryFiltersCardValidityInJavaWithoutAnOrQuery() {
        UserCardMapper mapper = mock(UserCardMapper.class);
        when(mapper.selectCount(any())).thenReturn(2L);
        when(mapper.selectList(any())).thenReturn(List.of(
            card("active", 3, LocalDateTime.now().minusDays(1), null),
            card("active", 5, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusMinutes(1))
        ));
        MiniCardController controller = new MiniCardController(mapper, null, null, null, null, null, null);

        ApiResponse<Map<String, Object>> response = controller.summary(1L);

        assertEquals(2L, response.getData().get("totalCount"));
        assertEquals(1L, response.getData().get("availableCardRows"));
        assertEquals(3, response.getData().get("remainingTimes"));
        ArgumentCaptor<LambdaQueryWrapper<UserCard>> query = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectList(query.capture());
        assertFalse(query.getValue().getSqlSegment().toUpperCase().contains(" OR "));
    }

    private UserCard card(String status, int remainingTimes, LocalDateTime effectiveTime, LocalDateTime expireTime) {
        UserCard card = new UserCard();
        card.setStatus(status);
        card.setRemainingTimes(remainingTimes);
        card.setEffectiveTime(effectiveTime);
        card.setExpireTime(expireTime);
        return card;
    }
}
