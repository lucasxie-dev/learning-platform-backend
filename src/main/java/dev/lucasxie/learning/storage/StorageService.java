package dev.lucasxie.learning.storage;

public interface StorageService {

	StorageUploadResult upload(StorageUploadCommand command);

	StorageDownloadResult download(String storageKey);

	void delete(String storageKey);

	String getAccessUrl(String storageKey);
}
