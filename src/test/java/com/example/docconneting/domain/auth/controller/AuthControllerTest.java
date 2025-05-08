package com.example.docconneting.domain.auth.controller;


import com.example.docconneting.common.config.JwtUtil;
import com.example.docconneting.common.filter.JwtFilter;
import com.example.docconneting.common.resolver.AuthUserArgumentResolver;
import com.example.docconneting.domain.alarm.service.AlarmService;
import com.example.docconneting.domain.auth.dto.request.UserRefreshTokenRequest;
import com.example.docconneting.domain.auth.dto.request.UserSignInRequest;
import com.example.docconneting.domain.auth.dto.request.UserSignUpRequest;
import com.example.docconneting.domain.auth.dto.response.UserRefreshTokenResponse;
import com.example.docconneting.domain.auth.dto.response.UserSignInResponse;
import com.example.docconneting.domain.auth.entity.AuthUser;
import com.example.docconneting.domain.auth.service.AuthService;
import com.example.docconneting.domain.user.enums.UserRole;
import com.example.docconneting.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(AuthController.class)
@TestPropertySource(properties = {
        "jwt.secret.key=5Gk6hibHDtKLFVk4NdBX039rvehSLNjfKsdXpm/pHsU="
})
@Import({JwtUtil.class, AuthUserArgumentResolver.class, JwtFilter.class})
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AlarmService alarmService;

    @MockitoBean
    private UserRepository userRepository;

    //JPA, @EntityListeners(AuditingEntityListener.class) 무시용 mock 삽입
    @MockitoBean
    private AuditorAware<String> auditorAware;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean(name = "elasticsearchMappingContext")
    private MappingContext<?, ?> elasticsearchMappingContext;

    @Test
    public void 회원가입() throws Exception {
        //given
        UserSignUpRequest request = new UserSignUpRequest();
        ReflectionTestUtils.setField(request, "email", "test@test.com");
        ReflectionTestUtils.setField(request, "password", "test");
        ReflectionTestUtils.setField(request, "username", "testpatient");
        ReflectionTestUtils.setField(request, "userRole", "PATIENT");

        // JSON 요청 파트
        MockMultipartFile requestDtoPart = new MockMultipartFile(
                "requestDto",
                "requestDto.json",
                "application/json",
                objectMapper.writeValueAsBytes(request)
        );

        // 이미지 파일, null임
        MockMultipartFile imagePart = new MockMultipartFile(
                "multipartFile",
                "",
                "application/octet-stream",
                new byte[0]
        );

        Map<String, String> message = new HashMap<>();
        message.put("message", "회원 가입이 성공적으로 됐습니다");

        given(authService.signUp(refEq(request), refEq(imagePart))).willReturn(message);

        //when & then
        mockMvc.perform(multipart("/api/v1/signup")
                        .file(requestDtoPart)
                        .file(imagePart)
                        .contentType("multipart/form-data"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.message").value(message.get("message")));
    }

    @Test
    public void 로그인() throws Exception {
        //given
        UserSignInRequest request = new UserSignInRequest();
        ReflectionTestUtils.setField(request, "email", "test@test.com");
        ReflectionTestUtils.setField(request, "password", "test");
        ReflectionTestUtils.setField(request,"fcmToken","testFcmToken");

        String accessToken = "access";
        String refreshToken = "refresh";

        given(authService.signIn(refEq(request))).willReturn(UserSignInResponse.of(accessToken, refreshToken));

        //when & then
        mockMvc.perform(post("/api/v1/signin")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").value(accessToken))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.refreshToken").value(refreshToken));
    }

    @Test
    public void accessToken_재발급() throws Exception {
        //given
        AuthUser authUser = AuthUser.of(1L, UserRole.PATIENT);

        UserRefreshTokenRequest request = new UserRefreshTokenRequest();
        ReflectionTestUtils.setField(request, "refreshToken", "refresh");

        String newAccessToken = jwtUtil.createToken(authUser.getId(),authUser.getUserRole());
        String newRefreshToken = jwtUtil.createRefreshToken(authUser.getId());

        given(authService.refreshAccessToken(any(AuthUser.class), any(UserRefreshTokenRequest.class)))
                .willReturn(UserRefreshTokenResponse.of(newAccessToken, newRefreshToken));

        //when & then
        mockMvc.perform(post("/api/v1/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").value(newAccessToken))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.refreshToken").value(newRefreshToken));

    }
}
