package br.com.erp.purchase;

import br.com.erp.auth.Actor;
import br.com.erp.shared.Audit;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class SupplierService {
    private final JdbcTemplate db;
    private final Audit audit;
    public SupplierService(JdbcTemplate db,Audit audit) { this.db=db;this.audit=audit; }
    public record Input(@NotBlank @Pattern(regexp="[A-Za-z0-9._-]{1,40}") String code,@NotBlank @Size(max=200) String name,
        @NotNull @Size(max=40) String taxIdentifier,@NotNull @Size(max=160) String contactName,
        @NotNull @Email @Size(max=254) String email,@NotNull @Size(max=40) String phone,
        @NotNull @Size(max=500) String address,@NotNull @Size(max=2000) String notes) {}
    public record Update(@NotNull @jakarta.validation.Valid Input data,@NotNull Boolean active,@NotNull @Min(0) Long version) {}
    public record View(UUID id,String code,String name,String taxIdentifier,String contactName,String email,String phone,String address,String notes,boolean active,long version) {}
    private View row(java.sql.ResultSet rs,int index) throws java.sql.SQLException {
        return new View(rs.getObject("id",UUID.class),rs.getString("code"),rs.getString("name"),rs.getString("tax_identifier"),rs.getString("contact_name"),rs.getString("email"),rs.getString("phone"),rs.getString("address"),rs.getString("notes"),rs.getBoolean("active"),rs.getLong("version"));
    }
    private String code(Input input) { return input.code().toUpperCase(Locale.ROOT); }
    @Transactional(readOnly=true) public View get(Actor actor,UUID id) { return find(actor,id,false); }
    private View find(Actor actor,UUID id,boolean lock) {
        var rows=db.query("SELECT * FROM supplier WHERE tenant_id=? AND id=?"+(lock?" FOR UPDATE":""),this::row,actor.tenantId(),id);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Fornecedor não encontrado.");return rows.getFirst();
    }
    @Transactional(readOnly=true) public Map<String,Object> list(Actor actor,String query,int page,int size) {
        String q=query.trim();
        String where=" WHERE tenant_id=? AND (POSITION(LOWER(?) IN LOWER(name))>0 OR POSITION(LOWER(?) IN LOWER(code))>0 OR POSITION(LOWER(?) IN LOWER(tax_identifier))>0)";
        var items=db.query("SELECT * FROM supplier"+where+" ORDER BY name,id LIMIT ? OFFSET ?",this::row,actor.tenantId(),q,q,q,size,(long)page*size);
        Long total=db.queryForObject("SELECT COUNT(*) FROM supplier"+where,Long.class,actor.tenantId(),q,q,q);
        return Map.of("items",items,"total",total,"page",page,"size",size);
    }
    @Transactional public View create(Actor actor,Input input) {
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO supplier(id,tenant_id,code,name,tax_identifier,contact_name,email,phone,address,notes) VALUES(?,?,?,?,?,?,?,?,?,?)",id,actor.tenantId(),code(input),input.name().trim(),input.taxIdentifier().trim(),input.contactName().trim(),input.email().toLowerCase(Locale.ROOT),input.phone().trim(),input.address().trim(),input.notes().trim());
        audit.record(actor,"SUPPLIER_CREATED",id);return find(actor,id,false);
    }
    @Transactional public View update(Actor actor,UUID id,Update input) {
        var current=find(actor,id,true);
        if(current.version()!=input.version()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Fornecedor alterado por outro acesso. Atualize a lista.");
        var data=input.data();
        View next=new View(id,code(data),data.name().trim(),data.taxIdentifier().trim(),data.contactName().trim(),data.email().toLowerCase(Locale.ROOT),data.phone().trim(),data.address().trim(),data.notes().trim(),input.active(),current.version());
        if(current.equals(next)) return current;
        db.update("UPDATE supplier SET code=?,name=?,tax_identifier=?,contact_name=?,email=?,phone=?,address=?,notes=?,active=?,version=version+1 WHERE tenant_id=? AND id=?",next.code(),next.name(),next.taxIdentifier(),next.contactName(),next.email(),next.phone(),next.address(),next.notes(),next.active(),actor.tenantId(),id);
        audit.record(actor,"SUPPLIER_UPDATED",id);return find(actor,id,false);
    }
}
