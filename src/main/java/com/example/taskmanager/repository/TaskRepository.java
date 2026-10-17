package com.example.taskmanager.repository;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.projection.PriorityCount;
import com.example.taskmanager.repository.projection.StatusCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @Query("select t.status as status, count(t) as total from Task t group by t.status")
    List<StatusCount> countByStatus();

    @Query("select t.priority as priority, count(t) as total from Task t group by t.priority")
    List<PriorityCount> countByPriority();

    long countByDueDateBeforeAndStatusIn(LocalDate date, Collection<TaskStatus> statuses);
}
