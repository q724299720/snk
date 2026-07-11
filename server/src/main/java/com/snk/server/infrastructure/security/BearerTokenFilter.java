package com.snk.server.infrastructure.security;

import com.snk.server.domain.auth.JwtTokenService;
import com.snk.server.infrastructure.persistence.user.AccountStatus;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class BearerTokenFilter extends OncePerRequestFilter {
	private final JwtTokenService jwtTokens; private final UserRepository users;
	public BearerTokenFilter(JwtTokenService jwtTokens, UserRepository users) { this.jwtTokens=jwtTokens; this.users=users; }

	@Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
		throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith("Bearer ")) { chain.doFilter(request, response); return; }
		try {
			var jwt = jwtTokens.decode(header.substring(7));
			Long userId = Long.valueOf(jwt.getSubject());
			UserEntity user = users.findById(userId).orElseThrow();
			long version = ((Number) jwt.getClaim("tokenVersion")).longValue();
			if (user.getAccountStatus() != AccountStatus.ACTIVE || user.getTokenVersion() != version) throw new IllegalStateException();
			AuthenticatedAccountPrincipal principal = new AuthenticatedAccountPrincipal(userId, user.getUsername(), user.getRole(), version, user.isMustChangePassword());
			var authentication = new UsernamePasswordAuthenticationToken(principal, null,
				List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
			SecurityContextHolder.getContext().setAuthentication(authentication);
			chain.doFilter(request, response);
		}
		catch (Exception exception) {
			SecurityContextHolder.clearContext();
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); response.setContentType("application/problem+json");
			response.getWriter().write("{\"status\":401,\"title\":\"TOKEN_INVALID\",\"code\":\"TOKEN_INVALID\"}");
		}
	}
}
