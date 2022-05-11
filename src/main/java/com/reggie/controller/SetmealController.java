package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.Result;
import com.reggie.dto.SetmealDTO;
import com.reggie.entity.Category;
import com.reggie.entity.Setmeal;
import com.reggie.service.CategoryService;
import com.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */
@Slf4j
@RestController
@RequestMapping("/setmeal")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 保存套餐信息
     *
     * @param setmealDTO 用于封装前端传过来的参数
     * @return 保存成功信息
     */
    @PostMapping
    public Result<String> save(@RequestBody SetmealDTO setmealDTO) {
        setmealService.saveWithDish(setmealDTO);
        String key = "setmeal:*";
        redisTemplate.delete(key);
        return Result.success("新增套餐成功！");
    }

    /**
     * 套餐分页查询
     *
     * @param page     当前页码
     * @param pageSize 当前页记录数
     * @param name     套餐名，作为可选条件参与条件查询
     * @return 套餐分页数据
     */
    @GetMapping("/page")
    public Result<Page<SetmealDTO>> page(int page, int pageSize, String name) {
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);
        Page<SetmealDTO> setmealDTOPage = new Page<>();
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.like(!StringUtils.isBlank(name), Setmeal::getName, name);
        setmealLambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime);
        setmealService.page(pageInfo, setmealLambdaQueryWrapper);

        BeanUtils.copyProperties(pageInfo, setmealDTOPage, "records");
        List<Setmeal> records = pageInfo.getRecords();
        List<SetmealDTO> list = records.stream().map(setmeal -> {
            SetmealDTO setmealDTO = new SetmealDTO();
            // 对象拷贝
            BeanUtils.copyProperties(setmeal, setmealDTO);
            // 获取菜品分类名
            Long categoryId = setmeal.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                setmealDTO.setCategoryName(categoryName);
            }
            return setmealDTO;
        }).collect(Collectors.toList());
        setmealDTOPage.setRecords(list);
        return Result.success(setmealDTOPage);
    }

    /**
     * 根据id删除套餐
     *
     * @param ids 前端请求的套餐id
     * @return 删除成功信息
     */
    @DeleteMapping
    public Result<String> delete(@RequestParam List<Long> ids) {
        log.info("将要被删除的套餐id:{}", ids);
        setmealService.removeWithDish(ids);
        String key = "setmeal:*";
        redisTemplate.delete(key);
        return Result.success("套餐数据删除成功！");
    }

    /**
     * 根据条件查询套餐数据
     *
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    public Result<List<Setmeal>> list(Setmeal setmeal) {
        List<Setmeal> list = null;
        String key = "setmeal:" + setmeal.getCategoryId() + ":" + setmeal.getStatus();
        list = (List<Setmeal>) redisTemplate.opsForValue().get(key);
        if (list != null) {
            return Result.success(list);
        }
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        list = setmealService.list(queryWrapper);
        redisTemplate.opsForValue().set(key, list, 60, TimeUnit.MINUTES);
        return Result.success(list);
    }
}
