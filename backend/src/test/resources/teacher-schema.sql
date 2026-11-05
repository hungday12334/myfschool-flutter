-- MyFSchool - MySQL 8.0.16+ initial schema
-- 30 domain tables; safe CREATE IF NOT EXISTS, no DROP, no seed data.
-- Execute on a new database. Existing tables are NOT migrated by this script.




-- 1. SCHOOL & ACADEMIC STRUCTURE
CREATE TABLE IF NOT EXISTS schools (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 school_code VARCHAR(40) NOT NULL,
 name VARCHAR(255) NOT NULL,
 school_level ENUM('SECONDARY','HIGH_SCHOOL','COMBINED') NOT NULL DEFAULT 'COMBINED',
 address VARCHAR(500) NULL, phone VARCHAR(20) NULL, email VARCHAR(255) NULL,
 status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_school_code (school_code)
);

CREATE TABLE IF NOT EXISTS academic_years (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, school_id BIGINT NOT NULL,
 name VARCHAR(20) NOT NULL, start_date DATE NOT NULL, end_date DATE NOT NULL,
 is_current BOOLEAN NOT NULL DEFAULT FALSE,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_academic_year_school_name (school_id,name),
 CONSTRAINT fk_academic_year_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT,
 CONSTRAINT chk_academic_year_dates CHECK (end_date > start_date)
);

CREATE TABLE IF NOT EXISTS semesters (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, academic_year_id BIGINT NOT NULL,
 semester_number TINYINT NOT NULL, name VARCHAR(60) NOT NULL,
 start_date DATE NOT NULL, end_date DATE NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_semester_year_number (academic_year_id,semester_number),
 CONSTRAINT fk_semester_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE RESTRICT,
 CONSTRAINT chk_semester_number CHECK (semester_number BETWEEN 1 AND 3),
 CONSTRAINT chk_semester_dates CHECK (end_date > start_date)
);

CREATE TABLE IF NOT EXISTS subjects (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, school_id BIGINT NOT NULL,
 subject_code VARCHAR(40) NOT NULL, name VARCHAR(150) NOT NULL,
 description TEXT NULL, is_active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_subject_school_code (school_id,subject_code),
 CONSTRAINT fk_subject_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT
);

-- 2. AUTHENTICATION & AUTHORIZATION
CREATE TABLE IF NOT EXISTS users (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, school_id BIGINT NOT NULL,
 username VARCHAR(100) NOT NULL, password_hash VARCHAR(255) NOT NULL,
 phone VARCHAR(20) NULL, email VARCHAR(255) NULL,
 status ENUM('ACTIVE','LOCKED','DISABLED') NOT NULL DEFAULT 'ACTIVE',
 must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
 last_login_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_user_username (username), KEY idx_user_school_status (school_id,status),
 CONSTRAINT fk_user_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS roles (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(40) NOT NULL, name VARCHAR(100) NOT NULL,
 description VARCHAR(500) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_role_code (code)
);

CREATE TABLE IF NOT EXISTS user_roles (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL,
 role_id BIGINT NOT NULL,
 assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_user_role (user_id,role_id), KEY idx_user_roles_role (role_id),
 CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
 CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT
);

-- 3. PEOPLE
CREATE TABLE IF NOT EXISTS teachers (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, school_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL, teacher_code VARCHAR(40) NOT NULL,
 full_name VARCHAR(150) NOT NULL, date_of_birth DATE NULL,
 gender ENUM('MALE','FEMALE','OTHER') NULL,
 phone VARCHAR(20) NULL, email VARCHAR(255) NULL,
 status ENUM('ACTIVE','ON_LEAVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_teacher_user (user_id), UNIQUE KEY uk_teacher_code (school_id,teacher_code),
 CONSTRAINT fk_teacher_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT,
 CONSTRAINT fk_teacher_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS students (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, school_id BIGINT NOT NULL,
 user_id BIGINT NULL, student_code VARCHAR(40) NOT NULL,
 full_name VARCHAR(150) NOT NULL, date_of_birth DATE NULL,
 gender ENUM('MALE','FEMALE','OTHER') NULL,
 admission_date DATE NULL, address VARCHAR(500) NULL,
 status ENUM('ACTIVE','TRANSFERRED','GRADUATED','WITHDRAWN') NOT NULL DEFAULT 'ACTIVE',
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_student_user (user_id), UNIQUE KEY uk_student_code (school_id,student_code),
 KEY idx_student_school_status (school_id,status),
 CONSTRAINT fk_student_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT,
 CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS parents (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, school_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL, full_name VARCHAR(150) NOT NULL,
 phone VARCHAR(20) NULL, email VARCHAR(255) NULL,
 address VARCHAR(500) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_parent_user (user_id), KEY idx_parent_school (school_id),
 CONSTRAINT fk_parent_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT,
 CONSTRAINT fk_parent_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS parent_students (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, parent_id BIGINT NOT NULL,
 student_id BIGINT NOT NULL,
 relationship_type ENUM('FATHER','MOTHER','GUARDIAN','OTHER') NOT NULL,
 is_primary_contact BOOLEAN NOT NULL DEFAULT FALSE,
 can_view_academic BOOLEAN NOT NULL DEFAULT TRUE,
 can_submit_leave BOOLEAN NOT NULL DEFAULT TRUE,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_parent_student (parent_id,student_id), KEY idx_parent_student_student (student_id),
 CONSTRAINT fk_parent_student_parent FOREIGN KEY (parent_id) REFERENCES parents(id) ON DELETE RESTRICT,
 CONSTRAINT fk_parent_student_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE RESTRICT
);

-- 4. CLASSES & TEACHING
CREATE TABLE IF NOT EXISTS school_classes (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, school_id BIGINT NOT NULL,
 academic_year_id BIGINT NOT NULL,
 class_name VARCHAR(50) NOT NULL, grade_level TINYINT NOT NULL,
 homeroom_teacher_id BIGINT NULL,
 status ENUM('ACTIVE','ARCHIVED') NOT NULL DEFAULT 'ACTIVE',
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_class_year_name (academic_year_id,class_name),
 KEY idx_class_school (school_id), KEY idx_class_homeroom (homeroom_teacher_id),
 CONSTRAINT fk_class_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT,
 CONSTRAINT fk_class_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE RESTRICT,
 CONSTRAINT fk_class_homeroom FOREIGN KEY (homeroom_teacher_id) REFERENCES teachers(id) ON DELETE SET NULL,
 CONSTRAINT chk_class_grade CHECK (grade_level BETWEEN 6 AND 12)
);

CREATE TABLE IF NOT EXISTS class_enrollments (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, class_id BIGINT NOT NULL,
 student_id BIGINT NOT NULL,
 enrolled_at DATE NOT NULL, left_at DATE NULL,
 status ENUM('ACTIVE','TRANSFERRED','COMPLETED','WITHDRAWN') NOT NULL DEFAULT 'ACTIVE',
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 KEY idx_enrollment_class_status (class_id,status),
 KEY idx_enrollment_student_status (student_id,status),
 UNIQUE KEY uk_enrollment_student_class_start (student_id,class_id,enrolled_at),
 CONSTRAINT fk_enrollment_class FOREIGN KEY (class_id) REFERENCES school_classes(id) ON DELETE RESTRICT,
 CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE RESTRICT,
 CONSTRAINT chk_enrollment_dates CHECK (left_at IS NULL OR left_at >= enrolled_at)
);

CREATE TABLE IF NOT EXISTS teaching_assignments (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, class_id BIGINT NOT NULL,
 teacher_id BIGINT NOT NULL, subject_id BIGINT NOT NULL,
 semester_id BIGINT NOT NULL,
 status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_assignment (class_id,teacher_id,subject_id,semester_id),
 KEY idx_assignment_teacher_semester (teacher_id,semester_id),
 KEY idx_assignment_subject (subject_id), KEY idx_assignment_semester (semester_id),
 CONSTRAINT fk_assignment_class FOREIGN KEY (class_id) REFERENCES school_classes(id) ON DELETE RESTRICT,
 CONSTRAINT fk_assignment_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE RESTRICT,
 CONSTRAINT fk_assignment_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE RESTRICT,
 CONSTRAINT fk_assignment_semester FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS timetable_entries (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, teaching_assignment_id BIGINT NOT NULL,
 day_of_week TINYINT NOT NULL, period_number TINYINT NOT NULL,
 shift ENUM('MORNING','AFTERNOON','EVENING') NOT NULL DEFAULT 'MORNING',
 room VARCHAR(50) NULL, effective_from DATE NOT NULL, effective_to DATE NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 KEY idx_timetable_assignment (teaching_assignment_id),
 KEY idx_timetable_slot (day_of_week,shift,period_number),
 CONSTRAINT fk_timetable_assignment FOREIGN KEY (teaching_assignment_id) REFERENCES teaching_assignments(id) ON DELETE RESTRICT,
 CONSTRAINT chk_timetable_day CHECK (day_of_week BETWEEN 1 AND 7),
 CONSTRAINT chk_timetable_period CHECK (period_number BETWEEN 1 AND 15),
 CONSTRAINT chk_timetable_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

-- 5. ATTENDANCE
CREATE TABLE IF NOT EXISTS attendance_sessions (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, class_id BIGINT NOT NULL,
 teaching_assignment_id BIGINT NULL,
 timetable_entry_id BIGINT NULL,
 session_date DATE NOT NULL,
 shift ENUM('MORNING','AFTERNOON','EVENING') NOT NULL DEFAULT 'MORNING',
 period_number TINYINT NULL,
 session_type ENUM('HOMEROOM','SUBJECT') NOT NULL,
 status ENUM('OPEN','COMPLETED','CANCELLED') NOT NULL DEFAULT 'OPEN',
 created_by BIGINT NOT NULL,
 completed_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 KEY idx_session_class_date (class_id,session_date),
 KEY idx_session_assignment (teaching_assignment_id),
 KEY idx_session_timetable (timetable_entry_id),
 KEY idx_session_creator (created_by),
 CONSTRAINT fk_session_class FOREIGN KEY (class_id) REFERENCES school_classes(id) ON DELETE RESTRICT,
 CONSTRAINT fk_session_assignment FOREIGN KEY (teaching_assignment_id) REFERENCES teaching_assignments(id) ON DELETE RESTRICT,
 CONSTRAINT fk_session_timetable FOREIGN KEY (timetable_entry_id) REFERENCES timetable_entries(id) ON DELETE SET NULL,
 CONSTRAINT fk_session_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
 CONSTRAINT chk_session_period CHECK (period_number IS NULL OR period_number BETWEEN 1 AND 15),
 CONSTRAINT chk_session_type CHECK (
   (session_type='HOMEROOM' AND teaching_assignment_id IS NULL AND period_number IS NULL)
   OR (session_type='SUBJECT' AND teaching_assignment_id IS NOT NULL AND period_number IS NOT NULL)
 )
);

CREATE TABLE IF NOT EXISTS attendance_records (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, session_id BIGINT NOT NULL,
 student_id BIGINT NOT NULL,
 status ENUM('PRESENT','ABSENT','LATE','EXCUSED') NOT NULL,
 note VARCHAR(500) NULL, marked_by BIGINT NULL,
 marked_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_attendance_session_student (session_id,student_id),
 KEY idx_attendance_student (student_id), KEY idx_attendance_marked_by (marked_by),
 CONSTRAINT fk_attendance_session FOREIGN KEY (session_id) REFERENCES attendance_sessions(id) ON DELETE RESTRICT,
 CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE RESTRICT,
 CONSTRAINT fk_attendance_marker FOREIGN KEY (marked_by) REFERENCES users(id) ON DELETE SET NULL
);

-- 6. GRADES & EVALUATION
CREATE TABLE IF NOT EXISTS grade_items (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 teaching_assignment_id BIGINT NOT NULL,
 title VARCHAR(200) NOT NULL,
 grade_type ENUM('REGULAR','MIDTERM','FINAL','OTHER') NOT NULL,
 weight DECIMAL(6,2) NOT NULL DEFAULT 1.00,
 max_score DECIMAL(6,2) NOT NULL DEFAULT 10.00,
 assessment_date DATE NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 KEY idx_grade_item_assignment (teaching_assignment_id),
 CONSTRAINT fk_grade_item_assignment FOREIGN KEY (teaching_assignment_id) REFERENCES teaching_assignments(id) ON DELETE RESTRICT,
 CONSTRAINT chk_grade_item_weight CHECK (weight > 0),
 CONSTRAINT chk_grade_item_max CHECK (max_score > 0)
);

CREATE TABLE IF NOT EXISTS student_grades (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 grade_item_id BIGINT NOT NULL, student_id BIGINT NOT NULL,
 score DECIMAL(6,2) NULL, comment TEXT NULL,
 graded_by BIGINT NULL, graded_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_student_grade (grade_item_id,student_id),
 KEY idx_grade_student (student_id), KEY idx_grade_marker (graded_by),
 CONSTRAINT fk_student_grade_item FOREIGN KEY (grade_item_id) REFERENCES grade_items(id) ON DELETE RESTRICT,
 CONSTRAINT fk_student_grade_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE RESTRICT,
 CONSTRAINT fk_student_grade_marker FOREIGN KEY (graded_by) REFERENCES users(id) ON DELETE SET NULL,
 CONSTRAINT chk_student_grade_score CHECK (score IS NULL OR score >= 0)
);

CREATE TABLE IF NOT EXISTS student_evaluations (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 student_id BIGINT NOT NULL, semester_id BIGINT NOT NULL,
 class_id BIGINT NOT NULL, evaluated_by BIGINT NULL,
 academic_result VARCHAR(50) NULL, conduct_result VARCHAR(50) NULL,
 overall_comment TEXT NULL, evaluated_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_evaluation_student_semester (student_id,semester_id),
 KEY idx_evaluation_class (class_id), KEY idx_evaluation_semester (semester_id),
 KEY idx_evaluation_author (evaluated_by),
 CONSTRAINT fk_evaluation_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE RESTRICT,
 CONSTRAINT fk_evaluation_semester FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE RESTRICT,
 CONSTRAINT fk_evaluation_class FOREIGN KEY (class_id) REFERENCES school_classes(id) ON DELETE RESTRICT,
 CONSTRAINT fk_evaluation_author FOREIGN KEY (evaluated_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS student_remarks (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 student_id BIGINT NOT NULL, teacher_id BIGINT NOT NULL,
 class_id BIGINT NOT NULL, semester_id BIGINT NOT NULL,
 content TEXT NOT NULL, remark_date DATE NOT NULL,
 visibility ENUM('STAFF_ONLY','PARENTS_VISIBLE') NOT NULL DEFAULT 'PARENTS_VISIBLE',
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 KEY idx_remark_student_date (student_id,remark_date),
 KEY idx_remark_teacher (teacher_id), KEY idx_remark_class (class_id), KEY idx_remark_semester (semester_id),
 CONSTRAINT fk_remark_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE RESTRICT,
 CONSTRAINT fk_remark_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE RESTRICT,
 CONSTRAINT fk_remark_class FOREIGN KEY (class_id) REFERENCES school_classes(id) ON DELETE RESTRICT,
 CONSTRAINT fk_remark_semester FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE RESTRICT
);

-- 7. COMMUNICATION & PARENT INTERACTION
CREATE TABLE IF NOT EXISTS announcements (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 school_id BIGINT NOT NULL, author_user_id BIGINT NOT NULL,
 title VARCHAR(255) NOT NULL, content LONGTEXT NOT NULL,
 status ENUM('DRAFT','PUBLISHED','RETRACTED') NOT NULL DEFAULT 'DRAFT',
 published_at DATETIME(6) NULL, expires_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 KEY idx_announcement_school_status (school_id,status,published_at),
 KEY idx_announcement_author (author_user_id),
 CONSTRAINT fk_announcement_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT,
 CONSTRAINT fk_announcement_author FOREIGN KEY (author_user_id) REFERENCES users(id) ON DELETE RESTRICT,
 CONSTRAINT chk_announcement_dates CHECK (expires_at IS NULL OR published_at IS NULL OR expires_at > published_at)
);

CREATE TABLE IF NOT EXISTS announcement_targets (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 announcement_id BIGINT NOT NULL,
 target_type ENUM('SCHOOL','CLASS','USER') NOT NULL,
 school_id BIGINT NULL, class_id BIGINT NULL, user_id BIGINT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 KEY idx_target_announcement (announcement_id),
 KEY idx_target_school (school_id), KEY idx_target_class (class_id), KEY idx_target_user (user_id),
 CONSTRAINT fk_target_announcement FOREIGN KEY (announcement_id) REFERENCES announcements(id) ON DELETE CASCADE,
 CONSTRAINT fk_target_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT,
 CONSTRAINT fk_target_class FOREIGN KEY (class_id) REFERENCES school_classes(id) ON DELETE RESTRICT,
 CONSTRAINT fk_target_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
 CONSTRAINT chk_target_exactly_one CHECK (
  (target_type='SCHOOL' AND school_id IS NOT NULL AND class_id IS NULL AND user_id IS NULL)
  OR (target_type='CLASS' AND school_id IS NULL AND class_id IS NOT NULL AND user_id IS NULL)
  OR (target_type='USER' AND school_id IS NULL AND class_id IS NULL AND user_id IS NOT NULL)
 )
);

CREATE TABLE IF NOT EXISTS announcement_reads (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 announcement_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
 read_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_announcement_read (announcement_id,user_id), KEY idx_read_user (user_id),
 CONSTRAINT fk_read_announcement FOREIGN KEY (announcement_id) REFERENCES announcements(id) ON DELETE CASCADE,
 CONSTRAINT fk_read_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS leave_requests (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 school_id BIGINT NOT NULL, student_id BIGINT NOT NULL,
 parent_id BIGINT NOT NULL, class_id BIGINT NOT NULL,
 start_date DATE NOT NULL, end_date DATE NOT NULL,
 reason TEXT NOT NULL,
 status ENUM('PENDING','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING',
 reviewed_by BIGINT NULL, reviewed_at DATETIME(6) NULL,
 review_note TEXT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 KEY idx_leave_student_dates (student_id,start_date,end_date),
 KEY idx_leave_class_status (class_id,status),
 KEY idx_leave_parent (parent_id), KEY idx_leave_school (school_id),
 KEY idx_leave_reviewer (reviewed_by),
 CONSTRAINT fk_leave_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT,
 CONSTRAINT fk_leave_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE RESTRICT,
 CONSTRAINT fk_leave_parent FOREIGN KEY (parent_id) REFERENCES parents(id) ON DELETE RESTRICT,
 CONSTRAINT fk_leave_class FOREIGN KEY (class_id) REFERENCES school_classes(id) ON DELETE RESTRICT,
 CONSTRAINT fk_leave_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL,
 CONSTRAINT chk_leave_dates CHECK (end_date >= start_date)
);

CREATE TABLE IF NOT EXISTS leave_request_histories (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 leave_request_id BIGINT NOT NULL,
 actor_user_id BIGINT NOT NULL,
 old_status ENUM('PENDING','APPROVED','REJECTED','CANCELLED') NULL,
 new_status ENUM('PENDING','APPROVED','REJECTED','CANCELLED') NOT NULL,
 note TEXT NULL, changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 KEY idx_leave_history_request_time (leave_request_id,changed_at),
 KEY idx_leave_history_actor (actor_user_id),
 CONSTRAINT fk_leave_history_request FOREIGN KEY (leave_request_id) REFERENCES leave_requests(id) ON DELETE RESTRICT,
 CONSTRAINT fk_leave_history_actor FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS device_tokens (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 token VARCHAR(512) NOT NULL,
 platform ENUM('ANDROID','IOS','WEB') NOT NULL,
 device_name VARCHAR(150) NULL, is_active BOOLEAN NOT NULL DEFAULT TRUE,
 last_seen_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_device_token (token), KEY idx_device_user_active (user_id,is_active),
 CONSTRAINT fk_device_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 8. IMPORT & AUDIT
CREATE TABLE IF NOT EXISTS import_batches (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 school_id BIGINT NOT NULL, imported_by BIGINT NOT NULL,
 import_type ENUM('STUDENT','TEACHER','PARENT','PARENT_STUDENT','GRADE') NOT NULL,
 file_name VARCHAR(255) NOT NULL, file_checksum CHAR(64) NULL,
 total_rows INT NOT NULL DEFAULT 0,
 success_rows INT NOT NULL DEFAULT 0,
 failed_rows INT NOT NULL DEFAULT 0,
 status ENUM('PENDING','VALIDATING','PROCESSING','COMPLETED','PARTIAL','FAILED') NOT NULL DEFAULT 'PENDING',
 started_at DATETIME(6) NULL, finished_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 KEY idx_import_school_time (school_id,created_at), KEY idx_import_actor (imported_by),
 CONSTRAINT fk_import_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT,
 CONSTRAINT fk_import_actor FOREIGN KEY (imported_by) REFERENCES users(id) ON DELETE RESTRICT,
 CONSTRAINT chk_import_counts CHECK (success_rows + failed_rows <= total_rows)
);

CREATE TABLE IF NOT EXISTS import_errors (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 batch_id BIGINT NOT NULL, row_number INT NOT NULL,
 column_name VARCHAR(100) NULL, error_code VARCHAR(60) NULL,
 error_message TEXT NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 KEY idx_import_error_batch_row (batch_id,row_number),
 CONSTRAINT fk_import_error_batch FOREIGN KEY (batch_id) REFERENCES import_batches(id) ON DELETE CASCADE,
 CONSTRAINT chk_import_error_row CHECK (row_number >= 1)
);

CREATE TABLE IF NOT EXISTS audit_logs (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 school_id BIGINT NULL, actor_user_id BIGINT NULL,
 action VARCHAR(100) NOT NULL, entity_type VARCHAR(100) NOT NULL,
 entity_id BIGINT NULL,
 old_values JSON NULL, new_values JSON NULL,
 ip_address VARCHAR(45) NULL, user_agent VARCHAR(500) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 KEY idx_audit_school_time (school_id,created_at),
 KEY idx_audit_actor_time (actor_user_id,created_at),
 KEY idx_audit_entity (entity_type,entity_id),
 CONSTRAINT fk_audit_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE SET NULL,
 CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Business invariants to enforce in the service layer (cross-table checks):
-- * Every child record must belong to the same school as its referenced parent records.
-- * A teacher's class, subject and semester must share the same school/year.
-- * Student must be enrolled in the session/grade class on the relevant date.
-- * At most one active enrollment per student per academic year; no overlapping periods.
-- * No overlapping timetable slots for a class/teacher/room during effective dates.
-- * Score must not exceed grade_items.max_score.
-- * Only linked parents with permission can submit leave requests or view records.
-- * Attendance mode (HOMEROOM/SUBJECT) is chosen per session.
-- * Restrict announcement recipients to the authorizing school/class.
-- * Avoid logging passwords, tokens, or sensitive data in audit_logs/import_errors.
-- * Enforce one current academic year per school in service or via additional generated-column index.
-- * Use application transactions for importing related users/parents/students.
