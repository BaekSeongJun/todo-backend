package com.example.todo.service;

import com.example.common.exception.TodoNotFoundException;
import com.example.common.exception.UnauthorizedException;
import com.example.todo.dto.TodoCreateRequest;
import com.example.todo.dto.TodoResponse;
import com.example.todo.dto.TodoUpdateRequest;
import com.example.todo.entity.Todo;
import com.example.todo.repository.TodoRepository;
import com.example.todo.util.HtmlSanitizer;
import com.example.user.entity.User;
import com.example.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final HtmlSanitizer sanitizer;

    @Transactional
    public TodoResponse create(Long userId, TodoCreateRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(UnauthorizedException::new);

        Todo todo =
                Todo.builder()
                        .user(user)
                        .title(request.title())
                        .content(sanitizer.sanitize(request.content()))
                        .dueDate(request.dueDate())
                        .priority(request.priority())
                        .build();

        return TodoResponse.from(todoRepository.save(todo));
    }

    public Page<TodoResponse> getList(Long userId, Boolean completed, Pageable pageable) {
        return todoRepository
                .findAllByUserIdAndCompletedOptional(userId, completed, pageable)
                .map(TodoResponse::from);
    }

    public TodoResponse getOne(Long userId, Long id) {
        return TodoResponse.from(getOwnedTodo(userId, id));
    }

    @Transactional
    public TodoResponse update(Long userId, Long id, TodoUpdateRequest request) {
        Todo todo = getOwnedTodo(userId, id);

        todo.update(
                request.title(),
                sanitizer.sanitize(request.content()),
                request.dueDate(),
                request.priority());

        return TodoResponse.from(todo);
    }

    @Transactional
    public TodoResponse toggle(Long userId, Long id) {
        Todo todo = getOwnedTodo(userId, id);
        todo.toggle();
        return TodoResponse.from(todo);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Todo todo = getOwnedTodo(userId, id);
        todo.markDeleted();
    }

    private Todo getOwnedTodo(Long userId, Long id) {
        return todoRepository.findByIdAndUserId(id, userId).orElseThrow(TodoNotFoundException::new);
    }
}
