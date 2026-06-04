package dev.lucasxie.learning.file;

import dev.lucasxie.learning.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
	name = "file_asset",
	indexes = {
		@Index(name = "idx_file_asset_owner_id", columnList = "owner_id"),
		@Index(name = "idx_file_asset_asset_type", columnList = "asset_type"),
		@Index(name = "idx_file_asset_related", columnList = "related_type, related_id")
	}
)
public class FileAsset extends BaseEntity {

	@Column(name = "original_name", nullable = false, length = 255)
	private String originalName;

	@Column(name = "storage_key", nullable = false, length = 1024)
	private String storageKey;

	@Column(name = "url", length = 1024)
	private String url;

	@Column(name = "content_type", nullable = false, length = 100)
	private String contentType;

	@Column(name = "size_bytes", nullable = false)
	private Long sizeBytes;

	@Enumerated(EnumType.STRING)
	@Column(name = "asset_type", nullable = false, length = 50)
	private FileAssetType assetType;

	@Enumerated(EnumType.STRING)
	@Column(name = "storage_provider", nullable = false, length = 30)
	private StorageProvider storageProvider;

	@Column(name = "owner_id", nullable = false)
	private Long ownerId;

	@Column(name = "related_type", length = 100)
	private String relatedType;

	@Column(name = "related_id")
	private Long relatedId;

	@Column(name = "checksum", length = 128)
	private String checksum;
}
