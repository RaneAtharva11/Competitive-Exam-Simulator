
package com.examplatform.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

public class ExamSeedConfig {

    // ── Complete seed blueprint for all exams ────────────────────────────────
    public static final List<ExamBlueprint> EXAMS = List.of(

            new ExamBlueprint("JEE", 75, 10800, 4, -1, 0, List.of(
                    new PaperBlueprint(1, List.of(
                            new SubjectBlueprint("PHYSICS", List.of(
                                    "Kinematics", "Laws of Motion", "Work Energy Power",
                                    "Rotational Motion", "Gravitation"
                            ), 25),
                            new SubjectBlueprint("CHEMISTRY", List.of(
                                    "Atomic Structure", "Chemical Bonding", "Organic Chemistry Basics",
                                    "Equilibrium", "Electrochemistry"
                            ), 25)
                    )),
                    new PaperBlueprint(2, List.of(
                            new SubjectBlueprint("MATHEMATICS", List.of(
                                    "Limits and Derivatives", "Integration", "Matrices",
                                    "Probability", "Coordinate Geometry"
                            ), 25)
                    ))
            )),

            new ExamBlueprint("MHT-CET", 150, 5400, 2, 0, 0, List.of(
                    new PaperBlueprint(1, List.of(
                            new SubjectBlueprint("PHYSICS", List.of(
                                    "Circular Motion", "Gravitation", "Current Electricity",
                                    "Magnetism", "Semiconductors"
                            ), 50),
                            new SubjectBlueprint("CHEMISTRY", List.of(
                                    "Solid State", "Solutions", "Chemical Kinetics",
                                    "Coordination Compounds", "Polymers"
                            ), 50)
                    )),
                    new PaperBlueprint(2, List.of(
                            new SubjectBlueprint("MATHEMATICS", List.of(
                                    "Trigonometry", "Vectors", "Three Dimensional Geometry",
                                    "Linear Programming", "Statistics"
                            ), 50)
                    ))
            )),

            new ExamBlueprint("BITSAT", 130, 10800, 3, -1, 0, List.of(
                    new PaperBlueprint(1, List.of(
                            new SubjectBlueprint("PHYSICS", List.of(
                                    "Units and Dimensions", "Optics", "Thermodynamics",
                                    "Waves", "Modern Physics"
                            ), 40),
                            new SubjectBlueprint("CHEMISTRY", List.of(
                                    "Periodic Table", "Acids and Bases", "Organic Reactions",
                                    "Surface Chemistry", "Environmental Chemistry"
                            ), 40),
                            new SubjectBlueprint("MATHEMATICS", List.of(
                                    "Complex Numbers", "Permutation Combination",
                                    "Binomial Theorem", "Sequence Series", "Differential Equations"
                            ), 45),
                            new SubjectBlueprint("ENGLISH", List.of(
                                    "Reading Comprehension", "Grammar", "Vocabulary"
                            ), 5)
                    ))
            )),

            new ExamBlueprint("VIT", 125, 9000, 1, 0, 0, List.of(
                    new PaperBlueprint(1, List.of(
                            new SubjectBlueprint("PHYSICS", List.of(
                                    "Electrostatics", "Capacitors", "Current Electricity",
                                    "Magnetic Effects", "Alternating Current"
                            ), 35),
                            new SubjectBlueprint("CHEMISTRY", List.of(
                                    "Biomolecules", "Aldehydes Ketones", "Amines",
                                    "d-block Elements", "Haloalkanes"
                            ), 35),
                            new SubjectBlueprint("MATHEMATICS", List.of(
                                    "Sets Relations Functions", "Inverse Trigonometry",
                                    "Continuity Differentiability", "Applications of Integrals",
                                    "Determinants"
                            ), 40),
                            new SubjectBlueprint("ENGLISH", List.of(
                                    "Synonyms Antonyms", "Fill in the Blanks"
                            ), 15)
                    ))
            ))
    );

    // ── Blueprint inner classes ───────────────────────────────────────────────

    @Data @AllArgsConstructor
    public static class ExamBlueprint {
        private String name;
        private int totalQuestions;
        private int durationSeconds;
        private int marksCorrect;
        private int marksWrong;
        private int marksUnattempted;
        private List<PaperBlueprint> papers;
    }

    @Data @AllArgsConstructor
    public static class PaperBlueprint {
        private int paperNumber;
        private List<SubjectBlueprint> subjects;
    }

    @Data @AllArgsConstructor
    public static class SubjectBlueprint {
        private String subjectName;
        private List<String> chapters;
        private int totalQuestions;  // total for this subject across all chapters
    }
}