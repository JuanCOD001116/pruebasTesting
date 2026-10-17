package com.example.taskmanager.service.validation;

import com.example.taskmanager.exception.InvalidTaskDataException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SortValidator {

    private static final Set<String> ALLOWED_PROPERTIES = Set.of(
            "id", "title", "status", "priority", "dueDate", "createdAt", "updatedAt"
    );

    public void validate(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return;
        }

        for (Sort.Order order : sort) {
            if (!ALLOWED_PROPERTIES.contains(order.getProperty())) {
                throw new InvalidTaskDataException("Invalid sort property: " + order.getProperty());
            }
        }
    }
}
