package com.pukio.appserver.dataaccess;

import com.pukio.appserver.domain.Arqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArqueoRepository extends JpaRepository<Arqueo, Long> {
}
