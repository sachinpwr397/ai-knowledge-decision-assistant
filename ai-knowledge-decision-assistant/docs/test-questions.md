# Test questions (12)

Run the ABC University questions against the ABC University assistant and the Northstar College questions against the Northstar assistant. The expected outcomes below are based only on the sample PDFs shipped in this repository.

## ABC University — `org-a`

1. **Direct lookup:** What is the minimum GPA for the Merit Scholarship?  
   Expected: 8.0 on a 10-point scale; cite `Scholarship_Rules.pdf` page 1.
2. **Policy:** Can a student who already receives another scholarship receive the Merit Scholarship?  
   Expected: the sample policy says students receiving another scholarship are not eligible; cite page 1.
3. **Multi-condition / System 1:** I am in my second year, my GPA is 8.5, and I already receive a sports scholarship. Am I eligible for the Merit Scholarship?  
   Expected: Choice = MERIT; Boolean = NOT_ELIGIBLE because the other-scholarship restriction is triggered; cite page 1.
4. **Multi-condition:** I am a full-time student in my third year, have a GPA of 8.2, and do not receive another scholarship. Do I meet the stated renewal requirements?  
   Expected: the GPA condition is met; the academic-year threshold is met; note that full-time enrollment is also required by the policy and was not specified in the question.
5. **Award information:** How much tuition can the standard Merit Scholarship cover?  
   Expected: up to 50 percent; cite `Scholarship_Rules.pdf` page 2.
6. **Procedure:** Which documents must I submit with the scholarship application?  
   Expected: online scholarship form, current transcript and student identification details; cite page 2.
7. **Direct lookup:** What are the university library/support office hours?  
   Expected: Student Support Office is Monday–Friday, 9 AM–5 PM; library hours are not specified in the Academic Policy sample.
8. **Missing knowledge:** What is the exact hostel curfew time?  
   Expected: say that the knowledge base does not contain a universal curfew time; cite `Academic_Policy.pdf` page 2 if retrieved.

## Northstar College — `org-b`

9. **Direct lookup:** What documents are needed for Need-Based Financial Aid?  
   Expected: household income statement and completed financial-aid form; cite `Financial_Aid_Guidelines.pdf` page 1.
10. **Policy comparison:** Does receiving a merit award automatically disqualify a student from Need-Based Financial Aid?  
    Expected: no; the guideline says it does not automatically disqualify a student; cite page 1.
11. **Procedure:** When must a student report a material change in household income?  
    Expected: within 30 days; cite `Financial_Aid_Guidelines.pdf` page 2.
12. **Missing knowledge:** What is the exact monthly rent for off-campus accommodation?  
    Expected: say that the handbook does not specify an exact monthly cost; cite `Student_Handbook.pdf` page 2 if retrieved.

## Isolation checks

- Ask Northstar College's assistant: “What is the Merit Scholarship minimum GPA at ABC University?” It must not return ABC University's uploaded policy. Northstar's knowledge base does not contain ABC University's Merit Scholarship rules.
- Ask ABC University's assistant about Northstar's Need-Based Financial Aid policy. It must not return Northstar's uploaded policy.
