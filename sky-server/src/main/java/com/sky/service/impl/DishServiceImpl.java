package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.google.common.collect.Lists;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 新增菜品
     * @param dishDTO
     */
    @Transactional
    public void saveWithFlavors(DishDTO dishDTO) {

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        // 新增菜品
        dishMapper.insert(dish);

        //返回dish的id
        Long dishId = dish.getId();

        //绑定口味
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size()>0){
            flavors.forEach(flavor -> {
                flavor.setDishId(dishId);
            });

            //插入该dish的口味
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());

        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 菜品批量删除
     * @param ids
     */
    public void deleteBatch(List<Long> ids) {
        //判断菜品是否 启售中
        for (Long id : ids) {
            Dish dish  =dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断菜品是否被套餐关联
        List<Long> setMealId = setmealDishMapper.getSetmealIdByDishIds(ids);
        if(setMealId != null && setMealId.size() > 0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品及其口味数据
        dishMapper.deleteByIds(ids);

        dishFlavorMapper.deleteByDishIds(ids);
    }

    /**
     * 根据id查询菜品
     * @param id
     */
    public DishVO getByIdWothFlavors(Long id) {
        Dish dish = dishMapper.getById(id);
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 修改菜品及其口味信息
     * @param dishDTO
     */
    @Transactional
    public void updateWithFlavors(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        //修改菜品信息
        dishMapper.update(dish);

        List<DishFlavor> flavors = dishDTO.getFlavors();
        //删除原有的口味信息
        if(flavors != null && flavors.size() > 0){
            dishFlavorMapper.deleteByDishIds(Lists.newArrayList(dish.getId()));
        }
        //新增现在的口味信息
        dishFlavorMapper.insertBatch(flavors);
    }

    /**
     * 启售、停售菜品
     * @param status
     * @param id
     */
    @Transactional
    public void setStatus(Integer status, Long id) {
        //设置菜品
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);

        //如果是停售，对相关套餐也要停售
        if (status == StatusConstant.DISABLE){
            ArrayList<Long> dishIds = Lists.newArrayList(id);
            List<Long> setmealIds = setmealDishMapper.getSetmealIdByDishIds(dishIds);

            if(setmealIds != null && setmealIds.size() > 0){
                for (Long setmealId : setmealIds) {
                    Setmeal setmeal = Setmeal.builder()
                            .id(setmealId)
                            .status(status)
                            .build();
                    setmealMapper.update(setmeal);
                }
            }
        }
    }
}























