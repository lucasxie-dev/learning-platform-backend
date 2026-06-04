package dev.lucasxie.learning.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.lucasxie.learning.file.FileAsset;
import dev.lucasxie.learning.file.FileAssetRepository;
import dev.lucasxie.learning.file.FileAssetType;
import dev.lucasxie.learning.file.FileBlob;
import dev.lucasxie.learning.file.FileBlobRepository;
import dev.lucasxie.learning.file.StorageProvider;

class DatabaseStorageServiceTest {

	@Test
	void uploadDownloadAndDeleteFileBlob() {
		FakeFileAssetRepository fileAssets = new FakeFileAssetRepository();
		FakeFileBlobRepository fileBlobs = new FakeFileBlobRepository();
		StorageProperties properties = new StorageProperties();
		properties.setPublicBaseUrl("http://localhost:8080");
		DatabaseStorageService storageService = new DatabaseStorageService(
			fileBlobs.repository(),
			fileAssets.repository(),
			properties
		);
		FileAsset fileAsset = fileAsset(1L, "cover.png", "image/png", 4L);
		fileAssets.save(fileAsset);

		StorageUploadResult uploadResult = storageService.upload(new StorageUploadCommand(
			"cover.png",
			"image/png",
			4L,
			new ByteArrayInputStream(new byte[] {1, 2, 3, 4}),
			FileAssetType.COURSE_COVER,
			10L,
			1L
		));
		fileAsset.setStorageKey(uploadResult.storageKey());

		assertThat(uploadResult.storageProvider()).isEqualTo(StorageProvider.DATABASE);
		assertThat(uploadResult.storageKey()).startsWith("database/");
		assertThat(uploadResult.checksum()).hasSize(64);
		assertThat(fileBlobs.findByFileAssetId(1L)).isPresent()
			.get()
			.extracting(FileBlob::getContent)
			.isEqualTo(new byte[] {1, 2, 3, 4});

		StorageDownloadResult downloadResult = storageService.download(uploadResult.storageKey());

		assertThat(downloadResult.content()).isEqualTo(new byte[] {1, 2, 3, 4});
		assertThat(downloadResult.contentType()).isEqualTo("image/png");
		assertThat(downloadResult.originalName()).isEqualTo("cover.png");

		storageService.delete(uploadResult.storageKey());

		assertThat(fileBlobs.findByFileAssetId(1L)).isEmpty();
	}

	private FileAsset fileAsset(Long id, String originalName, String contentType, Long sizeBytes) {
		FileAsset fileAsset = new FileAsset();
		fileAsset.setId(id);
		fileAsset.setOriginalName(originalName);
		fileAsset.setStorageKey("pending/1");
		fileAsset.setContentType(contentType);
		fileAsset.setSizeBytes(sizeBytes);
		fileAsset.setAssetType(FileAssetType.COURSE_COVER);
		fileAsset.setStorageProvider(StorageProvider.DATABASE);
		fileAsset.setOwnerId(10L);
		return fileAsset;
	}

	private static final class FakeFileAssetRepository {

		private final Map<Long, FileAsset> byId = new LinkedHashMap<>();

		private FileAsset save(FileAsset fileAsset) {
			byId.put(fileAsset.getId(), fileAsset);
			return fileAsset;
		}

		private FileAssetRepository repository() {
			return (FileAssetRepository) Proxy.newProxyInstance(
				FileAssetRepository.class.getClassLoader(),
				new Class<?>[] {FileAssetRepository.class},
				(proxy, method, args) -> switch (method.getName()) {
					case "findByStorageKey" -> findByStorageKey((String) args[0]);
					case "toString" -> "FakeFileAssetRepository";
					default -> throw new UnsupportedOperationException(method.getName());
				}
			);
		}

		private Optional<FileAsset> findByStorageKey(String storageKey) {
			return byId.values()
				.stream()
				.filter(fileAsset -> storageKey.equals(fileAsset.getStorageKey()))
				.findFirst();
		}
	}

	private static final class FakeFileBlobRepository {

		private final Map<Long, FileBlob> byFileAssetId = new LinkedHashMap<>();

		private Optional<FileBlob> findByFileAssetId(Long fileAssetId) {
			return Optional.ofNullable(byFileAssetId.get(fileAssetId));
		}

		private FileBlobRepository repository() {
			return (FileBlobRepository) Proxy.newProxyInstance(
				FileBlobRepository.class.getClassLoader(),
				new Class<?>[] {FileBlobRepository.class},
				(proxy, method, args) -> switch (method.getName()) {
					case "save" -> save((FileBlob) args[0]);
					case "findByFileAssetId" -> findByFileAssetId((Long) args[0]);
					case "deleteByFileAssetId" -> {
						byFileAssetId.remove((Long) args[0]);
						yield null;
					}
					case "toString" -> "FakeFileBlobRepository";
					default -> throw new UnsupportedOperationException(method.getName());
				}
			);
		}

		private FileBlob save(FileBlob fileBlob) {
			byFileAssetId.put(fileBlob.getFileAssetId(), fileBlob);
			return fileBlob;
		}
	}
}
