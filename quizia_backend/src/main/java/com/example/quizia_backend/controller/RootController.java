package com.example.quizia_backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class RootController {

    @GetMapping("/")
    public RedirectView root() {
        // Redirect root requests to the static auth.fxml resource
        return new RedirectView("/auth.fxml");
    }
}
