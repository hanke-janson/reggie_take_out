package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.Result;
import com.reggie.entity.Category;
import com.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增菜品分类
     *
     * @param category 分类实体
     * @return 新增成功信息
     */
    @PostMapping
    public Result<String> save(@RequestBody Category category) {
        log.info("category:{}", category);
        categoryService.save(category);
        return Result.success("新增分类成功!");
    }

    /**
     * 分类信息分页查询
     *
     * @param page     当前页
     * @param pageSize 页面展示条数
     * @return 分类信息
     */
    @GetMapping("/page")
    public Result<Page<Category>> page(int page, int pageSize) {
        log.info("page:{},pageSize:{}", page, pageSize);
        Page<Category> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Category> categoryLambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 排序查询规则
        categoryLambdaQueryWrapper.orderByDesc(Category::getSort);
        categoryService.page(pageInfo, categoryLambdaQueryWrapper);
        return Result.success(pageInfo);
    }

    /**
     * 根据id删除分类
     *
     * @param id 分类id
     * @return 删除成功信息
     */
    @DeleteMapping
    public Result<String> delete(Long id) {
        log.info("id:{}", id);
        categoryService.remove(id);
        return Result.success("删除成功!");
    }

    /**
     * 根据id修改分类信息
     *
     * @param category 前端传过来的信息封装成的分类对象
     * @return 修改成功信息
     */
    @PutMapping
    public Result<String> update(@RequestBody Category category) {
        log.info("category:{}", category);
        categoryService.updateById(category);
        return Result.success("修改分类信息成功!");
    }

    /**
     * 根据条件来查询分类数据
     *
     * @param category 将前端传过来的数据封装成一个对象(扩展性更强)
     * @return 一个分类列表, 用于前端下拉列表展示
     */
    @GetMapping("/list")
    public Result<List<Category>> list(Category category) {
        LambdaQueryWrapper<Category> categoryLambdaQueryWrapper = new LambdaQueryWrapper<>();
        categoryLambdaQueryWrapper.eq(
                category.getType() != null,
                Category::getType,
                category.getType()
        );
        categoryLambdaQueryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);
        List<Category> categoryList = categoryService.list(categoryLambdaQueryWrapper);
        return Result.success(categoryList);
    }
}
