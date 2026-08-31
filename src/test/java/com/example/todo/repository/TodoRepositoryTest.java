package com.example.todo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todo.entity.Priority;
import com.example.todo.entity.Todo;
import com.example.user.entity.AuthProvider;
import com.example.user.entity.User;
import com.example.user.entity.UserRole;
import com.example.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
class TodoRepositoryTest {

    @Autowired private TodoRepository todoRepository;
    @Autowired private UserRepository userRepository;

    private User createUser(String email) {
        return userRepository.save(
                User.builder()
                        .email(email)
                        .password("encoded")
                        .name("테스터")
                        .provider(AuthProvider.LOCAL)
                        .role(UserRole.USER)
                        .enabled(true)
                        .build());
    }

    private Todo createTodo(User owner, String title, boolean completed) {
        Todo todo = Todo.builder().user(owner).title(title).priority(Priority.MEDIUM).build();
        if (completed) {
            todo.toggle();
        }
        return todoRepository.save(todo);
    }

    private Todo createTodo(User owner, String title, LocalDate dueDate) {
        Todo todo =
                Todo.builder()
                        .user(owner)
                        .title(title)
                        .dueDate(dueDate)
                        .priority(Priority.MEDIUM)
                        .build();
        return todoRepository.save(todo);
    }

    @Test
    void findByIdAndUserId_소유자가_다르면_조회되지_않는다() {
        User owner = createUser("owner-" + System.nanoTime() + "@test.com");
        User stranger = createUser("stranger-" + System.nanoTime() + "@test.com");
        Todo todo = createTodo(owner, "소유권 테스트", false);

        assertThat(todoRepository.findByIdAndUserId(todo.getId(), owner.getId())).isPresent();
        assertThat(todoRepository.findByIdAndUserId(todo.getId(), stranger.getId())).isEmpty();
    }

    @Test
    void findByIdAndUserId_삭제된_할일은_조회되지_않는다() {
        User owner = createUser("owner-del-" + System.nanoTime() + "@test.com");
        Todo todo = createTodo(owner, "삭제될 할 일", false);
        todo.markDeleted();
        todoRepository.save(todo);

        assertThat(todoRepository.findByIdAndUserId(todo.getId(), owner.getId())).isEmpty();
    }

    @Test
    void findAllByUserIdAndCompletedOptional_completed가_null이면_전체를_반환한다() {
        User owner = createUser("owner-all-" + System.nanoTime() + "@test.com");
        createTodo(owner, "미완료1", false);
        createTodo(owner, "완료1", true);

        Page<Todo> result =
                todoRepository.findAllByUserIdAndCompletedOptional(
                        owner.getId(), null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findAllByUserIdAndCompletedOptional_completed값에_따라_필터링된다() {
        User owner = createUser("owner-filter-" + System.nanoTime() + "@test.com");
        createTodo(owner, "미완료1", false);
        createTodo(owner, "완료1", true);
        createTodo(owner, "완료2", true);

        Page<Todo> completedOnly =
                todoRepository.findAllByUserIdAndCompletedOptional(
                        owner.getId(), true, PageRequest.of(0, 10));
        Page<Todo> pendingOnly =
                todoRepository.findAllByUserIdAndCompletedOptional(
                        owner.getId(), false, PageRequest.of(0, 10));

        assertThat(completedOnly.getTotalElements()).isEqualTo(2);
        assertThat(pendingOnly.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findAllByUserIdAndCompletedOptional_삭제된_할일은_제외된다() {
        User owner = createUser("owner-soft-" + System.nanoTime() + "@test.com");
        Todo todo = createTodo(owner, "삭제 예정", false);
        todo.markDeleted();
        todoRepository.save(todo);

        Page<Todo> result =
                todoRepository.findAllByUserIdAndCompletedOptional(
                        owner.getId(), null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void findAllByUserIdAndCompletedOptional_타인의_할일은_포함되지_않는다() {
        User owner = createUser("owner-scope-" + System.nanoTime() + "@test.com");
        User other = createUser("other-scope-" + System.nanoTime() + "@test.com");
        createTodo(owner, "내 할 일", false);
        createTodo(other, "남의 할 일", false);

        Page<Todo> result =
                todoRepository.findAllByUserIdAndCompletedOptional(
                        owner.getId(), null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findAllByUserIdAndCompletedOptional_dueDate_오름차순으로_정렬된다() {
        User owner = createUser("owner-sort-due-" + System.nanoTime() + "@test.com");
        createTodo(owner, "늦은 마감", LocalDate.of(2026, 12, 31));
        createTodo(owner, "이른 마감", LocalDate.of(2026, 1, 1));
        createTodo(owner, "중간 마감", LocalDate.of(2026, 6, 15));

        Page<Todo> result =
                todoRepository.findAllByUserIdAndCompletedOptional(
                        owner.getId(), null, PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "dueDate")));

        List<Todo> content = result.getContent();
        assertThat(content).hasSize(3);
        assertThat(content.get(0).getTitle()).isEqualTo("이른 마감");
        assertThat(content.get(1).getTitle()).isEqualTo("중간 마감");
        assertThat(content.get(2).getTitle()).isEqualTo("늦은 마감");
    }

    @Test
    void findAllByUserIdAndCompletedOptional_createdAt_내림차순이_기본_정렬이다() {
        User owner = createUser("owner-sort-created-" + System.nanoTime() + "@test.com");
        Todo first = createTodo(owner, "먼저 생성", false);
        Todo second = createTodo(owner, "나중 생성", false);

        Page<Todo> result =
                todoRepository.findAllByUserIdAndCompletedOptional(
                        owner.getId(),
                        null,
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<Todo> content = result.getContent();
        assertThat(content.get(0).getId()).isEqualTo(second.getId());
        assertThat(content.get(1).getId()).isEqualTo(first.getId());
    }
}
