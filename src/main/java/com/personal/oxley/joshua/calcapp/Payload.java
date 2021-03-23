package com.personal.oxley.joshua.calcapp;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE) //Forces Jackson to use
public class Payload {
    private String text;
    private int id;
    private String type;
    private Long date;
    private String name;
}
