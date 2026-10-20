package com.example.taskmanager.repository.specification;

import com.example.taskmanager.dto.request.TaskSearchCriteria;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Stream;

public final class TaskSpecifications {

    private static final String ATTR_STATUS = "status";
    private static final String ATTR_PRIORITY = "priority";
    private static final String ATTR_ASSIGNED_TO = "assignedTo";
    private static final String ATTR_TITLE = "title";
    private static final String ATTR_DUE_DATE = "dueDate";

    private TaskSpecifications() {
        // Utility class
    }

    /**
     * Applies the requested sort inside the query. Enum attributes are ordered by their declared
     * position (LOW &lt; MEDIUM &lt; HIGH, PENDING &rarr; CANCELLED) instead of by the stored text,
     * which would sort them alphabetically.
     */
    public static Specification<Task> orderedBy(Sort sort) {
        return (root, query, cb) -> {
            query.orderBy(sort.stream().map(order -> toOrder(root, cb, order)).toList());
            return null;
        };
    }

    private static Order toOrder(Root<Task> root, CriteriaBuilder cb, Sort.Order order) {
        Expression<?> key = sortKey(root, cb, order.getProperty());
        return order.isAscending() ? cb.asc(key) : cb.desc(key);
    }

    private static Expression<?> sortKey(Root<Task> root, CriteriaBuilder cb, String property) {
        Path<Object> path = root.get(property);
        Object[] constants = path.getJavaType().getEnumConstants();
        if (constants == null) {
            return path;
        }
        CriteriaBuilder.SimpleCase<Object, Integer> rank = cb.selectCase(path);
        for (Object constant : constants) {
            rank = rank.when(constant, ((Enum<?>) constant).ordinal());
        }
        return rank.otherwise(constants.length);
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get(ATTR_STATUS), status);
    }

    public static Specification<Task> hasPriority(TaskPriority priority) {
        return priority == null
                ? null
                : (root, query, cb) -> cb.equal(root.get(ATTR_PRIORITY), priority);
    }

    public static Specification<Task> assignedToIgnoreCase(String assignedTo) {
        return assignedTo == null || assignedTo.isBlank()
                ? null
                : (root, query, cb) -> cb.equal(
                        cb.lower(root.get(ATTR_ASSIGNED_TO)), assignedTo.toLowerCase());
    }

    public static Specification<Task> titleContains(String text) {
        return text == null || text.isBlank()
                ? null
                : (root, query, cb) -> cb.like(
                        cb.lower(root.get(ATTR_TITLE)), "%" + text.toLowerCase() + "%");
    }

    public static Specification<Task> dueOnOrAfter(LocalDate dueFrom) {
        return dueFrom == null
                ? null
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(ATTR_DUE_DATE), dueFrom);
    }

    public static Specification<Task> dueOnOrBefore(LocalDate dueTo) {
        return dueTo == null
                ? null
                : (root, query, cb) -> cb.lessThanOrEqualTo(root.get(ATTR_DUE_DATE), dueTo);
    }

    public static Specification<Task> overdue(Boolean overdue, LocalDate today) {
        if (overdue == null || today == null) {
            return null;
        }
        if (Boolean.TRUE.equals(overdue)) {
            return (root, query, cb) -> cb.and(
                    cb.isNotNull(root.get(ATTR_DUE_DATE)),
                    cb.lessThan(root.get(ATTR_DUE_DATE), today),
                    root.get(ATTR_STATUS).in(TaskStatus.activeStatuses())
            );
        } else {
            return (root, query, cb) -> cb.or(
                    cb.isNull(root.get(ATTR_DUE_DATE)),
                    cb.greaterThanOrEqualTo(root.get(ATTR_DUE_DATE), today),
                    root.get(ATTR_STATUS).in(TaskStatus.DONE, TaskStatus.CANCELLED)
            );
        }
    }

    public static Specification<Task> from(TaskSearchCriteria c, LocalDate today) {
        if (c == null) {
            return Specification.where(null);
        }
        return Specification.allOf(Stream.of(
                        hasStatus(c.status()),
                        hasPriority(c.priority()),
                        assignedToIgnoreCase(c.assignedTo()),
                        titleContains(c.text()),
                        dueOnOrAfter(c.dueFrom()),
                        dueOnOrBefore(c.dueTo()),
                        overdue(c.overdue(), today))
                .filter(Objects::nonNull)
                .toList());
    }
}
