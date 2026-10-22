package com.agritrace.controller;

import com.agritrace.common.PageResult;
import com.agritrace.common.Result;
import com.agritrace.common.Roles;
import com.agritrace.entity.FarmBase;
import com.agritrace.security.RequireRole;
import com.agritrace.service.FarmBaseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

@RestController
@RequestMapping("/api/bases")
@RequiredArgsConstructor
public class FarmBaseController {
    private final FarmBaseService baseService;

    @GetMapping
    public Result<PageResult<FarmBase>> page(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status) {
        return Result.ok(baseService.page(page, size, keyword, status));
    }

    /** 下拉框用，只列使用中的 */
    @GetMapping("/options")
    public Result<List<FarmBase>> options() {
        return Result.ok(baseService.list(new LambdaQueryWrapper<FarmBase>().eq(FarmBase::getStatus, 1).orderByAsc(FarmBase::getId)));
    }

    @GetMapping("/{id}")
    public Result<FarmBase> get(@PathVariable Long id) {
        return Result.ok(baseService.getById(id));
    }

    @PostMapping
    @RequireRole(Roles.FARMER)
    public Result<FarmBase> create(@Valid @RequestBody FarmBase b) {
        return Result.ok(baseService.create(b));
    }

    @PutMapping("/{id}")
    @RequireRole(Roles.FARMER)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody FarmBase b) {
        baseService.update(id, b);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole(Roles.ADMIN)
    public Result<Void> delete(@PathVariable Long id) {
        baseService.delete(id);
        return Result.ok();
    }
}
