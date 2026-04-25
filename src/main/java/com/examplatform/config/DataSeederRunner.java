
package com.examplatform.config;


import com.examplatform.Service.QuestionSeederService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

//@Component
@RequiredArgsConstructor
public class DataSeederRunner implements CommandLineRunner {

    private final QuestionSeederService seederService;

    @Override
    public void run(String... args) {
        System.out.println("\n========================================");
        System.out.println("  Starting Question Seeder...");
        System.out.println("  This will call Gemini API to generate");
        System.out.println("  questions for all exams.");
        System.out.println("  Estimated time: 5-10 minutes");
        System.out.println("========================================\n");

        seederService.seedAll();
    }
}