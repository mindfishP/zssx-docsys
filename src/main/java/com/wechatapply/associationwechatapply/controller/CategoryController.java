package com.wechatapply.associationwechatapply.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wechatapply.associationwechatapply.common.result.R;
import com.wechatapply.associationwechatapply.entity.BizCategory;
import com.wechatapply.associationwechatapply.framework.annotation.RequireRole;
import com.wechatapply.associationwechatapply.framework.context.RoleCode;
import com.wechatapply.associationwechatapply.mapper.BizCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 档案分类与元数据字典接口（SRS FR-03）。
 */
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final BizCategoryMapper categoryMapper;

    /** 全部启用分类（填报表单与检索条件下拉使用），六类字母序 */
    @GetMapping("/list")
    public R<List<BizCategory>> list() {
        return R.ok(categoryMapper.selectList(Wrappers.<BizCategory>lambdaQuery()
                .eq(BizCategory::getStatus, 1)
                .orderByAsc(BizCategory::getMajorCode)
                .orderByAsc(BizCategory::getSubCode)));
    }

    /** 新增/修改分类（仅主管部门操作员，元数据字典驱动表单） */
    @PostMapping
    @RequireRole(RoleCode.ADMIN)
    public R<Void> save(@RequestBody BizCategory category) {
        if (categoryMapper.selectById(category.getCategoryCode()) == null) {
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }
        return R.ok();
    }
}
