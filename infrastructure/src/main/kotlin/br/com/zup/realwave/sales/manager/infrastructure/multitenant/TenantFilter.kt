package br.com.zup.realwave.sales.manager.infrastructure.multitenant

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class TenantFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val tenantId = request.getHeader("X-Realwave-Organization-Slug")
            ?: return response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing X-Realwave-Organization-Slug")

        try {
            TenantContext.set(tenantId)
            chain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }
}
