package com.menumaster.restaurant.measurementunit.controller;

import com.menumaster.restaurant.measurementunit.domain.dto.MeasurementUnitDTO;
import com.menumaster.restaurant.measurementunit.domain.dto.MeasurementUnitFormDTO;
import com.menumaster.restaurant.measurementunit.domain.model.MeasurementUnit;
import com.menumaster.restaurant.measurementunit.service.MeasurementUnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/measurement-unit")
@RequiredArgsConstructor
public class MeasurementUnitController {

    private final MeasurementUnitService measurementUnitService;

    @PostMapping("/create")
    public ResponseEntity<MeasurementUnitDTO> createMeasurementUnit(@Valid @RequestBody MeasurementUnitFormDTO measurementUnitFormDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(measurementUnitService.create(measurementUnitFormDTO));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<MeasurementUnitDTO> updateMeasurementUnit(@PathVariable Long id, @RequestBody MeasurementUnitFormDTO measurementUnitFormDTO) {
        MeasurementUnit measurementUnit = measurementUnitService.getOrThrowException(id);
        return ResponseEntity.status(HttpStatus.OK).body(measurementUnitService.update(measurementUnit, measurementUnitFormDTO));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteMeasurementUnit(@PathVariable Long id) {
        MeasurementUnit measurementUnit = measurementUnitService.getOrThrowException(id);
        measurementUnitService.delete(measurementUnit);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<MeasurementUnitDTO>> getMeasurementUnitDTOList() {
        return ResponseEntity.status(HttpStatus.OK).body(measurementUnitService.list());
    }

    @GetMapping("/page")
    public ResponseEntity<Page<MeasurementUnitDTO>> getMeasurementUnitDTOPage(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(measurementUnitService.listByPageable(pageable));
    }
}
