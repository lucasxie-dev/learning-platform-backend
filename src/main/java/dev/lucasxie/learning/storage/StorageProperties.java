package dev.lucasxie.learning.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import dev.lucasxie.learning.file.StorageProvider;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

	private StorageProvider provider = StorageProvider.DATABASE;

	private long maxFileSizeMb = 20;

	private String publicBaseUrl;
}
