package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.dto.DishDTO;
import com.reggie.entity.Dish;

public interface DishService extends IService<Dish> {
    /**
     * 新增菜品，同时保存口味数据
     */
    void saveWithFlavor(DishDTO dishDto);

    /**
     * 根据id来查询菜品信息和对应的口味信息
     * @param id 菜品id
     * @return DishDTO
     */
    DishDTO getByIdWithFlavor(Long id);

    /**
     * 更新菜品信息，同时更新口味信息
     * @param dishDto 前端传回的数据
     */
    void updateWithFlavor(DishDTO dishDto);
}
