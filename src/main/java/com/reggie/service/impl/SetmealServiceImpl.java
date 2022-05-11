package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.dto.SetmealDTO;
import com.reggie.entity.Setmeal;
import com.reggie.entity.SetmealDish;
import com.reggie.exception.CustomException;
import com.reggie.mapper.SetmealMapper;
import com.reggie.service.SetmealDishService;
import com.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    /**
     * 新增套餐，同时要保存套餐和菜品的关系
     *
     * @param setmealDTO 请求数据
     */
    @Transactional
    @Override
    public void saveWithDish(SetmealDTO setmealDTO) {
        // 保存套餐基本信息
        this.save(setmealDTO);
        // 获取套餐关联的菜品集合，并为集合中的每一个元素赋值套餐ID(setmealId)
        List<SetmealDish> setmealDishes = setmealDTO
                .getSetmealDishes()
                .stream()
                .peek(setmealDish -> setmealDish.setSetmealId(setmealDTO.getId()))
                .collect(Collectors.toList()
                );
        // 批量保存套餐关联的菜品集合
        setmealDishService.saveBatch(setmealDishes);
    }

    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 根据id删除套餐,同时需要删除套餐和菜品的关联数据
     *
     * @param ids 套餐id
     */
    @Transactional
    @Override
    public void removeWithDish(List<Long> ids) {
        //查询该批次套餐中是否存在售卖中的套餐, 如果存在, 不允许删除
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.in(ids != null, Setmeal::getId, ids);
        setmealLambdaQueryWrapper.eq(Setmeal::getStatus, 1);
        int count = this.count(setmealLambdaQueryWrapper);
        if (count > 0) {
            throw new CustomException("套餐正在售卖中，不可删除！");
        }
        //删除套餐数据
        this.removeByIds(ids);
        //删除套餐关联的菜品数据
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.in(SetmealDish::getSetmealId, ids);
        setmealDishService.remove(setmealDishLambdaQueryWrapper);
    }
}
