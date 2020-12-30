package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author nainai
 */
@Mapper
public interface TestMapper extends BaseMapper<Test> {

    int addMoney(@Param("money") int money, @Param("id") int id);
}
