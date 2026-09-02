package com.example.admin.service;

import com.example.admin.dto.AdminTodoResponse;
import com.example.admin.dto.StatsResponse;
import com.example.admin.dto.UserAdminResponse;
import com.example.common.exception.SelfStatusChangeException;
import com.example.common.exception.UserNotFoundException;
import com.example.todo.entity.Priority;
import com.example.todo.repository.TodoRepository;
import com.example.user.entity.User;
import com.example.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final TodoRepository todoRepository;

    public Page<UserAdminResponse> getUsers(String email, Pageable pageable) {
        Page<User> page =
                StringUtils.hasText(email)
                        ? userRepository.findAllByEmailContainingAndDeletedAtIsNull(email, pageable)
                        : userRepository.findAllByDeletedAtIsNull(pageable);

        return page.map(UserAdminResponse::from);
    }

    @Transactional
    public UserAdminResponse updateUserStatus(Long adminUserId, Long targetUserId, boolean enabled) {
        if (adminUserId.equals(targetUserId)) {
            throw new SelfStatusChangeException();
        }

        User user =
                userRepository.findByIdAndDeletedAtIsNull(targetUserId).orElseThrow(UserNotFoundException::new);
        user.changeEnabled(enabled);

        return UserAdminResponse.from(user);
    }

    public Page<AdminTodoResponse> getTodos(
            String title, String author, Boolean completed, Priority priority, Pageable pageable) {
        return todoRepository.findAllForAdmin(title, author, completed, priority, pageable);
    }

    public StatsResponse getStats() {
        long totalUsers = userRepository.countByDeletedAtIsNull();
        long activeUsers = userRepository.countByDeletedAtIsNullAndEnabledTrue();
        long totalTodos = todoRepository.countByDeletedAtIsNull();
        long completedTodos = todoRepository.countByDeletedAtIsNullAndCompletedTrue();

        return new StatsResponse(totalUsers, activeUsers, totalTodos, completedTodos);
    }
}
