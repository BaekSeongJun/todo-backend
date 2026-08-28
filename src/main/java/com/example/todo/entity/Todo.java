package com.example.todo.entity;

import com.example.common.entity.BaseEntity;
import com.example.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
        name = "todos",
        indexes = {
            @Index(
                    name = "idx_todos_user_deleted_created",
                    columnList = "user_id, deleted_at, created_at"),
            @Index(
                    name = "idx_todos_user_deleted_due",
                    columnList = "user_id, deleted_at, due_date")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Todo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Builder
    private Todo(User user, String title, String content, LocalDate dueDate, Priority priority) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.completed = false;
        this.dueDate = dueDate;
        this.priority = priority != null ? priority : Priority.MEDIUM;
    }
}
