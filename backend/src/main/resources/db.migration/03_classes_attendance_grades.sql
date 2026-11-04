USE myfschool;
SET NAMES utf8mb4;
-- DEMO ONLY: run on a new/empty MyFSchool schema, once, in filename order.
START TRANSACTION;
INSERT INTO school_classes(id,school_id,academic_year_id,class_name,grade_level,homeroom_teacher_id) VALUES
(1,1,1,'9A1',9,1),(2,1,1,'10A1',10,2);
INSERT INTO class_enrollments(class_id,student_id,enrolled_at,status) VALUES
(1,1,'2026-09-01','ACTIVE'),(1,2,'2026-09-01','ACTIVE'),(1,3,'2026-09-01','ACTIVE'),
(2,4,'2026-09-01','ACTIVE'),(2,5,'2026-09-01','ACTIVE'),(2,6,'2026-09-01','ACTIVE');
INSERT INTO teaching_assignments(id,class_id,teacher_id,subject_id,semester_id) VALUES
(1,1,1,1,1),(2,1,3,3,1),(3,2,2,1,1),(4,2,3,3,1),(5,1,2,2,1);
INSERT INTO timetable_entries(id,teaching_assignment_id,day_of_week,period_number,shift,room,effective_from,effective_to) VALUES
(1,1,1,1,'MORNING','A101','2026-09-01','2027-01-15'),
(2,2,2,2,'MORNING','A101','2026-09-01','2027-01-15'),
(3,3,1,1,'MORNING','B201','2026-09-01','2027-01-15'),
(4,4,3,2,'MORNING','B201','2026-09-01','2027-01-15'),
(5,5,4,3,'MORNING','A101','2026-09-01','2027-01-15');
INSERT INTO attendance_sessions(id,class_id,teaching_assignment_id,timetable_entry_id,session_date,shift,period_number,session_type,status,created_by,completed_at) VALUES
(1,1,1,1,'2026-11-02','MORNING',1,'SUBJECT','COMPLETED',2,'2026-11-02 08:00:00'),
(2,2,3,3,'2026-11-02','MORNING',1,'SUBJECT','COMPLETED',3,'2026-11-02 08:00:00'),
(3,1,NULL,NULL,'2026-11-03','MORNING',NULL,'HOMEROOM','COMPLETED',2,'2026-11-03 08:00:00');
INSERT INTO attendance_records(session_id,student_id,status,marked_by,note) VALUES
(1,1,'PRESENT',2,NULL),(1,2,'LATE',2,'Đến muộn 10 phút'),(1,3,'ABSENT',2,'Phụ huynh chưa báo'),
(2,4,'PRESENT',3,NULL),(2,5,'PRESENT',3,NULL),(2,6,'EXCUSED',3,'Có đơn xin nghỉ'),
(3,1,'PRESENT',2,NULL),(3,2,'PRESENT',2,NULL),(3,3,'PRESENT',2,NULL);
INSERT INTO grade_items(id,teaching_assignment_id,title,grade_type,weight,max_score,assessment_date) VALUES
(1,1,'Kiểm tra thường xuyên 1','REGULAR',1,10,'2026-10-15'),
(2,1,'Kiểm tra giữa kỳ I','MIDTERM',2,10,'2026-10-30'),
(3,3,'Kiểm tra thường xuyên 1','REGULAR',1,10,'2026-10-20'),
(4,4,'Kiểm tra thường xuyên 1','REGULAR',1,10,'2026-10-21');
INSERT INTO student_grades(grade_item_id,student_id,score,comment,graded_by,graded_at) VALUES
(1,1,9.00,'Tốt',2,'2026-10-16 10:00:00'),(1,2,7.50,NULL,2,'2026-10-16 10:00:00'),
(1,3,8.00,NULL,2,'2026-10-16 10:00:00'),(2,1,8.50,NULL,2,'2026-11-01 10:00:00'),
(2,2,7.00,NULL,2,'2026-11-01 10:00:00'),(2,3,9.00,NULL,2,'2026-11-01 10:00:00'),
(3,4,8.00,NULL,3,'2026-10-21 10:00:00'),(3,5,9.50,NULL,3,'2026-10-21 10:00:00'),
(3,6,7.00,NULL,3,'2026-10-21 10:00:00'),(4,4,8.50,NULL,4,'2026-10-22 10:00:00');
INSERT INTO student_evaluations(student_id,semester_id,class_id,evaluated_by,academic_result,conduct_result,overall_comment,evaluated_at) VALUES
(1,1,1,2,'TỐT','TỐT','Chăm chỉ và chủ động.','2026-11-03 09:00:00'),
(4,1,2,3,'KHÁ','TỐT','Có tiến bộ trong học tập.','2026-11-03 09:00:00');
INSERT INTO student_remarks(student_id,teacher_id,class_id,semester_id,content,remark_date,visibility) VALUES
(1,1,1,1,'Tích cực phát biểu trong giờ Toán.','2026-11-02','PARENTS_VISIBLE'),
(2,1,1,1,'Cần đến lớp đúng giờ.','2026-11-02','PARENTS_VISIBLE'),
(4,2,2,1,'Có tiến bộ rõ rệt.','2026-11-03','PARENTS_VISIBLE');

COMMIT;
