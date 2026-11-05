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
    private final EntityManager em;
    private final TransactionTemplate tx;
    public record Result(Long id,String fileName,ImportStatus status,int totalRows,int successRows,int failedRows) {}
    public record RowError(int rowNumber,String columnName,String errorCode,String errorMessage) {}
    private Result result(ImportBatch b){return new Result(b.getId(),b.getFileName(),b.getStatus(),b.getTotalRows(),b.getSuccessRows(),b.getFailedRows());}
    public Result run(Long uid,MultipartFile file) {
        if(file.isEmpty()||file.getSize()>5*1024*1024)throw new BusinessException(ErrorCode.INVALID_REQUEST);
        List<CreateTeacher> rows=new ArrayList<>();
        byte[] bytes;
        try {
            bytes=file.getBytes();
            try(Workbook wb=WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes))){
                if(wb.getNumberOfSheets()==0)throw new IllegalArgumentException();
                Sheet sheet=wb.getSheetAt(0);if(sheet.getLastRowNum()>1000)throw new IllegalArgumentException();
                String[] headers={"username","password","teacherCode","fullName","email","phone"};
                DataFormatter fmt=new DataFormatter(Locale.ROOT);
                if(sheet.getRow(0)==null)throw new IllegalArgumentException();
                for(int c=0;c<6;c++)if(!headers[c].equals(fmt.formatCellValue(sheet.getRow(0).getCell(c)).trim()))throw new IllegalArgumentException();
                for(int i=1;i<=sheet.getLastRowNum();i++) {
                    Row row=sheet.getRow(i);String[] v=new String[6];
                    for(int c=0;c<6;c++){Cell cell=row==null?null:row.getCell(c);if(cell!=null&&cell.getCellType()==CellType.FORMULA)throw new IllegalArgumentException();v[c]=fmt.formatCellValue(cell);}
                    rows.add(new CreateTeacher(v[0],v[1],v[2],v[3],v[4].isBlank()?null:v[4],v[5].isBlank()?null:v[5]));
                }
            }
        }catch(Exception ex){throw new BusinessException(ErrorCode.INVALID_REQUEST,"Invalid Excel workbook, headers or row limit (1000)");}
        if(rows.isEmpty())throw new BusinessException(ErrorCode.INVALID_REQUEST);
        final String checksum;
        try{checksum=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}
        Long id=tx.execute(s->{User a=accounts.admin(uid);ImportBatch b=new ImportBatch();b.setSchool(a.getSchool());b.setImportedBy(a);b.setImportType(ImportType.TEACHER);String n=file.getOriginalFilename();b.setFileName(n==null?"accounts.xlsx":n.substring(Math.max(0,n.length()-255)));b.setFileChecksum(checksum);b.setTotalRows(rows.size());b.setStatus(ImportStatus.PROCESSING);em.persist(b);em.flush();em.createNativeQuery("update import_batches set started_at=:now where id=:id").setParameter("now",LocalDateTime.now()).setParameter("id",b.getId()).executeUpdate();return b.getId();});
        for(int i=0;i<rows.size();i++) {
            final int number=i+2;final CreateTeacher row=rows.get(i);
            // One transaction per row: an invalid row cannot roll back other accounts.
            try{tx.executeWithoutResult(s->{accounts.create(uid,row);ImportBatch b=em.find(ImportBatch.class,id);b.setSuccessRows(b.getSuccessRows()+1);});}
            catch(Exception ex){final String code=ex instanceof BusinessException?"INVALID_OR_DUPLICATE_ACCOUNT":"ROW_PERSISTENCE_FAILED";
                tx.executeWithoutResult(s->{ImportBatch b=em.find(ImportBatch.class,id);b.setFailedRows(b.getFailedRows()+1);ImportError e=new ImportError();e.setBatch(b);e.setRowNumber(number);e.setErrorCode(code);e.setErrorMessage("Account row rejected; check required fields, unique username/teacherCode and field limits");em.persist(e);});}
        }
        return tx.execute(s->{ImportBatch b=em.find(ImportBatch.class,id);b.setStatus(b.getFailedRows()==0?ImportStatus.COMPLETED:b.getSuccessRows()==0?ImportStatus.FAILED:ImportStatus.PARTIAL);em.flush();em.createNativeQuery("update import_batches set finished_at=:now where id=:id").setParameter("now",LocalDateTime.now()).setParameter("id",id).executeUpdate();return result(b);});
    }
    private ImportBatch batch(Long uid,Long id){User a=accounts.admin(uid);ImportBatch b=em.find(ImportBatch.class,id);if(b==null)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);if(!b.getSchool().getId().equals(a.getSchool().getId()))throw new BusinessException(ErrorCode.ACCESS_DENIED);return b;}
    public Result get(Long uid,Long id){return tx.execute(s->result(batch(uid,id)));}
    public Map<String,Object> errors(Long uid,Long id,int page,int size){SchoolAdminAccountService.page(page,size);return tx.execute(s->{batch(uid,id);var errors=em.createQuery("select e from ImportError e where e.batch.id=:id order by e.rowNumber,e.id",ImportError.class).setParameter("id",id).setFirstResult(page*size).setMaxResults(size).getResultList();long count=em.createQuery("select count(e) from ImportError e where e.batch.id=:id",Long.class).setParameter("id",id).getSingleResult();return Map.of("content",errors.stream().map(e->new RowError(e.getRowNumber(),e.getColumnName(),e.getErrorCode(),e.getErrorMessage())).toList(),"page",page,"size",size,"totalElements",count);});}
}
