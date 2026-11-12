package br.com.erp.branch;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.Repository;

public interface BranchRepository extends Repository<Branch,UUID> {
    Branch save(Branch branch);
    Page<Branch> findByTenantId(UUID tenantId,Pageable page);
    Optional<Branch> findByTenantIdAndId(UUID tenantId,UUID id);
}
