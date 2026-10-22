package com.agritrace.controller;

import com.agritrace.common.PageResult;
import com.agritrace.common.Result;
import com.agritrace.common.Roles;
import com.agritrace.entity.Product;
import com.agritrace.security.RequireRole;
import com.agritrace.service.ProductService;
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
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public Result<PageResult<Product>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String category) {
        return Result.ok(productService.page(page, size, keyword, category));
    }

    @GetMapping("/options")
    public Result<List<Product>> options() {
        return Result.ok(productService.list(new LambdaQueryWrapper<Product>().orderByAsc(Product::getId)));
    }

    @GetMapping("/{id}")
    public Result<Product> get(@PathVariable Long id) {
        return Result.ok(productService.getById(id));
    }

    @PostMapping
    @RequireRole(Roles.FARMER)
    public Result<Product> create(@Valid @RequestBody Product p) {
        return Result.ok(productService.create(p));
    }

    @PutMapping("/{id}")
    @RequireRole(Roles.FARMER)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Product p) {
        productService.update(id, p);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole(Roles.ADMIN)
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.ok();
    }
}
