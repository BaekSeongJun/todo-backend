package com.example.todo.repository;

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
                    + " AND (:completed IS NULL OR t.completed = :completed)")
    Page<Todo> findAllByUserIdAndCompletedOptional(
            @Param("userId") Long userId, @Param("completed") Boolean completed, Pageable pageable);
}
