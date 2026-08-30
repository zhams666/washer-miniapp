package com.washer.backend.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class QueryParameterValidationInterceptorTest {

    private final QueryParameterValidationInterceptor interceptor = new QueryParameterValidationInterceptor();

    @Test
    void rejectsExcessivePageSize() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("size", "101");
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("size must be between 1 and 100");
    }

    @Test
    void rejectsNonNumericLimit() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("limit", "all");
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("limit must be an integer");
    }
}
