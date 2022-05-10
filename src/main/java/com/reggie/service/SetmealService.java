package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.dto.setmealDTO;
import com.reggie.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    /**
     * 新增套餐，同时要保存套餐和菜品的关系
     * @param setmealDTO 前端请求信息
     */
    void saveWithDish(setmealDTO setmealDTO);

    /**
     * 根据id删除套餐,同时需要删除套餐和菜品的关联数据
     * @param ids 套餐id
     */
    void removeWithDish(List<Long> ids);
}
