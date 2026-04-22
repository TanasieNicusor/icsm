package com.example.icsm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * Maps the root URL to the home template.
     * The view name "home/index" resolves to src/main/resources/templates/home/index.html
     */
    @GetMapping("/")
    public String index() {
        return "home/index";
    }
}
