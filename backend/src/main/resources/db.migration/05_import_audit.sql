USE myfschool;
SET NAMES utf8mb4;
-- DEMO ONLY: run on a new/empty MyFSchool schema, once, in filename order.
START TRANSACTION;
INSERT INTO import_batches(id,school_id,imported_by,import_type,file_name,total_rows,success_rows,failed_rows,status,started_at,finished_at) VALUES
(1,1,1,'STUDENT','demo_students.xlsx',6,6,0,'COMPLETED','2026-09-01 08:00:00','2026-09-01 08:01:00'),
(2,1,1,'PARENT','demo_parents.xlsx',5,4,1,'PARTIAL','2026-09-01 08:05:00','2026-09-01 08:06:00');
INSERT INTO import_errors(batch_id,`row_number`,column_name,error_code,error_message) VALUES
(2,5,'phone','INVALID_PHONE','Số điện thoại không đúng định dạng (dữ liệu demo).');
INSERT INTO audit_logs(school_id,actor_user_id,action,entity_type,entity_id,old_values,new_values,ip_address) VALUES
(1,1,'IMPORT','ImportBatch',1,NULL,JSON_OBJECT('status','COMPLETED'),'127.0.0.1'),
(1,3,'APPROVE_LEAVE','LeaveRequest',1,JSON_OBJECT('status','PENDING'),JSON_OBJECT('status','APPROVED'),'127.0.0.1');

COMMIT;
