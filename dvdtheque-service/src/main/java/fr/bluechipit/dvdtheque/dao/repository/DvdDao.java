package fr.bluechipit.dvdtheque.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import fr.bluechipit.dvdtheque.dao.domain.Dvd;

public interface DvdDao extends JpaRepository<Dvd, Long>, JpaSpecificationExecutor<Dvd>{

}
