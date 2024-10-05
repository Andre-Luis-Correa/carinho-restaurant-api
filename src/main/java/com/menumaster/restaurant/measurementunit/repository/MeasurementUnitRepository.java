package com.menumaster.restaurant.measurementunit.repository;

import com.menumaster.restaurant.measurementunit.domain.model.MeasurementUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeasurementUnitRepository extends JpaRepository<MeasurementUnit, Long> {
}
