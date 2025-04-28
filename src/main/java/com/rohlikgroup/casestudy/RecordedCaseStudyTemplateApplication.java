package com.rohlikgroup.casestudy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RecordedCaseStudyTemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecordedCaseStudyTemplateApplication.class, args);
    }

}
