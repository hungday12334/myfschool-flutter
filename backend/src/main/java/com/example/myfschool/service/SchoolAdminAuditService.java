package com.example.myfschool.service;

import com.example.myfschool.entity.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Metadata only: never persist account credentials or raw request bodies. */
@Service
@RequiredArgsConstructor
public class SchoolAdminAuditService {
    private final EntityManager em;
    @Transactional(propagation=Propagation.MANDATORY)
    public void record(User actor,String action,String entity,Long id){AuditLog l=new AuditLog();l.setActor(actor);l.setSchool(actor.getSchool());l.setAction(action);l.setEntityType(entity);l.setEntityId(id);em.persist(l);}
}
