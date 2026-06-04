package dev.lucasxie.learning.storage;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.lucasxie.learning.common.exception.BusinessException;
import dev.lucasxie.learning.common.exception.ErrorCode;
import dev.lucasxie.learning.file.FileAsset;
import dev.lucasxie.learning.file.FileAssetRepository;
import dev.lucasxie.learning.file.FileBlob;
import dev.lucasxie.learning.file.FileBlobRepository;
import dev.lucasxie.learning.file.StorageProvider;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "database", matchIfMissing = true)
public class DatabaseStorageService implements StorageService {

	private static final String STORAGE_KEY_PREFIX = "database/";

	private final FileBlobRepository fileBlobRepository;

	private final FileAssetRepository fileAssetRepository;

	private final StorageProperties storageProperties;

	public DatabaseStorageService(
		FileBlobRepository fileBlobRepository,
		FileAssetRepository fileAssetRepository,
		StorageProperties storageProperties
	) {
		this.fileBlobRepository = fileBlobRepository;
		this.fileAssetRepository = fileAssetRepository;
		this.storageProperties = storageProperties;
	}

	@Override
	@Transactional
	public StorageUploadResult upload(StorageUploadCommand command) {
		if (command.fileAssetId() == null) {
			throw new BusinessException(ErrorCode.STORAGE_ERROR, "File asset id is required for database storage");
		}

		byte[] content = readAllBytes(command.inputStream());
		String checksum = sha256(content);
		String storageKey = STORAGE_KEY_PREFIX + UUID.randomUUID();

		FileBlob fileBlob = new FileBlob();
		fileBlob.setFileAssetId(command.fileAssetId());
		fileBlob.setContent(content);
		fileBlobRepository.save(fileBlob);

		return new StorageUploadResult(
			storageKey,
			getAccessUrl(storageKey),
			command.contentType(),
			command.sizeBytes(),
			checksum,
			StorageProvider.DATABASE
		);
	}

	@Override
	@Transactional(readOnly = true)
	public StorageDownloadResult download(String storageKey) {
		FileAsset fileAsset = fileAssetRepository.findByStorageKey(storageKey)
			.orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND, "File was not found"));

		FileBlob fileBlob = fileBlobRepository.findByFileAssetId(fileAsset.getId())
			.orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND, "File content was not found"));

		return new StorageDownloadResult(
			fileBlob.getContent(),
			fileAsset.getContentType(),
			fileAsset.getSizeBytes(),
			fileAsset.getOriginalName()
		);
	}

	@Override
	@Transactional
	public void delete(String storageKey) {
		fileAssetRepository.findByStorageKey(storageKey)
			.ifPresent(fileAsset -> fileBlobRepository.deleteByFileAssetId(fileAsset.getId()));
	}

	@Override
	public String getAccessUrl(String storageKey) {
		return fileAssetRepository.findByStorageKey(storageKey)
			.map(fileAsset -> buildContentUrl(fileAsset.getId()))
			.orElse(normalizeBaseUrl(storageProperties.getPublicBaseUrl()) + "/api/v1/files/content");
	}

	private String buildContentUrl(Long fileId) {
		return normalizeBaseUrl(storageProperties.getPublicBaseUrl()) + "/api/v1/files/" + fileId + "/content";
	}

	private byte[] readAllBytes(InputStream inputStream) {
		try {
			return inputStream.readAllBytes();
		}
		catch (IOException exception) {
			throw new BusinessException(ErrorCode.STORAGE_ERROR, "Failed to read uploaded file");
		}
	}

	private String sha256(byte[] content) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(content));
		}
		catch (NoSuchAlgorithmException exception) {
			throw new BusinessException(ErrorCode.STORAGE_ERROR, "SHA-256 is not available");
		}
	}

	private String normalizeBaseUrl(String publicBaseUrl) {
		if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
			return "";
		}

		return publicBaseUrl.endsWith("/")
			? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
			: publicBaseUrl;
	}
}
