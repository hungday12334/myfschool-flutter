package com.example.myfschool.service;

import com.example.myfschool.dto.AdminAccountDtos.*;
import com.example.myfschool.entity.*;
import com.example.myfschool.entity.UserRole;
import com.example.myfschool.enums.*;
import com.example.myfschool.exception.*;
import jakarta.persistence.*;
import jakarta.validation.Validator;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class SchoolAdminAccountService {
    private final EntityManager em;
    private final PasswordEncoder encoder;
    private final Validator validator;
    private final SchoolAdminAuditService audit;
    public User admin(Long uid) {
        User u=em.find(User.class,uid);
        if(u==null || u.getStatus()!=UserStatus.ACTIVE || u.getSchool().getStatus()!=SchoolStatus.ACTIVE
            || em.createQuery("select count(r) from UserRole r where r.user.id=:u and r.role.code='SCHOOL_ADMIN'",Long.class).setParameter("u",uid).getSingleResult()==0) fail(ErrorCode.ACCESS_DENIED);
        return u;
    }
    private User target(User a,Long id) {
        User u=em.find(User.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(u==null) fail(ErrorCode.RESOURCE_NOT_FOUND);
        if(!u.getSchool().getId().equals(a.getSchool().getId())) fail(ErrorCode.ACCESS_DENIED);
        return u;
    }
    public Account view(User u) {
        return new Account(u.getId(),u.getUsername(),u.getEmail(),u.getPhone(),u.getStatus(),u.isMustChangePassword(),
            new HashSet<>(em.createQuery("select r.role.code from UserRole r where r.user.id=:u",String.class).setParameter("u",u.getId()).getResultList()));
    }
    @Transactional
    public Account create(Long uid,CreateTeacher r) {
        User a=admin(uid);
        if(!validator.validate(r).isEmpty() || !r.username().equals(r.username().trim()) || r.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) fail(ErrorCode.INVALID_REQUEST);
        em.lock(a.getSchool(),LockModeType.PESSIMISTIC_WRITE);
        if(em.createQuery("select count(u) from User u where lower(u.username)=:n",Long.class).setParameter("n",r.username().toLowerCase(Locale.ROOT)).getSingleResult()>0
            || em.createQuery("select count(t) from Teacher t where t.school.id=:s and lower(t.teacherCode)=:c",Long.class).setParameter("s",a.getSchool().getId()).setParameter("c",r.teacherCode().trim().toLowerCase(Locale.ROOT)).getSingleResult()>0) fail(ErrorCode.RESOURCE_CONFLICT);
        Role role=em.createQuery("select r from Role r where r.code='TEACHER'",Role.class).getResultStream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        User u=new User(); u.setSchool(a.getSchool());u.setUsername(r.username());u.setPasswordHash(encoder.encode(r.password()));u.setEmail(r.email());u.setPhone(r.phone());u.setStatus(UserStatus.ACTIVE);u.setMustChangePassword(true);em.persist(u);
        Teacher t=new Teacher();t.setSchool(a.getSchool());t.setUser(u);t.setTeacherCode(r.teacherCode().trim());t.setFullName(r.fullName().trim());t.setEmail(r.email());t.setPhone(r.phone());t.setStatus(TeacherStatus.ACTIVE);em.persist(t);
        UserRole ur=new UserRole();ur.setUser(u);ur.setRole(role);em.persist(ur);em.flush();audit.record(a,"CREATE_TEACHER_ACCOUNT","User",u.getId());return view(u);
    }
    public Page<Account> list(Long uid,String q,UserStatus status,int page,int size) {
        User a=admin(uid);page(page,size);String key=q==null?"":q.trim().toLowerCase(Locale.ROOT);if(key.length()>100)fail(ErrorCode.INVALID_REQUEST);
        String w=" where u.school.id=:s and (:status is null or u.status=:status) and locate(:q,lower(u.username))>0";
        var query=em.createQuery("select u from User u"+w+" order by u.id",User.class);var count=em.createQuery("select count(u) from User u"+w,Long.class);
        for(var x:List.of(query,count))x.setParameter("s",a.getSchool().getId()).setParameter("status",status).setParameter("q",key);
        return new PageImpl<>(query.setFirstResult(page*size).setMaxResults(size).getResultList().stream().map(this::view).toList(),PageRequest.of(page,size),count.getSingleResult());
    }
    @Transactional public Account update(Long uid,Long id,UpdateAccount r) {
        User a=admin(uid);User u=target(a,id);audit.record(a,"UPDATE_ACCOUNT","User",id);u.setEmail(r.email());u.setPhone(r.phone());
        var ts=em.createQuery("select t from Teacher t where t.user.id=:u",Teacher.class).setParameter("u",id).getResultList();
        for(Teacher t:ts){t.setEmail(r.email());t.setPhone(r.phone());if(r.fullName()!=null){if(r.fullName().isBlank())fail(ErrorCode.INVALID_REQUEST);t.setFullName(r.fullName().trim());}}
        return view(u);
    }
    @Transactional public Account status(Long uid,Long id,UserStatus status) {
        User a=admin(uid);User u=target(a,id);if(uid.equals(id))fail(ErrorCode.RESOURCE_CONFLICT);u.setStatus(status);audit.record(a,"CHANGE_ACCOUNT_STATUS","User",id);return view(u);
    }
    @Transactional public void reset(Long uid,Long id,String password) {
        User a=admin(uid);User u=target(a,id);if(password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)fail(ErrorCode.INVALID_REQUEST);u.setPasswordHash(encoder.encode(password));u.setMustChangePassword(true);audit.record(a,"RESET_PASSWORD","User",id);
    }
    @Transactional public Account roles(Long uid,Long id,Set<String> codes) {
        User a=admin(uid);User u=target(a,id);if(uid.equals(id))fail(ErrorCode.RESOURCE_CONFLICT);audit.record(a,"CHANGE_ROLES","User",id);
        if(!Set.of("TEACHER","PARENT","STUDENT","SCHOOL_ADMIN").containsAll(codes))fail(ErrorCode.INVALID_REQUEST);
        for(String code:codes) {
            String entity=switch(code){case "TEACHER"->"Teacher";case "PARENT"->"Parent";case "STUDENT"->"Student";default->null;};
            if(entity!=null && em.createQuery("select count(x) from "+entity+" x where x.user.id=:u and x.school.id=:s",Long.class).setParameter("u",id).setParameter("s",u.getSchool().getId()).getSingleResult()==0)fail(ErrorCode.INVALID_REQUEST);
        }
        var roles=em.createQuery("select r from Role r where r.code in :codes",Role.class).setParameter("codes",codes).getResultList();if(roles.size()!=codes.size())fail(ErrorCode.INVALID_REQUEST);
        em.createQuery("delete from UserRole r where r.user.id=:u").setParameter("u",id).executeUpdate();
        for(Role r:roles){UserRole ur=new UserRole();ur.setUser(u);ur.setRole(r);em.persist(ur);}em.flush();return view(u);
    }
    public Map<String,Long> dashboard(Long uid) {
        User a=admin(uid);Map<String,Long> result=new LinkedHashMap<>();
        for(String e:List.of("User","Teacher","Student","SchoolClass"))result.put(e,em.createQuery("select count(x) from "+e+" x where x.school.id=:s",Long.class).setParameter("s",a.getSchool().getId()).getSingleResult());return result;
    }
    public static void page(int p,int s){if(p<0||s<1||s>100||(long)p*s>Integer.MAX_VALUE)fail(ErrorCode.INVALID_REQUEST);}
    public static void fail(ErrorCode c){throw new BusinessException(c);}
}
