package com.example.todo.repository;

import com.example.admin.dto.AdminTodoResponse;
import com.example.todo.entity.Priority;
import com.example.todo.entity.Todo;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    Optional<Todo> findByIdAndUserId(Long id, Long userId);

    @Query(
            "SELECT t FROM Todo t WHERE t.user.id = :userId"
                    + " AND (:completed IS NULL OR t.completed = :completed)"
                    + " AND (:title IS NULL OR t.title LIKE %:title%)")
    Page<Todo> findAllByUserIdAndFilters(
            @Param("userId") Long userId,
            @Param("completed") Boolean completed,
            @Param("title") String title,
            Pageable pageable);

    @Query(
            "SELECT new com.example.admin.dto.AdminTodoResponse("
                    + "t.id, t.title, t.completed, t.dueDate, t.priority,"
                    + " u.id, u.email, u.name, t.createdAt)"
                    + " FROM Todo t JOIN t.user u"
                    + " WHERE t.deletedAt IS NULL AND u.deletedAt IS NULL"
                    + " AND (:title IS NULL OR t.title LIKE %:title%)"
                    + " AND (:author IS NULL OR u.name LIKE %:author% OR u.email LIKE %:author%)"
                    + " AND (:completed IS NULL OR t.completed = :completed)"
                    + " AND (:priority IS NULL OR t.priority = :priority)")
    Page<AdminTodoResponse> findAllForAdmin(
            @Param("title") String title,
            @Param("author") String author,
            @Param("completed") Boolean completed,
            @Param("priority") Priority priority,
            Pageable pageable);

    long countByDeletedAtIsNull();

    long countByDeletedAtIsNullAndCompletedTrue();
}
