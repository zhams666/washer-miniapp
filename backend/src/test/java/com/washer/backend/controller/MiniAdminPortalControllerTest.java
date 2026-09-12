package com.washer.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.miniadmin.MiniAdminOperationOverview;
import com.washer.backend.dto.miniadmin.MiniAdminSessionContext;
import com.washer.backend.entity.MiniAdminStaff;
import com.washer.backend.service.MiniAdminAuthService;
import com.washer.backend.service.MiniAdminPortalService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MiniAdminPortalControllerTest {

    @Mock
    private MiniAdminAuthService miniAdminAuthService;

    @Mock
    private MiniAdminPortalService miniAdminPortalService;

    @InjectMocks
    private MiniAdminPortalController controller;

    @Test
    void operationOverview_returnsServiceResultWithRequestTrace() {
        MiniAdminStaff staff = new MiniAdminStaff();
        staff.setId(101L);
        MiniAdminSessionContext context = new MiniAdminSessionContext(staff, true, List.of(), List.of());
        MiniAdminOperationOverview expected = new MiniAdminOperationOverview();
        LocalDate bizDate = LocalDate.of(2026, 9, 12);
        when(miniAdminAuthService.requireContext("admin-token")).thenReturn(context);
        when(miniAdminPortalService.getOperationOverview(context, bizDate, 1L)).thenReturn(expected);

        ApiResponse<MiniAdminOperationOverview> result = controller.operationOverview(
            "admin-token",
            "WASHER_1726140000000_test01",
            bizDate,
            1L
        );

        assertThat(result.getData()).isSameAs(expected);
        verify(miniAdminAuthService).requireContext("admin-token");
        verify(miniAdminPortalService).getOperationOverview(context, bizDate, 1L);
    }
}
