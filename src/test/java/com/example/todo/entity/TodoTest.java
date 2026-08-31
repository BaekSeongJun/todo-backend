package com.example.todo.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TodoTest {

    @Test
    void toggle_호출시_완료여부가_반전된다() {
        Todo todo = Todo.builder().title("제목").priority(Priority.MEDIUM).build();
        assertThat(todo.isCompleted()).isFalse();

        todo.toggle();
        assertThat(todo.isCompleted()).isTrue();

        todo.toggle();
        assertThat(todo.isCompleted()).isFalse();
    }

    @Test
    void update_호출시_필드가_모두_교체된다() {
        Todo todo =
                Todo.builder()
                        .title("원래 제목")
                        .content("원래 내용")
                        .dueDate(LocalDate.of(2026, 1, 1))
                        .priority(Priority.LOW)
                        .build();

        todo.update("새 제목", "새 내용", LocalDate.of(2026, 12, 31), Priority.HIGH);

        assertThat(todo.getTitle()).isEqualTo("새 제목");
        assertThat(todo.getContent()).isEqualTo("새 내용");
        assertThat(todo.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(todo.getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void update시_우선순위가_null이면_MEDIUM으로_대체된다() {
        Todo todo = Todo.builder().title("제목").priority(Priority.HIGH).build();

        todo.update("제목", null, null, null);

        assertThat(todo.getPriority()).isEqualTo(Priority.MEDIUM);
    }
}
