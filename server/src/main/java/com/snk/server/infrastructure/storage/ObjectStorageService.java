package com.snk.server.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ObjectStorageService {

	StoredObject storeImage(MultipartFile file);
}
