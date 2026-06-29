package com.smartstock.warehouse.service;

import com.smartstock.warehouse.api.dto.request.CreateWarehouseRequest;
import com.smartstock.warehouse.api.dto.request.UpdateWarehouseRequest;
import com.smartstock.warehouse.api.dto.response.*;
import com.smartstock.warehouse.domain.event.WarehouseCapacityUpdatedEvent;
import com.smartstock.warehouse.domain.event.WarehouseCreatedEvent;
import com.smartstock.warehouse.domain.event.WarehouseDeactivatedEvent;
import com.smartstock.warehouse.domain.event.WarehouseUpdatedEvent;
import com.smartstock.warehouse.domain.model.Warehouse;
import com.smartstock.warehouse.domain.model.WarehouseZone;
import com.smartstock.warehouse.domain.repository.WarehouseBinRepository;
import com.smartstock.warehouse.domain.repository.WarehouseRepository;
import com.smartstock.warehouse.domain.repository.WarehouseZoneRepository;
import com.smartstock.warehouse.exception.WarehouseCodeAlreadyExistsException;
import com.smartstock.warehouse.exception.WarehouseNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseZoneRepository zoneRepository;
    private final WarehouseBinRepository binRepository;
    private final WarehouseEventPublisher eventPublisher;

    @Transactional
    @PreAuthorize("hasAuthority('warehouse:create')")
    public WarehouseResponse createWarehouse(CreateWarehouseRequest req, String userId) {
        if (warehouseRepository.existsByCodeAndDeletedAtIsNull(req.getCode())) {
            throw new WarehouseCodeAlreadyExistsException(req.getCode());
        }

        Warehouse warehouse = Warehouse.builder()
                .code(req.getCode())
                .name(req.getName())
                .description(req.getDescription())
                .type(req.getType() != null ? req.getType() : "GENERAL")
                .active(req.isActive())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        if (req.getLocation() != null) {
            var loc = req.getLocation();
            warehouse.setAddress(loc.getAddress());
            warehouse.setCity(loc.getCity());
            warehouse.setState(loc.getState());
            warehouse.setCountry(loc.getCountry() != null ? loc.getCountry() : "US");
            warehouse.setPostalCode(loc.getZipCode());
            if (loc.getLatitude() != null) warehouse.setLatitude(BigDecimal.valueOf(loc.getLatitude()));
            if (loc.getLongitude() != null) warehouse.setLongitude(BigDecimal.valueOf(loc.getLongitude()));
        }

        if (req.getCapacity() != null) {
            var cap = req.getCapacity();
            if (cap.getMaxFloorSpace() != null) warehouse.setTotalAreaSqm(BigDecimal.valueOf(cap.getMaxFloorSpace()));
            if (cap.getMaxPallets() != null) warehouse.setTotalCapacityUnits(cap.getMaxPallets());
            if (cap.getMaxWeight() != null) warehouse.setMaxWeightKg(BigDecimal.valueOf(cap.getMaxWeight()));
        }

        if (req.getManager() != null) {
            warehouse.setManagerId(req.getManager().getUserId());
            warehouse.setManagerEmail(req.getManager().getEmail());
        }

        if (req.getOperatingHours() != null) {
            var hrs = req.getOperatingHours();
            warehouse.setHoursMondayFriday(hrs.getMondayToFriday());
            warehouse.setHoursSaturday(hrs.getSaturday());
            warehouse.setHoursSunday(hrs.getSunday());
        }

        Warehouse saved = warehouseRepository.save(warehouse);
        log.info("Warehouse created: {} ({})", saved.getCode(), saved.getId());

        eventPublisher.publishWarehouseCreated(new WarehouseCreatedEvent(
                saved.getId(), saved.getCode(), saved.getName(), saved.getType(),
                saved.getCity(), saved.getCountry(), saved.getManagerId(), userId));

        return toResponse(saved, false);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('warehouse:read')")
    public WarehouseResponse getWarehouse(String id) {
        Warehouse warehouse = findActiveOrThrow(id);
        return toResponse(warehouse, true);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('warehouse:read')")
    public PagedResponse<WarehouseResponse> listWarehouses(int page, int size,
                                                            String search, String type, Boolean active) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Warehouse> warehousePage = warehouseRepository.findAllWithFilters(
                search, type, active, pageable);

        List<WarehouseResponse> content = warehousePage.getContent()
                .stream().map(w -> toResponse(w, false)).toList();

        return PagedResponse.of(warehousePage, content);
    }

    @Transactional
    @PreAuthorize("hasAuthority('warehouse:write')")
    public WarehouseResponse updateWarehouse(String id, UpdateWarehouseRequest req, String userId) {
        Warehouse warehouse = findActiveOrThrow(id);

        Map<String, Object> previous = captureSnapshot(warehouse);

        if (req.getName() != null) warehouse.setName(req.getName());
        if (req.getDescription() != null) warehouse.setDescription(req.getDescription());

        if (req.getManager() != null) {
            if (req.getManager().getUserId() != null) warehouse.setManagerId(req.getManager().getUserId());
            if (req.getManager().getEmail() != null) warehouse.setManagerEmail(req.getManager().getEmail());
        }

        if (req.getOperatingHours() != null) {
            var hrs = req.getOperatingHours();
            if (hrs.getMondayToFriday() != null) warehouse.setHoursMondayFriday(hrs.getMondayToFriday());
            if (hrs.getSaturday() != null) warehouse.setHoursSaturday(hrs.getSaturday());
            if (hrs.getSunday() != null) warehouse.setHoursSunday(hrs.getSunday());
        }

        if (req.getCapacity() != null) {
            var cap = req.getCapacity();
            if (cap.getMaxFloorSpace() != null) warehouse.setTotalAreaSqm(BigDecimal.valueOf(cap.getMaxFloorSpace()));
            if (cap.getMaxPallets() != null) warehouse.setTotalCapacityUnits(cap.getMaxPallets());
            if (cap.getMaxWeight() != null) warehouse.setMaxWeightKg(BigDecimal.valueOf(cap.getMaxWeight()));
        }

        boolean capacityChanged = req.getCapacity() != null;

        warehouse.setUpdatedBy(userId);
        Warehouse saved = warehouseRepository.save(warehouse);

        Map<String, Object> changes = captureSnapshot(saved);
        eventPublisher.publishWarehouseUpdated(new WarehouseUpdatedEvent(saved.getId(), userId, changes, previous));

        if (capacityChanged) {
            BigDecimal total = saved.getTotalAreaSqm() != null ? saved.getTotalAreaSqm() : BigDecimal.ZERO;
            BigDecimal used = saved.getUsedWeightKg() != null ? saved.getUsedWeightKg() : BigDecimal.ZERO;
            BigDecimal pct = saved.getCurrentUtilizationPercentage() != null
                    ? saved.getCurrentUtilizationPercentage() : BigDecimal.ZERO;
            eventPublisher.publishCapacityUpdated(new WarehouseCapacityUpdatedEvent(
                    saved.getId(), saved.getName(), total, used, pct, userId));
        }

        return toResponse(saved, false);
    }

    @Transactional
    @PreAuthorize("hasAuthority('warehouse:write')")
    public WarehouseResponse deactivateWarehouse(String id, String userId) {
        Warehouse warehouse = findActiveOrThrow(id);
        warehouse.deactivate(userId);
        Warehouse saved = warehouseRepository.save(warehouse);
        eventPublisher.publishWarehouseDeactivated(
                new WarehouseDeactivatedEvent(saved.getId(), saved.getCode(), saved.getName(), userId));
        return toResponse(saved, false);
    }

    @Transactional
    @PreAuthorize("hasAuthority('warehouse:write')")
    public WarehouseResponse reactivateWarehouse(String id, String userId) {
        Warehouse warehouse = warehouseRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new WarehouseNotFoundException(id));
        warehouse.reactivate(userId);
        return toResponse(warehouseRepository.save(warehouse), false);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('warehouse:report')")
    public CapacityReportResponse getCapacityReport(String warehouseId) {
        Warehouse warehouse = findActiveOrThrow(warehouseId);
        List<WarehouseZone> zones = zoneRepository.findAllByWarehouseId(warehouseId);

        double totalFloor = warehouse.getTotalAreaSqm() != null ? warehouse.getTotalAreaSqm().doubleValue() : 0;
        double usedFloor = warehouse.getTotalAreaSqm() != null
                ? warehouse.getTotalAreaSqm().doubleValue() * (warehouse.getCurrentUtilizationPercentage() != null
                ? warehouse.getCurrentUtilizationPercentage().doubleValue() / 100.0 : 0) : 0;
        double totalWeight = warehouse.getMaxWeightKg() != null ? warehouse.getMaxWeightKg().doubleValue() : 0;
        double usedWeight = warehouse.getUsedWeightKg() != null ? warehouse.getUsedWeightKg().doubleValue() : 0;
        int totalPallets = warehouse.getTotalCapacityUnits() != null ? warehouse.getTotalCapacityUnits() : 0;

        List<CapacityReportResponse.ZoneSummary> zoneSummaries = zones.stream().map(z -> {
            int totalBins = z.getTotalBinCount();
            int occupiedBins = z.getShelves().stream()
                    .filter(s -> !s.isDeleted())
                    .mapToInt(s -> (int) s.getBins().stream().filter(b -> !b.isDeleted() && b.getCurrentUnits() > 0).count())
                    .sum();
            return CapacityReportResponse.ZoneSummary.builder()
                    .zoneId(z.getId())
                    .zoneName(z.getName())
                    .utilizationPercentage(z.getOccupancyPercentage())
                    .shelves(z.getShelfCount())
                    .bins(totalBins)
                    .occupiedBins(occupiedBins)
                    .build();
        }).toList();

        double floorUtil = totalFloor > 0 ? usedFloor / totalFloor * 100 : 0;
        double weightUtil = totalWeight > 0 ? usedWeight / totalWeight * 100 : 0;

        List<CapacityReportResponse.Alert> alerts = new ArrayList<>();
        if (weightUtil >= 100) alerts.add(alert("CRITICAL", "Weight capacity at 100% — immediate action required"));
        else if (weightUtil >= 90) alerts.add(alert("WARNING", "Weight capacity at " + String.format("%.0f", weightUtil) + "%, consider redistribution"));
        else if (weightUtil >= 80) alerts.add(alert("INFO", "Weight capacity at " + String.format("%.0f", weightUtil) + "%"));

        if (floorUtil >= 90) alerts.add(alert("WARNING", "Floor space at " + String.format("%.0f", floorUtil) + "%"));

        return CapacityReportResponse.builder()
                .warehouseId(warehouseId)
                .warehouseName(warehouse.getName())
                .reportDate(LocalDate.now().toString())
                .floorSpace(metric(totalFloor, usedFloor))
                .pallets(metric(totalPallets, 0))
                .weight(metric(totalWeight, usedWeight))
                .zones(zoneSummaries)
                .alerts(alerts)
                .build();
    }

    // ---- private helpers ----

    private Warehouse findActiveOrThrow(String id) {
        return warehouseRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new WarehouseNotFoundException(id));
    }

    private Map<String, Object> captureSnapshot(Warehouse w) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("name", w.getName());
        snap.put("description", w.getDescription());
        snap.put("managerId", w.getManagerId());
        snap.put("active", w.isActive());
        return snap;
    }

    private CapacityReportResponse.CapacityMetric metric(double total, double used) {
        double available = total - used;
        double util = total > 0 ? used / total * 100 : 0;
        return CapacityReportResponse.CapacityMetric.builder()
                .total(total).used(used).available(available).utilizationPercentage(util).build();
    }

    private CapacityReportResponse.Alert alert(String level, String message) {
        return CapacityReportResponse.Alert.builder()
                .level(level).type("CAPACITY_THRESHOLD").message(message).build();
    }

    public WarehouseResponse toResponse(Warehouse w, boolean includeStats) {
        WarehouseResponse.WarehouseResponseBuilder builder = WarehouseResponse.builder()
                .id(w.getId())
                .code(w.getCode())
                .name(w.getName())
                .description(w.getDescription())
                .type(w.getType())
                .active(w.isActive())
                .createdAt(w.getCreatedAt() != null ? w.getCreatedAt().toString() : null)
                .updatedAt(w.getUpdatedAt() != null ? w.getUpdatedAt().toString() : null)
                .deactivatedAt(w.getDeactivatedAt() != null ? w.getDeactivatedAt().toString() : null)
                .location(WarehouseResponse.LocationData.builder()
                        .address(w.getAddress())
                        .city(w.getCity())
                        .state(w.getState())
                        .country(w.getCountry())
                        .zipCode(w.getPostalCode())
                        .latitude(w.getLatitude())
                        .longitude(w.getLongitude())
                        .build())
                .capacity(WarehouseResponse.CapacityData.builder()
                        .maxFloorSpace(w.getTotalAreaSqm())
                        .usedFloorSpace(BigDecimal.ZERO)
                        .availableFloorSpace(w.getTotalAreaSqm())
                        .utilizationPercentage(w.getCurrentUtilizationPercentage())
                        .maxPallets(w.getTotalCapacityUnits())
                        .usedPallets(0)
                        .maxWeight(w.getMaxWeightKg())
                        .usedWeight(w.getUsedWeightKg())
                        .build())
                .manager(WarehouseResponse.ManagerData.builder()
                        .userId(w.getManagerId())
                        .email(w.getManagerEmail())
                        .build())
                .operatingHours(WarehouseResponse.OperatingHoursData.builder()
                        .mondayToFriday(w.getHoursMondayFriday())
                        .saturday(w.getHoursSaturday())
                        .sunday(w.getHoursSunday())
                        .build());

        if (includeStats) {
            builder.zoneCount(w.getZoneCount())
                   .totalBins(binRepository.countByWarehouseId(w.getId()))
                   .occupiedBins(binRepository.countOccupiedByWarehouseId(w.getId()));
        }

        return builder.build();
    }
}
