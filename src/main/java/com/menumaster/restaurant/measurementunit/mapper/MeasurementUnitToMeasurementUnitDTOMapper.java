package com.menumaster.restaurant.measurementunit.mapper;

import com.menumaster.restaurant.measurementunit.domain.dto.MeasurementUnitDTO;
import com.menumaster.restaurant.measurementunit.domain.model.MeasurementUnit;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MeasurementUnitToMeasurementUnitDTOMapper {

    MeasurementUnitDTO convert(MeasurementUnit measurementUnit);
}
