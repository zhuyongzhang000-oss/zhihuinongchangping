package com.agritrace.controller;

import com.agritrace.common.PageResult;
import com.agritrace.common.Result;
import com.agritrace.common.Roles;
import com.agritrace.entity.Inspection;
import com.agritrace.security.RequireRole;
import com.agritrace.security.UserContext;
import com.agritrace.service.InspectionService;
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
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
public class InspectionController {
    private final InspectionService service;

    @GetMapping
    public Result<PageResult<Inspection>> page(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) Long batchId,
                                               @RequestParam(required = false) String result,
                                               @RequestParam(required = false) String keyword) {
        return Result.ok(service.page(page, size, batchId, result, keyword));
    }

    @PostMapping
    @RequireRole(Roles.INSPECTOR)
    public Result<Inspection> create(@Valid @RequestBody Inspection r) {
        return Result.ok(service.create(r, UserContext.userId()));
    }

    @PutMapping("/{id}")
    @RequireRole(Roles.INSPECTOR)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Inspection r) {
        service.update(id, r);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole(Roles.INSPECTOR)
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
