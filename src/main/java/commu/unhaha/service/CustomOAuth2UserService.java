package commu.unhaha.service;

import commu.unhaha.controller.SessionConst;
import commu.unhaha.domain.User;
import commu.unhaha.dto.OAuthAttributes;
import commu.unhaha.dto.SessionUser;
import commu.unhaha.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {


    private final UserRepository userRepository;
    private final HttpSession httpSession;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();

        String tokenValue = userRequest.getAccessToken().getTokenValue();
        log.info("tokenValue ={}", tokenValue);
        String clientSecret = userRequest.getClientRegistration().getClientSecret();
        String clientId = userRequest.getClientRegistration().getClientId();
        log.info("clientId={}", clientId);
        log.info("clientSecret={}", clientSecret);

        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        //서비스 구분을 위한 작업 (구글: google, 네이버: naver)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("registrationId = " + registrationId);

        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();
        log.info("userNameAttributeName = " + userNameAttributeName);

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());
        log.info("oAuth2User.getAttributes()={}", oAuth2User.getAttributes());
        log.info("attributes.getAttributes() = " + attributes.getAttributes());

        User user = saveOrUpdate(attributes);

        SessionUser sessionUser = new SessionUser(user);

        //세션 설정
        httpSession.setAttribute(SessionConst.LOGIN_USER, sessionUser);


        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey()
        );
    }

    private User saveOrUpdate(OAuthAttributes attributes) {

        Optional<User> findUser = userRepository.findByEmail(attributes.getEmail());
        // DB에 User가 존재하지 않을 경우 6자리 랜덤 닉네임 생성
        if (findUser.isEmpty()){
            String randomNickname = RandomStringUtils.randomAlphanumeric(6);
            while (userRepository.findByNickname(randomNickname).isPresent()) {
                randomNickname = RandomStringUtils.randomAlphanumeric(6);
            }
            attributes.setNickname(randomNickname);
            User user = attributes.toEntity();
            return userRepository.save(user);
        }
        else {
            User user = findUser.get();
            return userRepository.save(user);
        }

    }


}
