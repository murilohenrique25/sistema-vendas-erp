package br.com.erp.product;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends Repository<Product,UUID> {
    Product saveAndFlush(Product product);
    Optional<Product> findByTenantIdAndId(UUID tenantId,UUID id);
    @Query("select p from Product p where p.tenantId=:tenant and (locate(lower(:q),lower(p.name))>0 or locate(lower(:q),lower(p.sku))>0 or exists (select 1 from ProductBarcodeEntity b where b.tenantId=p.tenantId and b.productId=p.id and locate(:q,b.code)>0))")
    Page<Product> search(@Param("tenant") UUID tenantId,@Param("q") String query,Pageable page);
    @Query("select p from Product p where p.tenantId=:tenant and (:active is null or p.active=:active) and (locate(lower(:q),lower(p.name))>0 or locate(lower(:q),lower(p.sku))>0 or exists (select 1 from ProductBarcodeEntity b where b.tenantId=p.tenantId and b.productId=p.id and locate(:q,b.code)>0))")
    Page<Product> searchFiltered(@Param("tenant") UUID tenantId,@Param("q") String query,@Param("active") Boolean active,Pageable page);
}
