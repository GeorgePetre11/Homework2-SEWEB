package org.example.controller;

import org.example.model.GraphData;
import org.example.service.RdfService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
public class RdfController {

    private final RdfService rdfService;

    public RdfController(RdfService rdfService) {
        this.rdfService = rdfService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("pageContext", Map.of("page", "home"));
        return "index";
    }

    @GetMapping("/upload")
    public String uploadPage(Model model) {
        model.addAttribute("pageContext", Map.of("page", "upload"));
        return "upload";
    }

    @PostMapping("/upload")
    @ResponseBody
    public GraphData handleUpload(@RequestParam("file") MultipartFile file) throws Exception {
        return rdfService.parse(file.getInputStream());
    }
}
