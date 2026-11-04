USE myfschool;
SET NAMES utf8mb4;
-- DEMO ONLY: run on a new/empty MyFSchool schema, once, in filename order.
START TRANSACTION;
-- 1. School and calendar
INSERT INTO schools(id,school_code,name,school_level,address,status) VALUES (1,'MFS-DEMO','Trường THCS & THPT MyFSchool Demo','COMBINED','Hà Nội','ACTIVE');
INSERT INTO academic_years(id,school_id,name,start_date,end_date,is_current) VALUES (1,1,'2026-2027','2026-09-01','2027-05-31',1);
INSERT INTO semesters(id,academic_year_id,semester_number,name,start_date,end_date) VALUES (1,1,1,'Học kỳ I','2026-09-01','2027-01-15'),(2,1,2,'Học kỳ II','2027-01-16','2027-05-31');
INSERT INTO subjects(id,school_id,subject_code,name) VALUES (1,1,'MATH','Toán'),(2,1,'LIT','Ngữ văn'),(3,1,'ENG','Tiếng Anh'),(4,1,'PHY','Vật lý');
INSERT INTO roles(id,code,name) VALUES (1,'SCHOOL_ADMIN','Quản trị trường'),(2,'TEACHER','Giáo viên'),(3,'PARENT','Phụ huynh'),(4,'STUDENT','Học sinh');
-- DEMO password for ALL accounts: Password@123 (BCrypt). Change immediately outside local dev.
INSERT INTO users(id,school_id,username,password_hash,email,status,must_change_password) VALUES (1,1,'admin_demo','$2y$10$fN31K.BS8P27G.ZhTlOS.eaYpi1v48Yu6gjlHU./9kzTpFiMfCP.m','admin@myfschool.test','ACTIVE',1);
INSERT INTO users(id,school_id,username,password_hash,email,status,must_change_password) VALUES (2,1,'GV0001','$2y$10$fN31K.BS8P27G.ZhTlOS.eaYpi1v48Yu6gjlHU./9kzTpFiMfCP.m','lan@myfschool.test','ACTIVE',1);
INSERT INTO users(id,school_id,username,password_hash,email,status,must_change_password) VALUES (3,1,'GV0002','$2y$10$fN31K.BS8P27G.ZhTlOS.eaYpi1v48Yu6gjlHU./9kzTpFiMfCP.m','minh@myfschool.test','ACTIVE',1);
INSERT INTO users(id,school_id,username,password_hash,email,status,must_change_password) VALUES (4,1,'GV0003','$2y$10$fN31K.BS8P27G.ZhTlOS.eaYpi1v48Yu6gjlHU./9kzTpFiMfCP.m','hoa@myfschool.test','ACTIVE',1);
INSERT INTO users(id,school_id,username,password_hash,email,status,must_change_password) VALUES (5,1,'PH0001','$2y$10$fN31K.BS8P27G.ZhTlOS.eaYpi1v48Yu6gjlHU./9kzTpFiMfCP.m','ph1@myfschool.test','ACTIVE',1);
INSERT INTO users(id,school_id,username,password_hash,email,status,must_change_password) VALUES (6,1,'PH0002','$2y$10$fN31K.BS8P27G.ZhTlOS.eaYpi1v48Yu6gjlHU./9kzTpFiMfCP.m','ph2@myfschool.test','ACTIVE',1);
INSERT INTO users(id,school_id,username,password_hash,email,status,must_change_password) VALUES (7,1,'PH0003','$2y$10$fN31K.BS8P27G.ZhTlOS.eaYpi1v48Yu6gjlHU./9kzTpFiMfCP.m','ph3@myfschool.test','ACTIVE',1);
INSERT INTO users(id,school_id,username,password_hash,email,status,must_change_password) VALUES (8,1,'PH0004','$2y$10$fN31K.BS8P27G.ZhTlOS.eaYpi1v48Yu6gjlHU./9kzTpFiMfCP.m','ph4@myfschool.test','ACTIVE',1);
INSERT INTO user_roles(user_id,role_id) VALUES (1,1);
INSERT INTO user_roles(user_id,role_id) VALUES (2,2);
INSERT INTO user_roles(user_id,role_id) VALUES (3,2);
INSERT INTO user_roles(user_id,role_id) VALUES (4,2);
INSERT INTO user_roles(user_id,role_id) VALUES (5,3);
INSERT INTO user_roles(user_id,role_id) VALUES (6,3);
INSERT INTO user_roles(user_id,role_id) VALUES (7,3);
INSERT INTO user_roles(user_id,role_id) VALUES (8,3);
INSERT INTO teachers(id,school_id,user_id,teacher_code,full_name,status) VALUES
(1,1,2,'GV0001','Nguyễn Thị Lan','ACTIVE'),(2,1,3,'GV0002','Trần Văn Minh','ACTIVE'),(3,1,4,'GV0003','Lê Thị Hoa','ACTIVE');
INSERT INTO parents(id,school_id,user_id,full_name,phone) VALUES
(1,1,5,'Nguyễn Văn Bình','0900000001'),(2,1,6,'Trần Thị Mai','0900000002'),(3,1,7,'Phạm Văn Nam','0900000003'),(4,1,8,'Đỗ Thị Thu','0900000004');
INSERT INTO students(id,school_id,user_id,student_code,full_name,date_of_birth,gender,admission_date,status) VALUES
(1,1,NULL,'HS0001','Nguyễn Minh Anh','2011-05-10','FEMALE','2026-09-01','ACTIVE'),
(2,1,NULL,'HS0002','Nguyễn Đức Anh','2011-11-12','MALE','2026-09-01','ACTIVE'),
(3,1,NULL,'HS0003','Trần Bảo Ngọc','2011-04-22','FEMALE','2026-09-01','ACTIVE'),
(4,1,NULL,'HS0004','Phạm Gia Huy','2010-07-19','MALE','2026-09-01','ACTIVE'),
(5,1,NULL,'HS0005','Đỗ Khánh Linh','2010-03-08','FEMALE','2026-09-01','ACTIVE'),
(6,1,NULL,'HS0006','Trần Quốc Bảo','2010-12-05','MALE','2026-09-01','ACTIVE');
INSERT INTO parent_students(parent_id,student_id,relationship_type,is_primary_contact) VALUES
(1,1,'FATHER',1),(1,2,'FATHER',1),(2,3,'MOTHER',1),(2,6,'MOTHER',1),
(3,4,'FATHER',1),(4,5,'MOTHER',1);

COMMIT;
