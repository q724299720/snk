package com.snk.server.api.controller;

import com.snk.server.api.dto.UploadImageResponse;
import com.snk.server.infrastructure.storage.ObjectStorageService;
import com.snk.server.infrastructure.storage.StoredObject;
import com.snk.server.infrastructure.persistence.auth.UploadedObjectEntity;
import com.snk.server.infrastructure.persistence.auth.UploadedObjectRepository;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import com.snk.server.infrastructure.security.CurrentUser;
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
	private final UploadedObjectRepository uploadedObjects;
	private final UserRepository users;
	private final CurrentUser currentUser;

	public UploadController(ObjectStorageService objectStorageService, UploadedObjectRepository uploadedObjects, UserRepository users, CurrentUser currentUser) {
		this.objectStorageService = objectStorageService;
		this.uploadedObjects = uploadedObjects;
		this.users = users;
		this.currentUser = currentUser;
	}

	@PostMapping(
		path = "/image",
		consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ResponseStatus(HttpStatus.CREATED)
	public UploadImageResponse uploadImage(@RequestPart("file") MultipartFile file) {
		StoredObject storedObject = objectStorageService.storeImage(file);
		UploadedObjectEntity metadata = new UploadedObjectEntity();
		metadata.setObjectKey(storedObject.objectKey());
		metadata.setThumbnailObjectKey(storedObject.thumbnailObjectKey());
		metadata.setOwner(users.getReferenceById(currentUser.requiredUserId()));
		metadata.setContentType(storedObject.contentType());
		metadata.setSizeBytes(storedObject.size());
		uploadedObjects.save(metadata);
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
