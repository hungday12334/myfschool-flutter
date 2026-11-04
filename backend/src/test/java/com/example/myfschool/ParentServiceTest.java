package com.example.myfschool;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.myfschool.entity.*;
import com.example.myfschool.dto.request.ParentLeaveRequest;
import com.example.myfschool.dto.request.ParentDeviceRequest;
import com.example.myfschool.enums.DevicePlatform;
import com.example.myfschool.exception.BusinessException;
import com.example.myfschool.exception.ErrorCode;
import com.example.myfschool.repository.*;
import com.example.myfschool.service.ParentService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParentServiceTest {
    @Mock ParentRepository parents;
    @Mock ParentStudentRepository links;
    @Mock LeaveRequestRepository leaveRequests;
    @Mock DeviceTokenRepository devices;
    @Mock AnnouncementReadRepository reads;
    @Mock LeaveRequestHistoryRepository histories;
    @Mock EntityManager em;
    ParentService service;
    Parent parent;
    School school;

    @BeforeEach void setUp() {
        service = new ParentService(parents, links, leaveRequests, devices, reads, histories);
        school = new School(); school.setId(10L); school.setName("School A"); school.setSchoolCode("A");
        parent = new Parent(); parent.setId(20L); parent.setSchool(school); parent.setFullName("Parent");
        when(parents.findByUser_Id(7L)).thenReturn(Optional.of(parent));
        try {
            var field=ParentService.class.getDeclaredField("em"); field.setAccessible(true); field.set(service,em);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    @Test void parentCannotAccessUnlinkedStudent() {
        when(links.findByParent_IdAndStudent_Id(20L, 99L)).thenReturn(Optional.empty());
        BusinessException error=assertThrows(BusinessException.class,()->service.childContext(7L,99L));
        assertEquals(ErrorCode.ACCESS_DENIED,error.getErrorCode());
    }

    @Test void parentCannotAccessLinkedStudentFromDifferentSchool() {
        School other=new School();other.setId(11L);
        Student student=new Student();student.setId(99L);student.setSchool(other);
        ParentStudent link=new ParentStudent();link.setParent(parent);link.setStudent(student);
        when(links.findByParent_IdAndStudent_Id(20L,99L)).thenReturn(Optional.of(link));
        BusinessException error=assertThrows(BusinessException.class,()->service.childContext(7L,99L));
        assertEquals(ErrorCode.ACCESS_DENIED,error.getErrorCode());
    }

    @Test void parentWithNoLinkedChildrenGetsEmptyList() {
        when(links.findByParent_IdOrderByStudent_FullNameAsc(20L)).thenReturn(List.of());
        assertTrue(service.children(7L).isEmpty());
    }

    @Test void parentCanResolveOwnChildContext() {
        Student student=new Student();student.setId(99L);student.setSchool(school);student.setStudentCode("S99");student.setFullName("Student");
        ParentStudent link=new ParentStudent();link.setParent(parent);link.setStudent(student);
        when(links.findByParent_IdAndStudent_Id(20L,99L)).thenReturn(Optional.of(link));
        @SuppressWarnings("unchecked") TypedQuery<ClassEnrollment> query=mock(TypedQuery.class);
        when(em.createQuery(anyString(),eq(ClassEnrollment.class))).thenReturn(query);
        when(query.setParameter(anyString(),any())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
        var dto=service.childContext(7L,99L);
        assertEquals("S99",dto.get("studentCode"));
        assertEquals(99L,dto.get("id"));
    }

    @Test void parentCanSwitchBetweenTwoAuthorizedChildren() {
        Student first=new Student();first.setId(101L);first.setSchool(school);first.setStudentCode("S101");first.setFullName("First");
        Student second=new Student();second.setId(102L);second.setSchool(school);second.setStudentCode("S102");second.setFullName("Second");
        ParentStudent firstLink=new ParentStudent();firstLink.setParent(parent);firstLink.setStudent(first);
        ParentStudent secondLink=new ParentStudent();secondLink.setParent(parent);secondLink.setStudent(second);
        when(links.findByParent_IdAndStudent_Id(20L,101L)).thenReturn(Optional.of(firstLink));
        when(links.findByParent_IdAndStudent_Id(20L,102L)).thenReturn(Optional.of(secondLink));
        @SuppressWarnings("unchecked") TypedQuery<ClassEnrollment> query=mock(TypedQuery.class);
        when(em.createQuery(anyString(),eq(ClassEnrollment.class))).thenReturn(query);
        when(query.setParameter(anyString(),any())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
        assertEquals(101L,service.childContext(7L,101L).get("id"));
        assertEquals(102L,service.childContext(7L,102L).get("id"));
    }

    @Test void invalidLeaveDateRangeIsRejected() {
        Student child=new Student();child.setId(99L);child.setSchool(school);
        ParentStudent link=new ParentStudent();link.setParent(parent);link.setStudent(child);link.setCanSubmitLeave(true);
        when(links.findByParent_IdAndStudent_Id(20L,99L)).thenReturn(Optional.of(link));
        var error=assertThrows(BusinessException.class,()->service.submitLeave(7L,99L,new ParentLeaveRequest(java.time.LocalDate.parse("2026-11-12"),java.time.LocalDate.parse("2026-11-10"),"illness")));
        assertEquals(ErrorCode.INVALID_REQUEST,error.getErrorCode());
        verify(leaveRequests,never()).save(any());
    }

    @Test void deviceTokenCannotBeClaimedFromAnotherAccount() {
        User owner=new User();owner.setId(88L);
        DeviceToken existing=new DeviceToken();existing.setId(8L);existing.setUser(owner);
        when(devices.findByToken("sensitive-token")).thenReturn(Optional.of(existing));
        var error=assertThrows(BusinessException.class,()->service.registerDevice(7L,new ParentDeviceRequest("sensitive-token",DevicePlatform.ANDROID,"phone")));
        assertEquals(ErrorCode.RESOURCE_CONFLICT,error.getErrorCode());
        verify(devices,never()).save(any());
    }
}
