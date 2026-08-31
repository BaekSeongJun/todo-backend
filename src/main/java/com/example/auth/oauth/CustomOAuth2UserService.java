package com.example.auth.oauth;

import com.example.user.entity.AuthProvider;
import com.example.user.entity.User;
import com.example.user.entity.UserRole;
import com.example.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String email = oAuth2User.getAttribute("email");
        String profileName = oAuth2User.getAttribute("name");
        String sub = oAuth2User.getAttribute("sub");

        findOrCreateUser(email, profileName, sub);

        return oAuth2User;
    }

    User findOrCreateUser(String email, String profileName, String sub) {
        return userRepository
                .findByEmailAndDeletedAtIsNull(email)
                .orElseGet(
                        () ->
                                userRepository.save(
                                        User.builder()
                                                .email(email)
                                                .password(null)
                                                .name(resolveName(profileName, email))
                                                .provider(AuthProvider.GOOGLE)
                                                .providerId(sub)
                                                .role(UserRole.USER)
                                                .enabled(true)
                                                .build()));
    }

    String resolveName(String profileName, String email) {
        if (profileName != null && !profileName.isBlank()) {
            return profileName;
        }
        return email.substring(0, email.indexOf('@'));
    }
}
