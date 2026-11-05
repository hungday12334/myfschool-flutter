package com.example.myfschool.service;

import com.example.myfschool.dto.AdminAccountDtos.CreateTeacher;
import com.example.myfschool.entity.*;
import com.example.myfschool.enums.*;
import com.example.myfschool.exception.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SchoolAdminImportService {
    private final SchoolAdminAccountService accounts;
    private final SchoolAdminSchoolService schoolService;
    private final EntityManager em;
    private final TransactionTemplate tx;
    public record Result(Long id,String fileName,ImportStatus status,int totalRows,int successRows,int failedRows) {}
    public record RowError(int rowNumber,String columnName,String errorCode,String errorMessage) {}
    private Result result(ImportBatch b){return new Result(b.getId(),b.getFileName(),b.getStatus(),b.getTotalRows(),b.getSuccessRows(),b.getFailedRows());}
    public Result run(Long uid,MultipartFile file) {
        return run(uid,file,false);
    }
    public Result runStudents(Long uid,MultipartFile file) {
        return run(uid,file,true);
    }
    private Result run(Long uid,MultipartFile file,boolean students) {
        if(file.isEmpty()||file.getSize()>5*1024*1024)throw new BusinessException(ErrorCode.INVALID_REQUEST);
        List<String[]> rows=new ArrayList<>();
        byte[] bytes;
        try {
            bytes=file.getBytes();
            try(Workbook wb=WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes))){
                if(wb.getNumberOfSheets()==0)throw new IllegalArgumentException();
                Sheet sheet=wb.getSheetAt(0);if(sheet.getLastRowNum()>1000)throw new IllegalArgumentException();
                String[] headers=students?new String[]{"studentCode","fullName","dateOfBirth","gender","admissionDate","address","status"}:new String[]{"username","password","teacherCode","fullName","email","phone"};
                DataFormatter fmt=new DataFormatter(Locale.ROOT);
                if(sheet.getRow(0)==null)throw new IllegalArgumentException();
                for(int c=0;c<headers.length;c++)if(!headers[c].equals(fmt.formatCellValue(sheet.getRow(0).getCell(c)).trim()))throw new IllegalArgumentException();
                for(int i=1;i<=sheet.getLastRowNum();i++) {
                    Row row=sheet.getRow(i);String[] v=new String[headers.length];
                    for(int c=0;c<headers.length;c++){Cell cell=row==null?null:row.getCell(c);if(cell!=null&&cell.getCellType()==CellType.FORMULA)throw new IllegalArgumentException();v[c]=students&&cell!=null&&cell.getCellType()==CellType.NUMERIC&&DateUtil.isCellDateFormatted(cell)?cell.getLocalDateTimeCellValue().toLocalDate().toString():fmt.formatCellValue(cell);}
                    rows.add(v);
                }
            }
        }catch(Exception ex){throw new BusinessException(ErrorCode.INVALID_REQUEST,"Invalid Excel workbook, headers or row limit (1000)");}
        if(rows.isEmpty())throw new BusinessException(ErrorCode.INVALID_REQUEST);
        final String checksum;
        try{checksum=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}
        Long id=tx.execute(s->{User a=accounts.admin(uid);ImportBatch b=new ImportBatch();b.setSchool(a.getSchool());b.setImportedBy(a);b.setImportType(students?ImportType.STUDENT:ImportType.TEACHER);String n=file.getOriginalFilename();b.setFileName(n==null?"accounts.xlsx":n.substring(Math.max(0,n.length()-255)));b.setFileChecksum(checksum);b.setTotalRows(rows.size());b.setStatus(ImportStatus.PROCESSING);em.persist(b);em.flush();em.createNativeQuery("update import_batches set started_at=:now where id=:id").setParameter("now",LocalDateTime.now()).setParameter("id",b.getId()).executeUpdate();return b.getId();});
        for(int i=0;i<rows.size();i++) {
            final int number=i+2;final String[] row=rows.get(i);
            // One transaction per row: an invalid row cannot roll back other accounts.
            try{tx.executeWithoutResult(s->{if(students)schoolService.student(uid,null,new com.example.myfschool.dto.AdminSchoolDtos.StudentProfile(row[0],row[1],row[2].isBlank()?null:java.time.LocalDate.parse(row[2]),row[3].isBlank()?null:Gender.valueOf(row[3]),row[4].isBlank()?null:java.time.LocalDate.parse(row[4]),row[5],StudentStatus.valueOf(row[6])));else accounts.create(uid,new CreateTeacher(row[0],row[1],row[2],row[3],row[4].isBlank()?null:row[4],row[5].isBlank()?null:row[5]));ImportBatch b=em.find(ImportBatch.class,id);b.setSuccessRows(b.getSuccessRows()+1);});}
            catch(Exception ex){final String code=ex instanceof BusinessException?"INVALID_OR_DUPLICATE_ACCOUNT":"ROW_PERSISTENCE_FAILED";
                tx.executeWithoutResult(s->{ImportBatch b=em.find(ImportBatch.class,id);b.setFailedRows(b.getFailedRows()+1);ImportError e=new ImportError();e.setBatch(b);e.setRowNumber(number);e.setErrorCode(students?"INVALID_OR_DUPLICATE_STUDENT":code);e.setErrorMessage(students?"Student row rejected; check required fields, unique studentCode, ISO dates and enum values":"Account row rejected; check required fields, unique username/teacherCode and field limits");em.persist(e);});}
        }
        return tx.execute(s->{ImportBatch b=em.find(ImportBatch.class,id);b.setStatus(b.getFailedRows()==0?ImportStatus.COMPLETED:b.getSuccessRows()==0?ImportStatus.FAILED:ImportStatus.PARTIAL);em.flush();em.createNativeQuery("update import_batches set finished_at=:now where id=:id").setParameter("now",LocalDateTime.now()).setParameter("id",id).executeUpdate();return result(b);});
    }
    private ImportBatch batch(Long uid,Long id){User a=accounts.admin(uid);ImportBatch b=em.find(ImportBatch.class,id);if(b==null)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);if(!b.getSchool().getId().equals(a.getSchool().getId()))throw new BusinessException(ErrorCode.ACCESS_DENIED);return b;}
    public Result get(Long uid,Long id){return tx.execute(s->result(batch(uid,id)));}
    public Map<String,Object> errors(Long uid,Long id,int page,int size){SchoolAdminAccountService.page(page,size);return tx.execute(s->{batch(uid,id);var errors=em.createQuery("select e from ImportError e where e.batch.id=:id order by e.rowNumber,e.id",ImportError.class).setParameter("id",id).setFirstResult(page*size).setMaxResults(size).getResultList();long count=em.createQuery("select count(e) from ImportError e where e.batch.id=:id",Long.class).setParameter("id",id).getSingleResult();return Map.of("content",errors.stream().map(e->new RowError(e.getRowNumber(),e.getColumnName(),e.getErrorCode(),e.getErrorMessage())).toList(),"page",page,"size",size,"totalElements",count);});}
}
