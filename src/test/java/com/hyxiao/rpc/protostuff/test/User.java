package com.hyxiao.rpc.protostuff.test;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class User {
	
    private String id;

    private String name;

    private Integer age;

    private String desc;
    
}