package com.menumaster.restaurant.measurementunit.service;

import com.menumaster.restaurant.exception.type.EntityNotFoundException;
import com.menumaster.restaurant.measurementunit.domain.dto.MeasurementUnitDTO;
import com.menumaster.restaurant.measurementunit.domain.dto.MeasurementUnitFormDTO;
import com.menumaster.restaurant.measurementunit.domain.model.MeasurementUnit;
import com.menumaster.restaurant.measurementunit.mapper.MeasurementUnitFormDTOToMeasurementUnitMapper;
import com.menumaster.restaurant.measurementunit.mapper.MeasurementUnitToMeasurementUnitDTOMapper;
import com.menumaster.restaurant.measurementunit.repository.MeasurementUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeasurementUnitService {

    private final MeasurementUnitRepository measurementUnitRepository;
    private final MeasurementUnitFormDTOToMeasurementUnitMapper measurementUnitFormDTOToMeasurementUnitMapper;
    private final MeasurementUnitToMeasurementUnitDTOMapper measurementUnitToMeasurementUnitDTOMapper;

    public MeasurementUnitDTO create(MeasurementUnitFormDTO measurementUnitFormDTO) {
        MeasurementUnit measurementUnitToBeSaved = convertMeasurementUnitFormDTOToMeasurementUnit(measurementUnitFormDTO);
        MeasurementUnit measurementUnitSaved = measurementUnitRepository.save(measurementUnitToBeSaved);
        return convertMeasurementUnitToMeasurementUnitDTO(measurementUnitSaved);
    }

    public MeasurementUnit convertMeasurementUnitFormDTOToMeasurementUnit(MeasurementUnitFormDTO measurementUnitFormDTO) {
        return measurementUnitFormDTOToMeasurementUnitMapper.convert(measurementUnitFormDTO);
    }

    public MeasurementUnitDTO convertMeasurementUnitToMeasurementUnitDTO(MeasurementUnit measurementUnit) {
        return measurementUnitToMeasurementUnitDTOMapper.convert(measurementUnit);
    }

    public MeasurementUnit getOrThrowException(Long id) {
        if(id == null) {
            return null;
        }
        return measurementUnitRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("MeasurementUnit", id.toString()));
    }

    public MeasurementUnitDTO update(MeasurementUnit measurementUnit, MeasurementUnitFormDTO measurementUnitFormDTO) {
        if(measurementUnitFormDTO.name() != null && !measurementUnitFormDTO.name().isBlank()) {
            measurementUnit.setName(measurementUnitFormDTO.name());
        }

        if(measurementUnitFormDTO.acronym() != null && !measurementUnitFormDTO.acronym().isBlank()) {
            measurementUnit.setAcronym(measurementUnitFormDTO.acronym());
        }

        MeasurementUnit measurementUnitUpdated = measurementUnitRepository.save(measurementUnit);
        return convertMeasurementUnitToMeasurementUnitDTO(measurementUnitUpdated);
    }

    public void delete(MeasurementUnit measurementUnit) {
        measurementUnitRepository.delete(measurementUnit);
    }

    public List<MeasurementUnitDTO> list() {
        List<MeasurementUnit> measurementUnitList = measurementUnitRepository.findAll();
        return measurementUnitList.stream().map(measurementUnitToMeasurementUnitDTOMapper::convert).toList();
    }

    public Page<MeasurementUnitDTO> listByPageable(Pageable pageable) {
        Page<MeasurementUnit> measurementUnitPage = measurementUnitRepository.findAll(pageable);
        return measurementUnitPage.map(measurementUnitToMeasurementUnitDTOMapper::convert);
    }
}
