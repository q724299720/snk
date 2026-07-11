package com.snk.server.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import com.snk.server.domain.auth.JwtTokenService;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
	@Bean
	@ConditionalOnProperty(name = "snk.auth.enforce-security", havingValue = "true", matchIfMissing = true)
	BearerTokenFilter bearerTokenFilter(JwtTokenService jwtTokens, UserRepository users) {
		return new BearerTokenFilter(jwtTokens, users);
	}

	@Bean
	@ConditionalOnProperty(name = "snk.auth.enforce-security", havingValue = "true", matchIfMissing = true)
	SecurityFilterChain securityFilterChain(HttpSecurity http, BearerTokenFilter bearerTokenFilter) throws Exception {
		return http.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/admin", "/api/admin/**").hasRole("OWNER")
				.requestMatchers("/actuator/health/**", "/api/auth/register", "/api/auth/registration-status",
					"/api/auth/login", "/api/auth/refresh", "/admin/**", "/error").permitAll()
				.anyRequest().authenticated())
			.exceptionHandling(errors -> errors
				.authenticationEntryPoint((request, response, exception) -> write(response, 401, "AUTH_REQUIRED"))
				.accessDeniedHandler((request, response, exception) -> write(response, 403, "AUTH_FORBIDDEN")))
			.addFilterBefore(bearerTokenFilter, UsernamePasswordAuthenticationFilter.class).build();
	}

	@Bean
	@ConditionalOnProperty(name = "snk.auth.enforce-security", havingValue = "false")
	SecurityFilterChain compatibilitySecurityFilterChain(HttpSecurity http) throws Exception {
		return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
	}

	private static void write(HttpServletResponse response, int status, String code) throws java.io.IOException {
		response.setStatus(status); response.setContentType("application/problem+json");
		response.getWriter().write("{\"status\":" + status + ",\"title\":\"" + code + "\",\"code\":\"" + code + "\"}");
	}
}
