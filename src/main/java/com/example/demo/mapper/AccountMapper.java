package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author nainai
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {

    int addMoney(@Param("money") Long money, @Param("id") Long id);
}
