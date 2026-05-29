package com.bookbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long bookId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    private String edition;

    @Column(name = "published_year")
    private Integer publishedYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_condition", nullable = false)
    private BookCondition condition;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "asking_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal askingPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookStatus status = BookStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "book_image_urls", joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "image_url")
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @Column(name = "views_count", nullable = false)
    @Builder.Default
    private int viewsCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
