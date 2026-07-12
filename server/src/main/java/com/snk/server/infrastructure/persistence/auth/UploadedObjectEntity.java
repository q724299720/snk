package com.snk.server.infrastructure.persistence.auth;

import com.snk.server.infrastructure.persistence.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "uploaded_objects")
public class UploadedObjectEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "object_key", nullable = false, unique = true, length = 512) private String objectKey;
    @Column(name = "thumbnail_object_key", length = 512) private String thumbnailObjectKey;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_user_id", nullable = false) private UserEntity owner;
    @Column(name = "content_type", nullable = false, length = 128) private String contentType;
    @Column(name = "size_bytes", nullable = false) private long sizeBytes;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (createdAt == null) createdAt = OffsetDateTime.now(); }
    public void setObjectKey(String value) { objectKey = value; }
    public void setThumbnailObjectKey(String value) { thumbnailObjectKey = value; }
    public void setOwner(UserEntity value) { owner = value; }
    public void setContentType(String value) { contentType = value; }
    public void setSizeBytes(long value) { sizeBytes = value; }
}
