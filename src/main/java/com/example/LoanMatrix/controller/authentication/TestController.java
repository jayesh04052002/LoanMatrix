package com.example.LoanMatrix.controller.authentication;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/protected")
    public String protectedApi() {
        return "Success! You are authenticated. Protected API accessed.";
    }
}
