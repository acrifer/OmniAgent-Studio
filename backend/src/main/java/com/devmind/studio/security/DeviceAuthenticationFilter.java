package com.devmind.studio.security;

import com.devmind.studio.entity.DeviceProfile;
import com.devmind.studio.service.DeviceProfileService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class DeviceAuthenticationFilter extends OncePerRequestFilter {
    public static final String DEVICE_ID_HEADER = "X-Device-Id";

    private final DeviceProfileService devices;

    public DeviceAuthenticationFilter(DeviceProfileService devices) {
        this.devices = devices;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isBypassed(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            DeviceProfile device = devices.resolve(request.getHeader(DEVICE_ID_HEADER));
            var auth = new UsernamePasswordAuthenticationToken(device.getId(), null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }

    private boolean isBypassed(String path) {
        return path.startsWith("/api/internal/")
                || path.startsWith("/api/admin/")
                || path.startsWith("/api/auth/")
                || path.startsWith("/h2-console/")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/");
    }
}
