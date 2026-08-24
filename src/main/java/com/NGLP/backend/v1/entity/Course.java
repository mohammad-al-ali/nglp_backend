package com.NGLP.backend.v1.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;
@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String imageUrl;
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private User teacher;

    // العلاقات العكسية أدناه لا تُصدَّر كقوائم كاملة (ثقيلة وقد تسبب دوراناً لانهائياً في التسلسل)،
    // بل تُستخدم فقط لحساب lessonsCount وstudentsCount أدناه ليعرضهما الواجهة الأمامية مباشرة.
    @OneToMany(mappedBy = "course")
    @JsonIgnore
    private List<Lesson> lessons;

    @OneToMany(mappedBy = "course")
    @JsonIgnore
    private List<Enrollment> enrollments;

    public int getLessonsCount() {
        return lessons != null ? lessons.size() : 0;
    }

    public int getStudentsCount() {
        return enrollments != null ? enrollments.size() : 0;
    }
}
