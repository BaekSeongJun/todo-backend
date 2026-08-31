package com.example.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.user.entity.AuthProvider;
import com.example.user.entity.User;
import com.example.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CustomOAuth2UserServiceTest {

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final CustomOAuth2UserService service = new CustomOAuth2UserService(userRepository);

    @Test
    void 이름이_있으면_구글_프로필_이름을_사용한다() {
        assertThat(service.resolveName("홍길동", "hong@example.com")).isEqualTo("홍길동");
    }

    @Test
    void 이름이_없으면_이메일_로컬파트를_사용한다() {
        assertThat(service.resolveName(null, "hong@example.com")).isEqualTo("hong");
        assertThat(service.resolveName("", "hong@example.com")).isEqualTo("hong");
        assertThat(service.resolveName("   ", "hong@example.com")).isEqualTo("hong");
    }

    @Test
    void 신규_이메일이면_provider가_GOOGLE인_User를_생성한다() {
        Mockito.when(userRepository.findByEmailAndDeletedAtIsNull("new@example.com"))
                .thenReturn(Optional.empty());
        Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.findOrCreateUser("new@example.com", "New User", "google-sub-1");

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getName()).isEqualTo("New User");
        assertThat(result.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(result.getProviderId()).isEqualTo("google-sub-1");
        assertThat(result.getPassword()).isNull();
        assertThat(result.isEnabled()).isTrue();
        Mockito.verify(userRepository).save(Mockito.any(User.class));
    }

    @Test
    void 기존_로컬_계정과_이메일이_같으면_신규_생성하지_않고_기존_계정을_반환한다() {
        User existingLocalUser =
                User.builder()
                        .email("existing@example.com")
                        .password("encoded-password")
                        .name("기존사용자")
                        .provider(AuthProvider.LOCAL)
                        .providerId(null)
                        .build();
        Mockito.when(userRepository.findByEmailAndDeletedAtIsNull("existing@example.com"))
                .thenReturn(Optional.of(existingLocalUser));

        User result = service.findOrCreateUser("existing@example.com", "Existing User", "google-sub-2");

        assertThat(result).isSameAs(existingLocalUser);
        assertThat(result.getProvider()).isEqualTo(AuthProvider.LOCAL);
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any(User.class));
    }
}
