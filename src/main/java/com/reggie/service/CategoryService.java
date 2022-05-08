package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.entity.Category;

public interface CategoryService extends IService<Category> {
    /**
     * 根据id删除分类
     * @param id 分类id
     */
    void remove(Long id);
}
