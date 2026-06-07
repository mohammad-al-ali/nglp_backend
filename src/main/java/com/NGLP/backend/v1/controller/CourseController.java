package com.NGLP.backend.v1.controller;

import com.NGLP.backend.v1.entity.Course;
import com.NGLP.backend.v1.service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) { this.courseService = courseService; }

    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long teacherId) {

        if (teacherId != null) {
            return ResponseEntity.ok(courseService.findCoursesByTeacher(teacherId));
        }

        // إذا أرسلت الواجهة رقم القسم، نجلب كورسات هذا القسم فقط
        if (categoryId != null) {
            return ResponseEntity.ok(courseService.findCoursesByCategory(categoryId));
        }

        // إذا لم ترسل، نجلب كل الكورسات (مفيدة للأدمن)
        return ResponseEntity.ok(courseService.findAll());
    }
    @GetMapping("/{id}")
    public Course getById(@PathVariable Long id) { return courseService.findById(id); }

    @PostMapping
    public Course create(@RequestBody Course course) { return courseService.create(course); }

    @PutMapping("/{id}")
    public Course update(@PathVariable Long id, @RequestBody Course course) { return courseService.update(id, course); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
