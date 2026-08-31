package com.example.auth.oauth;

import com.example.auth.jwt.JwtTokenProvider;
import com.example.common.exception.UnauthorizedException;
import com.example.user.entity.User;
import com.example.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final OneTimeCodeStore oneTimeCodeStore;

    @Value("${app.oauth-redirect-url}")
    private String oauthRedirectUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        User user =
                userRepository
                        .findByEmailAndDeletedAtIsNull(email)
                        .orElseThrow(UnauthorizedException::new);

        String jwt = jwtTokenProvider.generateToken(user.getId(), user.getRole());
        String code = oneTimeCodeStore.issue(jwt);

        String targetUrl =
                UriComponentsBuilder.fromUriString(oauthRedirectUrl)
                        .queryParam("code", code)
                        .build()
                        .toUriString();
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
