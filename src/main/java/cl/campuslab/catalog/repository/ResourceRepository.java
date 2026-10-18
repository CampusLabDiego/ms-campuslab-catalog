package cl.campuslab.catalog.repository;

import cl.campuslab.catalog.entity.Resource;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResourceRepository
        extends JpaRepository<Resource, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Resource r where r.id = :id")
    Optional<Resource> findLockedById(
            @Param("id") Long id
    );
}