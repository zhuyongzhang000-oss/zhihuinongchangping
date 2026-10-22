package com.agritrace.controller;

import com.agritrace.common.PageResult;
import com.agritrace.common.Result;
import com.agritrace.common.Roles;
import com.agritrace.dto.BatchDetail;
import com.agritrace.entity.Batch;
import com.agritrace.security.RequireRole;
import com.agritrace.security.UserContext;
import com.agritrace.service.BatchService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
public class BatchController {
    private final BatchService batchService;

    @GetMapping
    public Result<PageResult<Batch>> page(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) Long productId,
                                          @RequestParam(required = false) Long baseId) {
        return Result.ok(batchService.page(page, size, keyword, status, productId, baseId));
    }

    /** 下拉框：?status=HARVESTED,INSPECTED 这样按状态过滤，不传则全部 */
    @GetMapping("/options")
    public Result<List<Batch>> options(@RequestParam(required = false) List<String> status) {
        return Result.ok(batchService.options(status));
    }

    @GetMapping("/{id}")
    public Result<BatchDetail> detail(@PathVariable Long id) {
        return Result.ok(batchService.detail(id));
    }

    @PostMapping
    @RequireRole(Roles.FARMER)
    public Result<Batch> create(@Valid @RequestBody Batch b) {
        return Result.ok(batchService.create(b, UserContext.userId()));
    }

    @PutMapping("/{id}")
    @RequireRole(Roles.FARMER)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Batch b) {
        batchService.update(id, b);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @RequireRole(Roles.ADMIN)
    public Result<Void> status(@PathVariable Long id, @RequestBody Map<String, String> body) {
        batchService.changeStatus(id, body.get("status"));
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole(Roles.ADMIN)
    public Result<Void> delete(@PathVariable Long id) {
        batchService.delete(id);
        return Result.ok();
    }
}
