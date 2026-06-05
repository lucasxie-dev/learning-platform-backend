package dev.lucasxie.learning.file;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long>, JpaSpecificationExecutor<FileAsset> {

	Optional<FileAsset> findByStorageKey(String storageKey);

	List<FileAsset> findByOwnerId(Long ownerId);

	List<FileAsset> findByRelatedTypeAndRelatedId(String relatedType, Long relatedId);

	boolean existsByIdAndOwnerId(Long id, Long ownerId);

	long countByOwnerId(Long ownerId);

	long countByAssetTypeIn(Collection<FileAssetType> assetTypes);

	long countByOwnerIdAndAssetTypeIn(Long ownerId, Collection<FileAssetType> assetTypes);

	@Query("select fileAsset from FileAsset fileAsset where fileAsset.assetType in :assetTypes order by fileAsset.createdAt desc")
	List<FileAsset> findRecentByAssetTypes(Collection<FileAssetType> assetTypes, Pageable pageable);

	@Query("select fileAsset from FileAsset fileAsset where fileAsset.ownerId = :ownerId and fileAsset.assetType in :assetTypes order by fileAsset.createdAt desc")
	List<FileAsset> findRecentByOwnerIdAndAssetTypes(Long ownerId, Collection<FileAssetType> assetTypes, Pageable pageable);
}
