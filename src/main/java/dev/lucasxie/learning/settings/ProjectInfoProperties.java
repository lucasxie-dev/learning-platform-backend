package dev.lucasxie.learning.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.project")
public class ProjectInfoProperties {

	private String backendRepository;

	private String frontendRepository;
}
