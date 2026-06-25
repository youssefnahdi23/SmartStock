package com.smartstock.warehouse.service;

import com.smartstock.warehouse.api.dto.request.CreateBinRequest;
import com.smartstock.warehouse.api.dto.request.CreateShelfRequest;
import com.smartstock.warehouse.api.dto.request.CreateZoneRequest;
import com.smartstock.warehouse.api.dto.response.*;
import com.smartstock.warehouse.domain.event.BinCreatedEvent;
import com.smartstock.warehouse.domain.event.ShelfCreatedEvent;
import com.smartstock.warehouse.domain.event.ZoneCreatedEvent;
import com.smartstock.warehouse.domain.model.Warehouse;
import com.smartstock.warehouse.domain.model.WarehouseBin;
import com.smartstock.warehouse.domain.model.WarehouseShelf;
import com.smartstock.warehouse.domain.model.WarehouseZone;
import com.smartstock.warehouse.domain.repository.*;
import com.smartstock.warehouse.exception.*;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZoneService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseZoneRepository zoneRepository;
    private final WarehouseShelfRepository shelfRepository;
    private final WarehouseBinRepository binRepository;
    private final WarehouseEventPublisher eventPublisher;

    // ---- ZONES ----

    @Transactional
    @PreAuthorize("hasAuthority('warehouse:zone:create')")
    public ZoneResponse createZone(String warehouseId, CreateZoneRequest req, String userId) {
        Warehouse warehouse = warehouseRepository.findByIdAndNotDeleted(warehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException(warehouseId));

        if (zoneRepository.existsByWarehouseIdAndCodeAndDeletedAtIsNull(warehouseId, req.getCode())) {
            throw new BusinessException("Zone code already exists in this warehouse: " + req.getCode(),
                    org.springframework.http.HttpStatus.CONFLICT, "ZONE_CODE_EXISTS");
        }

        WarehouseZone zone = WarehouseZone.builder()
                .warehouse(warehouse)
                .code(req.getCode())
                .name(req.getName())
                .description(req.getDescription())
                .type(req.getType())
                .active(true)
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        if (req.getFloorSpace() != null) zone.setAreaSqm(BigDecimal.valueOf(req.getFloorSpace()));
        if (req.getMaxCapacity() != null) zone.setCapacityUnits(req.getMaxCapacity());

        if (req.getTemperature() != null) {
            var temp = req.getTemperature();
            zone.setTemperatureControlled(true);
            if (temp.getMin() != null) zone.setMinTemperature(BigDecimal.valueOf(temp.getMin()));
            if (temp.getMax() != null) zone.setMaxTemperature(BigDecimal.valueOf(temp.getMax()));
            zone.setTemperatureUnit(temp.getUnit() != null ? temp.getUnit() : "CELSIUS");
        }

        WarehouseZone saved = zoneRepository.save(zone);
        log.info("Zone created: {} in warehouse {}", saved.getCode(), warehouseId);

        eventPublisher.publishZoneCreated(new ZoneCreatedEvent(
                saved.getId(), warehouseId, saved.getCode(), saved.getName(), saved.getType(), userId));

        return toZoneResponse(saved);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('warehouse:read')")
    public PagedResponse<ZoneResponse> listZones(String warehouseId, int page, int size) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new WarehouseNotFoundException(warehouseId);
        }
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").ascending());
        Page<WarehouseZone> zonePage = zoneRepository.findByWarehouseId(warehouseId, pageable);
        List<ZoneResponse> content = zonePage.getContent().stream().map(this::toZoneResponse).toList();
        return PagedResponse.of(zonePage, content);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('warehouse:read')")
    public ZoneResponse getZone(String warehouseId, String zoneId) {
        WarehouseZone zone = zoneRepository.findByIdAndNotDeleted(zoneId)
                .filter(z -> z.getWarehouse().getId().equals(warehouseId))
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));
        return toZoneResponse(zone);
    }

    // ---- SHELVES ----

    @Transactional
    @PreAuthorize("hasAuthority('warehouse:shelf:create')")
    public ShelfResponse createShelf(String warehouseId, String zoneId, CreateShelfRequest req, String userId) {
        WarehouseZone zone = zoneRepository.findByIdAndNotDeleted(zoneId)
                .filter(z -> z.getWarehouse().getId().equals(warehouseId))
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));

        if (shelfRepository.existsByZoneIdAndCodeAndDeletedAtIsNull(zoneId, req.getCode())) {
            throw new BusinessException("Shelf code already exists in this zone: " + req.getCode(),
                    org.springframework.http.HttpStatus.CONFLICT, "SHELF_CODE_EXISTS");
        }

        WarehouseShelf shelf = WarehouseShelf.builder()
                .zone(zone)
                .code(req.getCode())
                .name(req.getName())
                .level(req.getLevel())
                .capacityUnits(req.getCapacity())
                .active(true)
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        if (req.getWeightLimit() != null) shelf.setMaxWeightKg(BigDecimal.valueOf(req.getWeightLimit()));

        WarehouseShelf saved = shelfRepository.save(shelf);
        log.info("Shelf created: {} in zone {}", saved.getCode(), zoneId);

        eventPublisher.publishShelfCreated(new ShelfCreatedEvent(
                saved.getId(), zoneId, warehouseId, saved.getCode(), saved.getName(), userId));

        return toShelfResponse(saved);
    }

    // ---- BINS ----

    @Transactional
    @PreAuthorize("hasAuthority('warehouse:bin:create')")
    public BinResponse createBin(String warehouseId, String zoneId, String shelfId,
                                  CreateBinRequest req, String userId) {
        WarehouseShelf shelf = shelfRepository.findByIdAndNotDeleted(shelfId)
                .filter(s -> s.getZone().getId().equals(zoneId)
                          && s.getZone().getWarehouse().getId().equals(warehouseId))
                .orElseThrow(() -> new ShelfNotFoundException(shelfId));

        if (binRepository.existsByCodeAndDeletedAtIsNull(req.getCode())) {
            throw new BusinessException("Bin code already exists: " + req.getCode(),
                    org.springframework.http.HttpStatus.CONFLICT, "BIN_CODE_EXISTS");
        }

        WarehouseBin bin = WarehouseBin.builder()
                .shelf(shelf)
                .code(req.getCode())
                .name(req.getName() != null ? req.getName() : req.getCode())
                .number(req.getPosition())
                .position(req.getPosition() != null ? String.valueOf(req.getPosition()) : null)
                .type(req.getType() != null ? req.getType() : "STANDARD")
                .capacityUnits(req.getCapacity())
                .active(true)
                .build();

        WarehouseBin saved = binRepository.save(bin);
        log.info("Bin created: {} in shelf {}", saved.getCode(), shelfId);

        eventPublisher.publishBinCreated(new BinCreatedEvent(
                saved.getId(), shelfId, zoneId, warehouseId, saved.getCode(), saved.getType(),
                saved.getCapacityUnits() != null ? saved.getCapacityUnits() : 0));

        return toBinResponse(saved);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('warehouse:read')")
    public List<AvailableBinResponse> findAvailableBins(String warehouseId, String zoneId, int quantity) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new WarehouseNotFoundException(warehouseId);
        }
        List<WarehouseBin> bins = binRepository.findAvailableBins(warehouseId, zoneId, quantity);
        return bins.stream().map(b -> AvailableBinResponse.builder()
                .binId(b.getId())
                .code(b.getCode())
                .zoneId(b.getShelf().getZone().getId())
                .zoneName(b.getShelf().getZone().getName())
                .shelfId(b.getShelf().getId())
                .position(b.getPosition())
                .availableCapacity(b.getAvailableCapacity())
                .compatibility("COMPATIBLE")
                .build()).toList();
    }

    // ---- mappers ----

    private ZoneResponse toZoneResponse(WarehouseZone z) {
        ZoneResponse.ZoneResponseBuilder builder = ZoneResponse.builder()
                .id(z.getId())
                .warehouseId(z.getWarehouse().getId())
                .code(z.getCode())
                .name(z.getName())
                .description(z.getDescription())
                .type(z.getType())
                .floorSpace(z.getAreaSqm())
                .maxCapacity(z.getCapacityUnits())
                .usedCapacity(z.getCurrentUtilization())
                .occupancyPercentage(z.getOccupancyPercentage())
                .shelfCount(z.getShelfCount())
                .totalBins(z.getTotalBinCount())
                .active(z.isActive())
                .createdAt(z.getCreatedAt() != null ? z.getCreatedAt().toString() : null);

        if (z.isTemperatureControlled()) {
            builder.temperature(ZoneResponse.TemperatureData.builder()
                    .min(z.getMinTemperature())
                    .max(z.getMaxTemperature())
                    .unit(z.getTemperatureUnit())
                    .build());
        }
        return builder.build();
    }

    private ShelfResponse toShelfResponse(WarehouseShelf s) {
        return ShelfResponse.builder()
                .id(s.getId())
                .zoneId(s.getZone().getId())
                .code(s.getCode())
                .name(s.getName())
                .level(s.getLevel())
                .capacity(s.getCapacityUnits())
                .usedCapacity(s.getCurrentLoadUnits())
                .weightLimit(s.getMaxWeightKg())
                .binCount(s.getBinCount())
                .active(s.isActive())
                .createdAt(s.getCreatedAt() != null ? s.getCreatedAt().toString() : null)
                .build();
    }

    private BinResponse toBinResponse(WarehouseBin b) {
        return BinResponse.builder()
                .id(b.getId())
                .shelfId(b.getShelf().getId())
                .code(b.getCode())
                .name(b.getName())
                .position(b.getNumber())
                .capacity(b.getCapacityUnits())
                .usedCapacity(b.getCurrentUnits())
                .availableCapacity(b.getAvailableCapacity())
                .type(b.getType())
                .active(b.isActive())
                .full(b.isFull())
                .currentProductId(b.getCurrentProductId())
                .maxWeightKg(b.getMaxWeightKg())
                .createdAt(b.getCreatedAt() != null ? b.getCreatedAt().toString() : null)
                .build();
    }
}
