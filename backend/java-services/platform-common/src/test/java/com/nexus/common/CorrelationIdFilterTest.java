package com.nexus.common;

import com.nexus.common.error.ErrorCode;
import com.nexus.common.error.ProblemDetails;
import com.nexus.common.web.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    @Test
    void keepsValidIncomingId() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader(CorrelationIdFilter.HEADER, "abc123def456");
        var res = new MockHttpServletResponse();
        new CorrelationIdFilter().doFilter(req, res, new MockFilterChain());
        assertThat(res.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("abc123def456");
    }

    @Test
    void replacesMaliciousIncomingId() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader(CorrelationIdFilter.HEADER, "bad\nvalue<script>");
        var res = new MockHttpServletResponse();
        new CorrelationIdFilter().doFilter(req, res, new MockFilterChain());
        assertThat(res.getHeader(CorrelationIdFilter.HEADER)).matches("[a-f0-9]{16}");
    }

    @Test
    void problemDetailCarriesCodeAndType() {
        ProblemDetail pd = ProblemDetails.of(ErrorCode.NOT_FOUND, "User not found", "/api/v1/accounts/1");
        assertThat(pd.getStatus()).isEqualTo(404);
        assertThat(pd.getProperties()).containsEntry("code", "NOT_FOUND");
        assertThat(pd.getType().toString()).endsWith("/not-found");
    }
}
