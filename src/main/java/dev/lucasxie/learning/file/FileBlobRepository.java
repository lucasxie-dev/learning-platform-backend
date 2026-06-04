package dev.lucasxie.learning.file;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FileBlobRepository extends JpaRepository<FileBlob, Long> {

	Optional<FileBlob> findByFileAssetId(Long fileAssetId);

	void deleteByFileAssetId(Long fileAssetId);
}
