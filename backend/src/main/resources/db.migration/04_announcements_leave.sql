USE myfschool;
SET NAMES utf8mb4;
-- DEMO ONLY: run on a new/empty MyFSchool schema, once, in filename order.
START TRANSACTION;
INSERT INTO announcements(id,school_id,author_user_id,title,content,status,published_at) VALUES
(1,1,1,'Lịch họp phụ huynh','Nhà trường tổ chức họp phụ huynh vào cuối tháng 11.','PUBLISHED','2026-11-01 08:00:00'),
(2,1,2,'Nhắc lịch kiểm tra Toán 9A1','Các em ôn tập chương 1 và chương 2.','PUBLISHED','2026-11-02 08:00:00'),
(3,1,3,'Sinh hoạt lớp 10A1','Thông báo lịch sinh hoạt cuối tuần.','PUBLISHED','2026-11-03 08:00:00');
INSERT INTO announcement_targets(announcement_id,target_type,school_id,class_id,user_id) VALUES
(1,'SCHOOL',1,NULL,NULL),(2,'CLASS',NULL,1,NULL),(3,'CLASS',NULL,2,NULL);
INSERT INTO announcement_reads(announcement_id,user_id,read_at) VALUES
(1,5,'2026-11-01 09:00:00'),(1,6,'2026-11-01 10:00:00'),(2,5,'2026-11-02 09:00:00');
INSERT INTO leave_requests(id,school_id,student_id,parent_id,class_id,start_date,end_date,reason,status,reviewed_by,reviewed_at,review_note) VALUES
(1,1,6,2,2,'2026-11-02','2026-11-02','Con bị sốt, xin nghỉ học.','APPROVED',3,'2026-11-01 20:00:00','Đã duyệt'),
(2,1,3,2,1,'2026-11-05','2026-11-05','Gia đình có việc riêng.','PENDING',NULL,NULL,NULL);
INSERT INTO leave_request_histories(leave_request_id,actor_user_id,old_status,new_status,note,changed_at) VALUES
(1,6,NULL,'PENDING','Phụ huynh tạo đơn','2026-11-01 18:00:00'),
(1,3,'PENDING','APPROVED','Giáo viên duyệt','2026-11-01 20:00:00'),
(2,6,NULL,'PENDING','Phụ huynh tạo đơn','2026-11-03 18:00:00');
-- No real FCM tokens: dummy token below is nonfunctional and must never be used for sending.
INSERT INTO device_tokens(user_id,token,platform,device_name,is_active) VALUES
(5,'DEMO_NOT_A_REAL_FCM_TOKEN_0001','ANDROID','Demo Android',0);

COMMIT;
