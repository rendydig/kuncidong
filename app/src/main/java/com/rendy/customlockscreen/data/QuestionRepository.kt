package com.rendy.customlockscreen.data

import kotlin.random.Random

class QuestionRepository {
    
    private val questions = listOf(
        // Mathematics Questions
        Question(1, QuestionCategory.MATHEMATICS, "What is 15 + 27?", 
            listOf("42", "41", "43", "40"), 0),
        Question(2, QuestionCategory.MATHEMATICS, "What is 8 × 7?", 
            listOf("54", "56", "58", "52"), 1),
        Question(3, QuestionCategory.MATHEMATICS, "What is 144 ÷ 12?", 
            listOf("11", "12", "13", "14"), 1),
        Question(4, QuestionCategory.MATHEMATICS, "What is 25% of 80?", 
            listOf("15", "20", "25", "30"), 1),
        Question(5, QuestionCategory.MATHEMATICS, "What is 2³ (2 to the power of 3)?", 
            listOf("6", "8", "9", "12"), 1),
        
        // Science Questions
        Question(6, QuestionCategory.SCIENCE, "What is the chemical symbol for water?", 
            listOf("H2O", "CO2", "O2", "H2"), 0),
        Question(7, QuestionCategory.SCIENCE, "How many planets are in our solar system?", 
            listOf("7", "8", "9", "10"), 1),
        Question(8, QuestionCategory.SCIENCE, "What gas do plants absorb from the atmosphere?", 
            listOf("Oxygen", "Carbon Dioxide", "Nitrogen", "Hydrogen"), 1),
        Question(9, QuestionCategory.SCIENCE, "What is the hardest natural substance?", 
            listOf("Gold", "Iron", "Diamond", "Silver"), 2),
        Question(10, QuestionCategory.SCIENCE, "At what temperature does water boil?", 
            listOf("90°C", "100°C", "110°C", "120°C"), 1),
        
        // Geography Questions
        Question(11, QuestionCategory.GEOGRAPHY, "What is the capital of France?", 
            listOf("London", "Berlin", "Paris", "Madrid"), 2),
        Question(12, QuestionCategory.GEOGRAPHY, "Which is the largest ocean?", 
            listOf("Atlantic", "Pacific", "Indian", "Arctic"), 1),
        Question(13, QuestionCategory.GEOGRAPHY, "How many continents are there?", 
            listOf("5", "6", "7", "8"), 2),
        Question(14, QuestionCategory.GEOGRAPHY, "What is the longest river in the world?", 
            listOf("Amazon", "Nile", "Mississippi", "Yangtze"), 1),
        Question(15, QuestionCategory.GEOGRAPHY, "Which country has the most time zones?", 
            listOf("USA", "Russia", "China", "Canada"), 1),
        
        // History Questions
        Question(16, QuestionCategory.HISTORY, "In which year did World War II end?", 
            listOf("1944", "1945", "1946", "1947"), 1),
        Question(17, QuestionCategory.HISTORY, "Who was the first person to walk on the moon?", 
            listOf("Buzz Aldrin", "Neil Armstrong", "John Glenn", "Alan Shepard"), 1),
        Question(18, QuestionCategory.HISTORY, "Which ancient wonder was located in Egypt?", 
            listOf("Hanging Gardens", "Colossus of Rhodes", "Great Pyramid", "Lighthouse"), 2),
        Question(19, QuestionCategory.HISTORY, "Who painted the Mona Lisa?", 
            listOf("Michelangelo", "Leonardo da Vinci", "Raphael", "Donatello"), 1),
        Question(20, QuestionCategory.HISTORY, "In which year did the Berlin Wall fall?", 
            listOf("1987", "1988", "1989", "1990"), 2),
        
        // Language Questions
        Question(21, QuestionCategory.LANGUAGE, "What is the plural of 'child'?", 
            listOf("childs", "children", "childes", "child"), 1),
        Question(22, QuestionCategory.LANGUAGE, "Which word is a synonym for 'happy'?", 
            listOf("sad", "angry", "joyful", "tired"), 2),
        Question(23, QuestionCategory.LANGUAGE, "What is the past tense of 'go'?", 
            listOf("goed", "went", "gone", "going"), 1),
        Question(24, QuestionCategory.LANGUAGE, "How many letters are in the English alphabet?", 
            listOf("24", "25", "26", "27"), 2),
        Question(25, QuestionCategory.LANGUAGE, "What is an antonym for 'hot'?", 
            listOf("warm", "cool", "cold", "freezing"), 2),
        
        // General Knowledge Questions
        Question(26, QuestionCategory.GENERAL_KNOWLEDGE, "How many days are in a leap year?", 
            listOf("365", "366", "367", "364"), 1),
        Question(27, QuestionCategory.GENERAL_KNOWLEDGE, "What is the largest mammal?", 
            listOf("Elephant", "Blue Whale", "Giraffe", "Hippopotamus"), 1),
        Question(28, QuestionCategory.GENERAL_KNOWLEDGE, "How many sides does a hexagon have?", 
            listOf("5", "6", "7", "8"), 1),
        Question(29, QuestionCategory.GENERAL_KNOWLEDGE, "What color do you get when you mix red and blue?", 
            listOf("Green", "Purple", "Orange", "Yellow"), 1),
        Question(30, QuestionCategory.GENERAL_KNOWLEDGE, "How many minutes are in an hour?", 
            listOf("50", "60", "70", "80"), 1)
    )
    
    fun getRandomQuestion(): Question {
        return questions[Random.nextInt(questions.size)]
    }
    
    fun getQuestionsByCategory(category: QuestionCategory): List<Question> {
        return questions.filter { it.category == category }
    }
    
    fun getAllQuestions(): List<Question> = questions
}
