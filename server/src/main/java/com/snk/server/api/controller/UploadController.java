package com.snk.server.api.controller;

import com.snk.server.api.dto.UploadImageResponse;
import com.snk.server.infrastructure.storage.ObjectStorageService;
import com.snk.server.infrastructure.storage.StoredObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

	private final ObjectStorageService objectStorageService;

	public UploadController(ObjectStorageService objectStorageService) {
		this.objectStorageService = objectStorageService;
	}

	@PostMapping(
		path = "/image",
		consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ResponseStatus(HttpStatus.CREATED)
	public UploadImageResponse uploadImage(@RequestPart("file") MultipartFile file) {
		StoredObject storedObject = objectStorageService.storeImage(file);
		return new UploadImageResponse(
			storedObject.objectKey(),
			storedObject.resourceUrl(),
			storedObject.thumbnailObjectKey(),
			storedObject.thumbnailUrl(),
			storedObject.contentType(),
			storedObject.size()
		);
	}
}
