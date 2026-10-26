package com.mallmini.category.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryVO {

    private Long id;

    private String name;

    private String iconUrl;

    private Integer sortOrder;

    private List<CategoryVO> children = new ArrayList<>();
}
