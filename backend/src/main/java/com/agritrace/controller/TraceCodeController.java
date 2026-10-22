package com.agritrace.controller;

import com.agritrace.common.PageResult;
import com.agritrace.common.Result;
import com.agritrace.common.Roles;
import com.agritrace.entity.TraceCode;
import com.agritrace.security.RequireRole;
import com.agritrace.service.TraceCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/trace-codes")
@RequiredArgsConstructor
public class TraceCodeController {
    private final TraceCodeService service;

    @GetMapping
    public Result<PageResult<TraceCode>> page(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer status) {
        return Result.ok(service.page(page, size, keyword, status));
    }

    @PostMapping("/batch/{batchId}")
    @RequireRole(Roles.FARMER)
    public Result<TraceCode> generate(@PathVariable Long batchId) {
        return Result.ok(service.generate(batchId));
    }

    @PutMapping("/{id}/status")
    @RequireRole(Roles.ADMIN)
    public Result<Void> status(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        service.changeStatus(id, body.get("status"));
        return Result.ok();
    }
}
