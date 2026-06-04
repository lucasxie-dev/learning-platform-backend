package dev.lucasxie.learning.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.file-access")
public class FileAccessProperties {

	private String secret = "local-file-access-secret-change-me";

	private long defaultExpirationMinutes = 10;
}
