package com.pukio.appserver.dataaccess;

import com.pukio.appserver.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Store entity.
 * 
 * @author Pukio Team
 * @since Entregable 2
 */
@Repository
public interface StoreRepository extends JpaRepository<Store, String> {
}
