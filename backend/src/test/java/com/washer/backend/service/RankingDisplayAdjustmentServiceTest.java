package com.washer.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.washer.backend.dto.admin.AdminRankingDisplayAdjustmentCreateRequest;
import com.washer.backend.dto.admin.AdminRankingDisplayAdjustmentItem;
import com.washer.backend.entity.RankingDisplayAdjustment;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.mapper.RankingDisplayAdjustmentMapper;
import com.washer.backend.mapper.UserInfoMapper;
import com.washer.backend.service.impl.RankingDisplayAdjustmentServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RankingDisplayAdjustmentServiceTest {

    @Mock
    private RankingDisplayAdjustmentMapper rankingDisplayAdjustmentMapper;

    @Mock
    private UserInfoMapper userInfoMapper;

    @InjectMocks
    private RankingDisplayAdjustmentServiceImpl service;

    @BeforeEach
    void initializeMyBatisMetadata() {
        if (TableInfoHelper.getTableInfo(RankingDisplayAdjustment.class) == null) {
            TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new Configuration(), RankingDisplayAdjustment.class.getName()),
                RankingDisplayAdjustment.class
            );
        }
    }

    @Test
    void createAdjustment_storesDisplayDurationAtCurrentTimeWithoutChangingUser() {
        UserInfo user = new UserInfo();
        user.setId(21L);
        user.setNickname("演示车友");
        when(userInfoMapper.selectById(21L)).thenReturn(user);
        when(rankingDisplayAdjustmentMapper.insert(any(RankingDisplayAdjustment.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, RankingDisplayAdjustment.class).setId(9L);
            return 1;
        });

        AdminRankingDisplayAdjustmentCreateRequest request = new AdminRankingDisplayAdjustmentCreateRequest();
        request.setUserId(21L);
        request.setDurationMinutes(90L);
        request.setRemark("运营补充");

        AdminRankingDisplayAdjustmentItem result = service.createAdjustment(request);

        ArgumentCaptor<RankingDisplayAdjustment> captor = ArgumentCaptor.forClass(RankingDisplayAdjustment.class);
        verify(rankingDisplayAdjustmentMapper).insert(captor.capture());
        RankingDisplayAdjustment stored = captor.getValue();
        assertThat(stored.getUserId()).isEqualTo(21L);
        assertThat(stored.getDisplayDurationSeconds()).isEqualTo(5_400L);
        assertThat(stored.getOccurredAt()).isNotNull();
        assertThat(result.getDurationText()).isEqualTo("01时30分");
    }

    @Test
    void createAdjustment_rejectsInvalidDurationBeforeWritingAnything() {
        AdminRankingDisplayAdjustmentCreateRequest request = new AdminRankingDisplayAdjustmentCreateRequest();
        request.setUserId(21L);
        request.setDurationMinutes(0L);

        assertThatThrownBy(() -> service.createAdjustment(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("durationMinutes must be greater than 0");
        verifyNoInteractions(userInfoMapper, rankingDisplayAdjustmentMapper);
    }

    @Test
    void deleteAdjustment_removesOnlyTheSelectedAdjustmentRecord() {
        when(rankingDisplayAdjustmentMapper.deleteById(9L)).thenReturn(1);

        service.deleteAdjustment(9L);

        verify(rankingDisplayAdjustmentMapper).deleteById(9L);
        verifyNoInteractions(userInfoMapper);
    }

    @Test
    void listForRanking_includesAdjustmentsFromShorterRankingPeriods() {
        when(rankingDisplayAdjustmentMapper.selectList(any())).thenReturn(List.of());
        LocalDateTime now = LocalDateTime.now();

        service.listForRanking("day", now.minusHours(24), now);
        service.listForRanking("month", now.minusDays(30), now);
        service.listForRanking("total", null, now);

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<LambdaQueryWrapper<RankingDisplayAdjustment>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(rankingDisplayAdjustmentMapper, times(3)).selectList(captor.capture());

        assertThat(queryScopeValues(captor.getAllValues().get(0))).containsExactlyInAnyOrder("day");
        assertThat(queryScopeValues(captor.getAllValues().get(1))).containsExactlyInAnyOrder("day", "month");
        assertThat(queryScopeValues(captor.getAllValues().get(2))).containsExactlyInAnyOrder("day", "month", "total");
    }

    private List<String> queryScopeValues(LambdaQueryWrapper<RankingDisplayAdjustment> wrapper) {
        wrapper.getSqlSegment();
        return wrapper.getParamNameValuePairs().values().stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .toList();
    }
}
