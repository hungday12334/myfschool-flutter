package com.example.myfschool.repository;

import com.example.myfschool.entity.AnnouncementRead;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {
    Optional<AnnouncementRead> findByAnnouncement_IdAndUser_Id(Long announcementId, Long userId);
    List<AnnouncementRead> findByUser_IdAndAnnouncement_IdIn(Long userId, List<Long> announcementIds);

    @Modifying
    @Query(value = "INSERT INTO announcement_reads (announcement_id,user_id,read_at) VALUES (:announcementId,:userId,CURRENT_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE id=id", nativeQuery = true)
    int insertIfAbsent(@Param("announcementId") Long announcementId, @Param("userId") Long userId);
}
