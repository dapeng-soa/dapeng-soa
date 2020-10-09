package com.github.dapeng.api.dto;


import lombok.Data;

import java.util.Map;

@Data
public class StoreRequest {

    private int numberId;

    private String name;

    private Map<String, String> attachment;

}
