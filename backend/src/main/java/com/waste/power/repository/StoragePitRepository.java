package com.waste.power.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.waste.power.domain.StoragePit;

/**
 * 垃圾储坑数据访问。
 */
public interface StoragePitRepository extends JpaRepository<StoragePit, Long> {

    Optional<StoragePit> findByPitNo(String pitNo);
}
