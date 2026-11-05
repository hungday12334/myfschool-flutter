package com.example.myfschool;

import com.example.myfschool.dto.request.*;
import com.example.myfschool.entity.*;
import com.example.myfschool.entity.UserRole;
import com.example.myfschool.enums.*;
import com.example.myfschool.exception.BusinessException;
import com.example.myfschool.repository.*;
import com.example.myfschool.security.JwtService;
import com.example.myfschool.service.EmailService;
import com.example.myfschool.service.impl.AuthServiceImpl;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceWebTest {
    UserRepository users; UserRoleRepository roles; PasswordEncoder encoder; JwtService jwt; EmailService email; AuthServiceImpl service; User user;
    @BeforeEach void setup(){users=mock(UserRepository.class);roles=mock(UserRoleRepository.class);encoder=mock(PasswordEncoder.class);jwt=mock(JwtService.class);email=mock(EmailService.class);service=new AuthServiceImpl(users,roles,encoder,jwt,email);
        School school=new School();school.setStatus(SchoolStatus.ACTIVE);user=new User();user.setSchool(school);user.setUsername("web-user");user.setEmail("test@example.org");user.setPasswordHash("hash");user.setStatus(UserStatus.ACTIVE);
        Role role=new Role();role.setCode("TEACHER");UserRole ur=new UserRole();ur.setRole(role);when(roles.findByUser_IdOrderByIdAsc(null)).thenReturn(List.of(ur));when(encoder.matches("correct","hash")).thenReturn(true);when(jwt.getExpirationSeconds()).thenReturn(3600L);
    }
    @Test void webUsesUsernameAndDoesNotBreakFlutterPhoneLogin(){when(users.findByUsername("web-user")).thenReturn(Optional.of(user));when(users.findByPhone("0900000000")).thenReturn(Optional.of(user));assertEquals("web-user",service.login(new LoginRequest(" web-user ","correct","WEB")).user().username());assertEquals("web-user",service.login(new LoginRequest("0900000000","correct","FLUTTER")).user().username());}
    @Test void invalidCredentialsAndUnknownSourceAreRejected(){when(users.findByUsername("missing")).thenReturn(Optional.empty());assertThrows(BusinessException.class,()->service.login(new LoginRequest("missing","bad","WEB")));assertThrows(BusinessException.class,()->service.login(new LoginRequest("any","bad","OTHER")));}
    @Test void lockedAccountsCannotLogin(){when(users.findByUsername("web-user")).thenReturn(Optional.of(user));user.setStatus(UserStatus.LOCKED);assertThrows(BusinessException.class,()->service.login(new LoginRequest("web-user","correct","WEB")));}
    @Test void resetEmailUsesMockDeliveryAndTokenIsSingleUse(){when(users.findByEmailIgnoreCase("test@example.org")).thenReturn(Optional.of(user));when(encoder.encode(anyString())).thenAnswer(i->"hash:"+i.getArgument(0));service.requestPasswordReset(new PasswordResetRequest("test@example.org"));ArgumentCaptor<String> code=ArgumentCaptor.forClass(String.class);verify(email).sendPasswordResetCode(eq("test@example.org"),eq("web-user"),code.capture());when(encoder.matches(code.getValue(),"hash:"+code.getValue())).thenReturn(true);String token=service.verifyPasswordResetCode(new PasswordResetVerifyRequest("test@example.org",code.getValue())).resetToken();assertThrows(BusinessException.class,()->service.verifyPasswordResetCode(new PasswordResetVerifyRequest("test@example.org",code.getValue())));service.confirmPasswordReset(new PasswordResetConfirmRequest(token,"newPassword123","newPassword123"));assertFalse(user.isMustChangePassword());assertEquals("hash:newPassword123",user.getPasswordHash());assertThrows(BusinessException.class,()->service.confirmPasswordReset(new PasswordResetConfirmRequest(token,"newPassword123","newPassword123")));}
    @Test void unknownEmailDoesNotSendAndInvalidCodesTokensAreRejected(){when(users.findByEmailIgnoreCase("unknown@example.org")).thenReturn(Optional.empty());assertDoesNotThrow(()->service.requestPasswordReset(new PasswordResetRequest("unknown@example.org")));verifyNoInteractions(email);assertThrows(BusinessException.class,()->service.verifyPasswordResetCode(new PasswordResetVerifyRequest("unknown@example.org","123456")));assertThrows(BusinessException.class,()->service.confirmPasswordReset(new PasswordResetConfirmRequest("expired-or-unknown","newPassword123","newPassword123")));}
    @Test void bcryptByteLimitIsCheckedBeforePersistence(){String password="ầ".repeat(30);assertThrows(BusinessException.class,()->service.confirmPasswordReset(new PasswordResetConfirmRequest("token",password,password)));verify(users,never()).save(any());}
}
