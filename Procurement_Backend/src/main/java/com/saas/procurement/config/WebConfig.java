package com.saas.procurement.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serves uploaded photos/videos from the uploads folder at http://localhost:8081/uploads/... */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final Path uploadDir;

	public WebConfig(@Value("${app.upload-dir}") String uploadDir) {
		this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/uploads/**")
				.addResourceLocations(uploadDir.toUri().toString());
	}

}
