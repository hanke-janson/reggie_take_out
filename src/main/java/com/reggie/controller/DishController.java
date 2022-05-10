package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.Result;
import com.reggie.dto.DishDTO;
import com.reggie.entity.Category;
import com.reggie.entity.Dish;
import com.reggie.entity.DishFlavor;
import com.reggie.service.CategoryService;
import com.reggie.service.DishFlavorService;
import com.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/dish")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     *
     * @param dishDTO 菜品数据传输对象（页面请求过来的参数无法用实体类接收，就用封装一个dto来接收）
     * @return 新增成功信息
     */
    @PostMapping
    public Result<String> save(@RequestBody DishDTO dishDTO) {
        log.info(dishDTO.toString());
        dishService.saveWithFlavor(dishDTO);
        // 数据有变化，保证数据库数据的一致性，删除redis中相关数据
        String key = "dish:" + dishDTO.getCategoryId() + ":" + dishDTO.getStatus();
        redisTemplate.delete(key);
        return Result.success("新增菜品成功！");
    }

    /**
     * 菜品信息分页查询
     *
     * @param page     当前页
     * @param pageSize 当前页数据条数
     * @param name     菜品名
     * @return 菜品信息
     */
    @GetMapping("/page")
    public Result<Page<DishDTO>> page(int page, int pageSize, String name) {
        Page<Dish> pageInfo = new Page<>(page, pageSize);
        Page<DishDTO> dishDtoPage = new Page<>();
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishLambdaQueryWrapper.like(!StringUtils.isBlank(name), Dish::getName, name);
        dishLambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);
        dishService.page(pageInfo, dishLambdaQueryWrapper);
        // 对象拷贝
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");

        List<Dish> records = pageInfo.getRecords();
        List<DishDTO> list = records.stream().map((dish -> {
            DishDTO dishDto = new DishDTO();
            // 对象拷贝
            BeanUtils.copyProperties(dish, dishDto);

            Long categoryId = dish.getCategoryId();
            // 根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        })).collect(Collectors.toList());
        dishDtoPage.setRecords(list);
        return Result.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     *
     * @param id 菜品id
     * @return DishDTO
     */
    @GetMapping("/{id}")
    public Result<DishDTO> get(@PathVariable Long id) {
        DishDTO dishDto = dishService.getByIdWithFlavor(id);
        return Result.success(dishDto);
    }

    /**
     * 修改菜品
     *
     * @param dishDTO 封装传回的数据
     * @return 修改成功信息
     */
    @PutMapping
    public Result<String> update(@RequestBody DishDTO dishDTO) {
        log.info(dishDTO.toString());
        dishService.updateWithFlavor(dishDTO);
        String keys = "dish:*";
        redisTemplate.delete(keys);
        return Result.success("修改成功！");
    }

    /**
     * 根据菜品分类ID,查询菜品列表
     *
     * @param dish 封装前端传过来的菜品id
     * @return 菜品列表
     */
    @GetMapping("/list")
    public Result<List<DishDTO>> list(Dish dish) {
        // 缓存优化
        List<DishDTO> dishDTOList = null;
        // 动态构建key
        String key = "dish:" + dish.getCategoryId() + ":" + dish.getStatus();
        // 查询redis
        dishDTOList = (List<DishDTO>) redisTemplate.opsForValue().get(key);
        // 判断是否查到值
        if (dishDTOList != null) {
            return Result.success(dishDTOList);
        }
        // 构造条件
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 在查询时，需要根据菜品分类categoryId进行查询，
        dishLambdaQueryWrapper.eq(
                dish.getCategoryId() != null,
                Dish::getCategoryId,
                dish.getCategoryId()
        );
        // 并且还要限定菜品的状态为起售状态(status为1)，
        dishLambdaQueryWrapper.eq(Dish::getStatus, 1);
        // 然后对查询的结果进行排序。
        dishLambdaQueryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> dishList = dishService.list(dishLambdaQueryWrapper);
        dishDTOList = dishList.stream().map((item) -> {
            DishDTO dishDTO = new DishDTO();
            BeanUtils.copyProperties(item, dishDTO);

            Long categoryId = item.getCategoryId();//分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                dishDTO.setCategoryName(categoryName);
            }

            //当前菜品的id
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
            dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId, dishId);
            //SQL:select * from dish_flavor where dish_id = ?
            List<DishFlavor> dishFlavorList = dishFlavorService.list(dishFlavorLambdaQueryWrapper);
            dishDTO.setFlavors(dishFlavorList);
            return dishDTO;
        }).collect(Collectors.toList());
        // 将数据库查到的值存入redis中，并设置过期时间
        redisTemplate.opsForValue().set(key, dishDTOList, 60, TimeUnit.MINUTES);
        return Result.success(dishDTOList);
    }
}
