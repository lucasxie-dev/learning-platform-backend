package dev.lucasxie.learning.file;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {

	Optional<FileAsset> findByStorageKey(String storageKey);

	List<FileAsset> findByOwnerId(Long ownerId);

	List<FileAsset> findByRelatedTypeAndRelatedId(String relatedType, Long relatedId);

	boolean existsByIdAndOwnerId(Long id, Long ownerId);
}
