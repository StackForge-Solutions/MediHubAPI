package com.MediHubAPI.repository.scheduling.template;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.MediHubAPI.model.scheduling.template.ScheduleTemplate;

public interface ScheduleTemplateRepository extends JpaRepository<ScheduleTemplate, Long>,
        JpaSpecificationExecutor<ScheduleTemplate> {}
