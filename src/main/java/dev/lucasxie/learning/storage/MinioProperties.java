package dev.lucasxie.learning.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.minio")
public class MinioProperties {

	private String endpoint;

	private String accessKey;

	private String secretKey;

	private String bucket;

	private String region;

	private boolean secure;
}
