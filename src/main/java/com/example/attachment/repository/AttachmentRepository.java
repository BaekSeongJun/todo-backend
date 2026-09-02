package com.example.attachment.repository;

import com.example.attachment.entity.Attachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    Optional<Attachment> findByIdAndTodo_UserId(Long id, Long userId);

    List<Attachment> findAllByTodoIdOrderByCreatedAtAsc(Long todoId);

    long countByTodoId(Long todoId);

    /** FR-F08 목록 배지용 배치 카운트. Todo N개당 개별 조회 대신 한 번의 쿼리로 N+1을 회피한다. */
    @Query(
            "SELECT a.todo.id AS todoId, COUNT(a.id) AS count FROM Attachment a"
                    + " WHERE a.todo.id IN :todoIds GROUP BY a.todo.id")
    List<TodoAttachmentCount> countByTodoIdIn(@Param("todoIds") List<Long> todoIds);

    interface TodoAttachmentCount {
        Long getTodoId();

        Long getCount();
    }
}
