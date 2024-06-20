package com.liu.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/parser")
public class CSDNParserController {

    @GetMapping("/url")
    public String parser(String url) {
        return "parser";
    }
}
