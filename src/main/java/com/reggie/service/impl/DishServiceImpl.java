package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.dto.DishDTO;
import com.reggie.entity.Dish;
import com.reggie.entity.DishFlavor;
import com.reggie.mapper.DishMapper;
import com.reggie.service.DishFlavorService;
import com.reggie.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    @Autowired
    private DishFlavorService dishFlavorService;

    /**
     * 新增菜品，同时保存口味数据
     *
     * @param dishDto 用于封装Controller层和Service层的数据传输
     */
    @Transactional // 多张表操作需要添加事务控制(记得在启动类启动事务管理)
    @Override
    public void saveWithFlavor(DishDTO dishDto) {
        // 先保存菜品的基本信息到菜品表
        this.save(dishDto);
        // 获取菜品id
        Long dishId = dishDto.getId();
        // 菜品口味
        List<DishFlavor> flavors = dishDto.getFlavors();
        // 通过流操作将菜品id加入到菜品口味集合里
        flavors = flavors.stream().peek((flavor) -> flavor.setDishId(dishId)).collect(Collectors.toList());
        // 保存菜品口味表到口味表
        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 根据id来查询菜品信息和对应的口味信息
     *
     * @param id 菜品id
     * @return dishDto
     */
    @Override
    public DishDTO getByIdWithFlavor(Long id) {
        // 查询菜品基本信息，从dish表查
        Dish dish = this.getById(id);

        DishDTO dishDto = new DishDTO();
        // 对象拷贝
        BeanUtils.copyProperties(dish, dishDto);

        // 查询菜品对应的口味信息，从dish_flavor表查
        LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId, dish.getId());
        List<DishFlavor> list = dishFlavorService.list(dishFlavorLambdaQueryWrapper);
        dishDto.setFlavors(list);
        return dishDto;
    }

    /**
     * 更新菜品信息，同时更新口味信息
     *
     * @param dishDto 前端传回的数据
     */
    @Override
    public void updateWithFlavor(DishDTO dishDto) {
        // 更新dish菜品表
        this.updateById(dishDto);
        // 清理当前菜品对应口味数据 -- dish_flavor表的delete操作
        LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId, dishDto.getId());
        dishFlavorService.remove(dishFlavorLambdaQueryWrapper);
        // 添加当前提交过来的口味数据 -- dish_flavor表的insert操作
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().peek(flavor -> flavor.setDishId(dishDto.getId())).collect(Collectors.toList());

        dishFlavorService.saveBatch(flavors);
    }

}
