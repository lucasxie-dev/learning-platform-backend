package dev.lucasxie.learning.cache;

import java.time.Duration;

import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisCacheConfig {

	@Bean
	public CacheManager cacheManager(RedisConnectionFactory connectionFactory, CacheProperties cacheProperties) {
		CacheProperties.Redis redisProperties = cacheProperties.getRedis();
		GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

		RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
			.serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
			.serializeValuesWith(SerializationPair.fromSerializer(valueSerializer));

		Duration timeToLive = redisProperties.getTimeToLive();
		if (timeToLive != null) {
			configuration = configuration.entryTtl(timeToLive);
		}

		if (!redisProperties.isCacheNullValues()) {
			configuration = configuration.disableCachingNullValues();
		}

		if (redisProperties.isUseKeyPrefix()) {
			String keyPrefix = redisProperties.getKeyPrefix();
			if (keyPrefix != null) {
				configuration = configuration.computePrefixWith(cacheName -> keyPrefix + cacheName + "::");
			}
		}
		else {
			configuration = configuration.disableKeyPrefix();
		}

		return RedisCacheManager.builder(connectionFactory)
			.cacheDefaults(configuration)
			.transactionAware()
			.build();
	}
}
