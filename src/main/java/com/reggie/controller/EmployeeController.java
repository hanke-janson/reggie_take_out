package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.Result;
import com.reggie.entity.Employee;
import com.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登录
     *
     * @param request  获取session以便添加员工id
     * @param employee 用于封装用户名及密码属性
     * @return 返回登录成功, 登录失败, 员工禁用等信息
     */
    @PostMapping("/login")
    public Result<Employee> login(HttpServletRequest request, @RequestBody Employee employee) {
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        LambdaQueryWrapper<Employee> employeeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        employeeLambdaQueryWrapper.eq(Employee::getUsername, employee.getUsername());
        // 数据库中的用户名进行了唯一约束处理
        Employee emp = employeeService.getOne(employeeLambdaQueryWrapper);

        if (emp == null) {
            return Result.error("登录失败!");
        }
        if (!emp.getPassword().equals(password)) {
            return Result.error("登录失败!");
        }
        if (emp.getStatus() == 0) {
            return Result.error("员工已禁用!");
        }
        request.getSession().setAttribute("emp_id", emp.getId());
        return Result.success(emp);
    }

    /**
     * 员工退出
     *
     * @param request 获取session以便删除员工id
     * @return 退出成功信息
     */
    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        request.getSession().removeAttribute("emp_id");
        return Result.success("退出成功!");
    }

    /**
     * 新增员工
     *
     * @param employee 前端json格式传过来的employee对象
     * @return 新增成功信息
     */
    @PostMapping
    public Result<String> save(HttpServletRequest request, @RequestBody Employee employee) {
        log.info("新增员工:{}", employee.toString());
        // 设置初始密码123456,进行md5加密
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
        // 获取当前时间
//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());
//        // 获取当前登录用户Id
//        Long empId = (Long) request.getSession().getAttribute("emp_id");
//        employee.setCreateUser(empId);
//        employee.setUpdateUser(empId);
        employeeService.save(employee);
        return Result.success("新增员工成功!");
    }

    /**
     * 员工信息的分页查询
     *
     * @param page     当前页
     * @param pageSize 显示条数
     * @param name     查询名字
     * @return 员工查询后的信息
     */
    @GetMapping("/page")
    public Result<Page<Employee>> page(int page, int pageSize, String name) {
        log.info("page = {}, pageSize = {}, name = {}", page, pageSize, name);
        //构造分页构造器
        Page<Employee> pageInfo = new Page<>(page, pageSize);
        //构造条件构造器
        LambdaQueryWrapper<Employee> employeeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        employeeLambdaQueryWrapper.like(StringUtils.isNotBlank(name), Employee::getName, name);
        //排序条件
        employeeLambdaQueryWrapper.orderByDesc(Employee::getUpdateTime);
        //执行查询
        employeeService.page(pageInfo, employeeLambdaQueryWrapper);
        return Result.success(pageInfo);
    }

    /**
     * 根据id修改员工信息
     *
     * @param employee 前端以json格式传过来的id和状态信息封装为一个Employee对象
     * @return 修改成功信息
     */
    @PutMapping
    public Result<String> update(HttpServletRequest request, @RequestBody Employee employee) {
        log.info(employee.toString());

        long id = Thread.currentThread().getId();
        log.info("线程id:{}", id);

//        employee.setUpdateTime(LocalDateTime.now());
//        Long empId = (Long) request.getSession().getAttribute("emp_id");
//        employee.setUpdateUser(empId);
        employeeService.updateById(employee);
        return Result.success("员工信息修改成功!");
    }

    /**
     * 根据id查询员工信息
     *
     * @param id 前端页面传过来的员工账户id
     * @return 员工信息回显
     */
    @GetMapping("/{id}")
    public Result<Employee> getById(@PathVariable Long id) {
        log.info("根据id查询员工信息");
        Employee employee = employeeService.getById(id);
        if (employee != null) {
            return Result.success(employee);
        }
        return Result.error("没有查询到对应员工信息");
    }
}
