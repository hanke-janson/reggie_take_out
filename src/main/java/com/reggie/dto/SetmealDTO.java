package com.reggie.dto;

import com.reggie.entity.Setmeal;
import com.reggie.entity.SetmealDish;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDTO extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
