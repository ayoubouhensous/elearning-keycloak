package org.example.apisecurise.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final List<Map<String,String>> courses = new ArrayList<>(List.of(
            Map.of("id","1","title","Spring Boot Basics"),
            Map.of("id","2","title","React for Beginners")
    ));

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public List<Map<String,String>> getCourses() {
        return courses;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String,String> addCourse(@RequestBody Map<String,String> course) {
        courses.add(course);
        return course;
    }
}
