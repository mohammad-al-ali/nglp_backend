package com.NGLP.backend.v1.dto;

/**
 * جسم طلب تعليم/إلغاء تعليم درس كمكتمل. {@code null} أو غياب الحقل يعني "مكتمل".
 */
public record LessonCompletionRequest(Boolean completed) {}
