package com.agritrace.controller;

import com.agritrace.common.PageResult;
import com.agritrace.common.Result;
import com.agritrace.common.Roles;
import com.agritrace.config.JacksonConfig;
import com.agritrace.entity.Logistics;
import com.agritrace.security.RequireRole;
import com.agritrace.security.UserContext;
import com.agritrace.service.LogisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/logistics")
@RequiredArgsConstructor
public class LogisticsController {
    private final LogisticsService service;

    @GetMapping
    public Result<PageResult<Logistics>> page(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) Long batchId,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String keyword) {
        return Result.ok(service.page(page, size, batchId, status, keyword));
    }

    @PostMapping
    @RequireRole(Roles.LOGISTICS)
    public Result<Logistics> create(@Valid @RequestBody Logistics r) {
        return Result.ok(service.create(r, UserContext.userId()));
    }

    @PutMapping("/{id}")
    @RequireRole(Roles.LOGISTICS)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Logistics r) {
        service.update(id, r);
        return Result.ok();
    }

    /** 确认到达，body 可带 arriveTime，不带则取当前时间 */
    @PutMapping("/{id}/arrive")
    @RequireRole(Roles.LOGISTICS)
    public Result<Void> arrive(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String t = body == null ? null : body.get("arriveTime");
        service.arrive(id, StringUtils.hasText(t) ? LocalDateTime.parse(t, JacksonConfig.DATE_TIME) : null);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole(Roles.LOGISTICS)
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
