import json

def generate_addition_questions():
    questions = []
    question_id = 1
    
    # Addition questions (1+10 through 10+10)
    for first_num in range(1, 11):
        for second_num in range(10, 0, -1):
            if first_num + second_num >= 11 and first_num + second_num <= 20:
                result = first_num + second_num
                question = {
                    "id": question_id,
                    "category": "MATEMATIKA",
                    "question": f"Berapa hasil dari {first_num} + {second_num}?",
                    "options": [str(result), str(result-1), str(result+1), str(result-2)],
                    "correctAnswerIndex": 0
                }
                questions.append(question)
                question_id += 1
    
    return questions, question_id

def generate_subtraction_questions(start_id):
    questions = []
    question_id = start_id
    
    # Subtraction questions (20-10 through 11-1)
    for minuend in range(20, 10, -1):
        for subtrahend in range(10, 0, -1):
            if minuend - subtrahend >= 1 and minuend - subtrahend <= 10:
                result = minuend - subtrahend
                question = {
                    "id": question_id,
                    "category": "MATEMATIKA", 
                    "question": f"Berapa hasil dari {minuend} - {subtrahend}?",
                    "options": [str(result), str(result-1), str(result+1), str(result-2) if result > 2 else "0"],
                    "correctAnswerIndex": 0
                }
                questions.append(question)
                question_id += 1
    
    return questions

# Generate all questions
addition_questions, next_id = generate_addition_questions()
subtraction_questions = generate_subtraction_questions(next_id)

all_questions = addition_questions + subtraction_questions

# Create the final JSON structure
questions_data = {
    "questions": all_questions
}

# Write to JSON file
with open('questions.json', 'w', encoding='utf-8') as f:
    json.dump(questions_data, f, ensure_ascii=False, indent=2)

print(f"Generated {len(all_questions)} questions")
print(f"Addition questions: {len(addition_questions)}")
print(f"Subtraction questions: {len(subtraction_questions)}")
