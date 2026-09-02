package com.example.attachment.entity;

import com.example.todo.entity.Todo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

/**
 * attachments는 업로드 후 내용이 변경되지 않으므로(PRD 7장 예외) BaseEntity를 상속하지 않고
 * PasswordResetToken처럼 createdAt·deletedAt을 직접 선언한다.
 */
@Getter
@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
        name = "attachments",
        indexes = @Index(name = "idx_attachments_todo_deleted", columnList = "todo_id, deleted_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", nullable = false)
    private Todo todo;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "stored_key", nullable = false)
    private String storedKey;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private Attachment(
            Todo todo, String originalName, String storedKey, String contentType, Long sizeBytes) {
        this.todo = todo;
        this.originalName = originalName;
        this.storedKey = storedKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public static Attachment create(
            Todo todo, String originalName, String storedKey, String contentType, Long sizeBytes) {
        return new Attachment(todo, originalName, storedKey, contentType, sizeBytes);
    }

    public void markDeleted() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
