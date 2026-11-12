package br.com.erp.product;

import br.com.erp.auth.Actor;
import br.com.erp.shared.Audit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {
    private final ProductRepository repository;
    private final JdbcTemplate db;
    private final Audit audit;
    public ProductService(ProductRepository repository,JdbcTemplate db,Audit audit) { this.repository=repository; this.db=db; this.audit=audit; }
    public record Input(@NotBlank @Pattern(regexp="[A-Za-z0-9._-]{1,60}") String sku,@NotBlank @Size(max=200) String name,@NotNull @Size(max=2000) String description,@NotBlank @Pattern(regexp="UN|CX|PCT|KG|G|L|ML|M|M2") String unit,@NotNull @DecimalMin("0.00") @Digits(integer=17,fraction=2) BigDecimal salePrice,@NotNull @Size(max=20) List<@NotNull @Valid Barcode> barcodes) {}
    public record View(UUID id,String sku,String name,String description,String unit,@com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal salePrice,boolean active,long version,List<Barcode> barcodes) {}
    @Transactional public View create(Actor actor,Input input) {
        var canonical=input.barcodes().stream().map(Barcode::canonical).toList();
        if (new HashSet<>(canonical).size()!=canonical.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Códigos de barras repetidos no cadastro.");
        // Repository flush translates database constraints to the standard 409 API error.
        Product product=repository.saveAndFlush(new Product(actor.tenantId(),input.sku(),input.name().trim(),input.description().trim(),input.unit(),input.salePrice()));
        for (int i=0;i<input.barcodes().size();i++) {
            Barcode barcode=input.barcodes().get(i);
            db.update("INSERT INTO product_barcode(tenant_id,product_id,code,canonical_code,kind) VALUES(?,?,?,?,?)",actor.tenantId(),product.getId(),barcode.code(),canonical.get(i),barcode.kind().name());
        }
        audit.record(actor,"PRODUCT_CREATED",product.getId());
        return view(actor,product);
    }
    @Transactional(readOnly=true) public Map<String,Object> list(Actor actor,String query,int page,int size) {
        return list(actor,query,page,size,null);
    }
    @Transactional(readOnly=true) public Map<String,Object> list(Actor actor,String query,int page,int size,Boolean active) {
        var products=repository.searchFiltered(actor.tenantId(),query,active,PageRequest.of(page,size,Sort.by("name","id")));
        // Fetch all codes for this page once; avoid one query per product.
        var ids=products.getContent().stream().map(Product::getId).toList();
        Map<UUID,List<Barcode>> codes=new HashMap<>();
        if (!ids.isEmpty()) {
            List<Object> args=new ArrayList<>(); args.add(actor.tenantId()); args.addAll(ids);
            db.query("SELECT product_id,code,kind FROM product_barcode WHERE tenant_id=? AND product_id IN ("+String.join(",",Collections.nCopies(ids.size(),"?"))+") ORDER BY code",rs -> { codes.computeIfAbsent(rs.getObject(1,UUID.class),key->new ArrayList<>()).add(new Barcode(rs.getString(2),Barcode.Kind.valueOf(rs.getString(3)))); },args.toArray());
        }
        return Map.of("items",products.getContent().stream().map(p->view(p,codes.getOrDefault(p.getId(),List.of()))).toList(),"total",products.getTotalElements(),"page",page,"size",size);
    }
    @Transactional(readOnly=true) public View lookup(Actor actor,String code,Barcode.Kind kind) {
        String canonical=new Barcode(code,kind).canonical();
        var ids=db.query("SELECT product_id FROM product_barcode WHERE tenant_id=? AND canonical_code=?",(rs,i)->rs.getObject(1,UUID.class),actor.tenantId(),canonical);
        if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Produto não encontrado na base interna.");
        return get(actor,ids.getFirst());
    }
    @Transactional(readOnly=true) public View get(Actor actor,UUID id) {
        return view(actor,repository.findByTenantIdAndId(actor.tenantId(),id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Produto não encontrado.")));
    }
    private View view(Actor actor,Product product) {
        var codes=db.query("SELECT code,kind FROM product_barcode WHERE tenant_id=? AND product_id=? ORDER BY code",(rs,i)->new Barcode(rs.getString(1),Barcode.Kind.valueOf(rs.getString(2))),actor.tenantId(),product.getId());
        return view(product,codes);
    }
    private View view(Product p,List<Barcode> codes) { return new View(p.getId(),p.getSku(),p.getName(),p.getDescription(),p.getUnit(),p.getSalePrice(),p.isActive(),p.getVersion(),codes); }
}
