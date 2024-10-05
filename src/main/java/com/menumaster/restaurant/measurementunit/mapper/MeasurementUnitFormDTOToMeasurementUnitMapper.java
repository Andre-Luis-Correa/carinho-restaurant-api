package com.menumaster.restaurant.measurementunit.mapper;

import com.menumaster.restaurant.measurementunit.domain.dto.MeasurementUnitFormDTO;
import com.menumaster.restaurant.measurementunit.domain.model.MeasurementUnit;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MeasurementUnitFormDTOToMeasurementUnitMapper {

    MeasurementUnit convert(MeasurementUnitFormDTO measurementUnitFormDTO);
}
