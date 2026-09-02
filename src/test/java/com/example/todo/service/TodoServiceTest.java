package com.example.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.attachment.repository.AttachmentRepository;
import com.example.common.exception.TodoNotFoundException;
import com.example.todo.dto.TodoCreateRequest;
import com.example.todo.dto.TodoResponse;
import com.example.todo.dto.TodoUpdateRequest;
import com.example.todo.entity.Priority;
import com.example.todo.entity.Todo;
import com.example.todo.repository.TodoRepository;
import com.example.todo.util.HtmlSanitizer;
import com.example.user.entity.AuthProvider;
import com.example.user.entity.User;
import com.example.user.entity.UserRole;
import com.example.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class TodoServiceTest {

    private final TodoRepository todoRepository = Mockito.mock(TodoRepository.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final AttachmentRepository attachmentRepository = Mockito.mock(AttachmentRepository.class);
    private final HtmlSanitizer sanitizer = new HtmlSanitizer();
    private final TodoService todoService =
            new TodoService(todoRepository, userRepository, attachmentRepository, sanitizer);

    private User user(Long id) {
        User user =
                User.builder()
                        .email("user" + id + "@test.com")
                        .password("encoded")
                        .name("테스터")
                        .provider(AuthProvider.LOCAL)
                        .role(UserRole.USER)
                        .enabled(true)
                        .build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Todo todo(Long id, User owner) {
        Todo todo = Todo.builder().user(owner).title("제목").priority(Priority.MEDIUM).build();
        org.springframework.test.util.ReflectionTestUtils.setField(todo, "id", id);
        return todo;
    }

    @Test
    void create_content에_script태그가_있으면_정제되어_저장된다() {
        User owner = user(1L);
        Mockito.when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(owner));
        Mockito.when(todoRepository.save(Mockito.any(Todo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TodoCreateRequest request =
                new TodoCreateRequest(
                        "제목", "<script>alert(1)</script>본문", null, Priority.HIGH);

        TodoResponse response = todoService.create(1L, request);

        assertThat(response.content()).doesNotContain("script").contains("본문");
    }

    @Test
    void getOne_타인_소유_할일이면_TodoNotFoundException이_발생한다() {
        Mockito.when(todoRepository.findByIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.getOne(2L, 10L))
                .isInstanceOf(TodoNotFoundException.class);
    }

    @Test
    void update_타인_소유_할일이면_TodoNotFoundException이_발생한다() {
        Mockito.when(todoRepository.findByIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        TodoUpdateRequest request = new TodoUpdateRequest("새 제목", null, null, null);

        assertThatThrownBy(() -> todoService.update(2L, 10L, request))
                .isInstanceOf(TodoNotFoundException.class);
    }

    @Test
    void toggle_타인_소유_할일이면_TodoNotFoundException이_발생한다() {
        Mockito.when(todoRepository.findByIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.toggle(2L, 10L)).isInstanceOf(TodoNotFoundException.class);
    }

    @Test
    void delete_타인_소유_할일이면_TodoNotFoundException이_발생한다() {
        Mockito.when(todoRepository.findByIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.delete(2L, 10L)).isInstanceOf(TodoNotFoundException.class);
    }

    @Test
    void update_소유자가_맞으면_content가_정제되어_필드가_교체된다() {
        User owner = user(1L);
        Todo todo = todo(5L, owner);
        Mockito.when(todoRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(todo));

        TodoUpdateRequest request =
                new TodoUpdateRequest("새 제목", "<img src=x onerror=alert(1)>본문", null, Priority.LOW);

        TodoResponse response = todoService.update(1L, 5L, request);

        assertThat(response.title()).isEqualTo("새 제목");
        assertThat(response.content()).doesNotContain("img").contains("본문");
        assertThat(response.priority()).isEqualTo(Priority.LOW);
    }

    @Test
    void toggle_소유자가_맞으면_완료여부가_반전된다() {
        User owner = user(1L);
        Todo todo = todo(5L, owner);
        Mockito.when(todoRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(todo));

        TodoResponse response = todoService.toggle(1L, 5L);

        assertThat(response.completed()).isTrue();
    }

    @Test
    void delete_소유자가_맞으면_softDelete_상태가_된다() {
        User owner = user(1L);
        Todo todo = todo(5L, owner);
        Mockito.when(todoRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(todo));

        todoService.delete(1L, 5L);

        assertThat(todo.isDeleted()).isTrue();
    }

    @Test
    void getList_completed필터가_repository에_그대로_전달된다() {
        User owner = user(1L);
        Todo pending = todo(1L, owner);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Todo> page = new PageImpl<>(List.of(pending), pageable, 1);
        Mockito.when(todoRepository.findAllByUserIdAndFilters(1L, false, null, pageable))
                .thenReturn(page);

        Page<TodoResponse> result = todoService.getList(1L, false, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(1L);
    }
}
