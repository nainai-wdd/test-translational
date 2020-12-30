package com.example.demo.mapper;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author nainai
 */
@TableName("test")
@Data
public class Test {

    @TableId
    private Integer id;

    @TableField("money")
    private Long money;
}
