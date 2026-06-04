package dev.lucasxie.learning.file;

import dev.lucasxie.learning.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
	name = "file_blob",
	uniqueConstraints = @UniqueConstraint(name = "uk_file_blob_file_asset_id", columnNames = "file_asset_id"),
	indexes = @Index(name = "idx_file_blob_file_asset_id", columnList = "file_asset_id")
)
public class FileBlob extends BaseEntity {

	@Column(name = "file_asset_id", nullable = false)
	private Long fileAssetId;

	@Column(name = "content", nullable = false, columnDefinition = "bytea")
	private byte[] content;
}
