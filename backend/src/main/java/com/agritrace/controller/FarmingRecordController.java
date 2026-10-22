package com.agritrace.controller;

import com.agritrace.common.PageResult;
import com.agritrace.common.Result;
import com.agritrace.common.Roles;
import com.agritrace.entity.FarmingRecord;
import com.agritrace.security.RequireRole;
import com.agritrace.security.UserContext;
import com.agritrace.service.FarmingRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/farming-records")
@RequiredArgsConstructor
public class FarmingRecordController {
    private final FarmingRecordService service;

    @GetMapping
    public Result<PageResult<FarmingRecord>> page(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(required = false) Long batchId,
                                                  @RequestParam(required = false) String type) {
        return Result.ok(service.page(page, size, batchId, type));
    }

    @PostMapping
    @RequireRole(Roles.FARMER)
    public Result<FarmingRecord> create(@Valid @RequestBody FarmingRecord r) {
        return Result.ok(service.create(r, UserContext.userId()));
    }

    @PutMapping("/{id}")
    @RequireRole(Roles.FARMER)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody FarmingRecord r) {
        service.update(id, r);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole(Roles.FARMER)
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
