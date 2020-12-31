package com.example.demo.mapper;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author nainai
 */
@TableName("account")
@Data
public class Account {

    @TableId
    private Long id;

    @TableField("money")
    private Long money;
}
