package com.example.securefilevault.security;

import com.example.securefilevault.mapper.UserMapper;
import com.example.securefilevault.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // リクエストごとに Authorization ヘッダーを検証し、認証済みユーザーを SecurityContext に設定する。
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public JwtAuthenticationFilter(JwtService jwtService, UserMapper userMapper) {
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveBearerToken(request);

        if (token != null && jwtService.isTokenValid(token)) {
            // token の subject から userId を取り出し、DB の最新ユーザー情報を使う。
            Long userId = jwtService.getUserId(token);
            User user = userMapper.findById(userId);

            if (user != null) {
                // Spring Security の権限形式に合わせて ROLE_ prefix を付ける。
                List<SimpleGrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        // "Bearer " の後ろだけを JWT として取り出す。
        return authorization.substring(7);
    }
}
