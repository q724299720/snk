package com.snk.server.infrastructure.admin;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AdminProperties.class)
public class AdminWebMvcConfigurer implements WebMvcConfigurer {

	private final AdminProperties adminProperties;

	public AdminWebMvcConfigurer(AdminProperties adminProperties) {
		this.adminProperties = adminProperties;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new AdminApiTokenInterceptor(adminProperties))
			.addPathPatterns("/api/admin/**");
	}
}
