package com.example.varsivo.data

import com.example.varsivo.api.Bursary

/**
 * Starter bursaries written to Firestore the first time the app finds the collection empty.
 * Closing dates change every year, so they are left blank (shown as "TBC"); students must confirm
 * dates and criteria on the provider's own website.
 */
object BursarySeed {

    private const val CHECK = "Confirm the current closing date and criteria on the provider's website."

    val bursaries = listOf(
        Bursary("nsfas", "NSFAS", "Government funding for tuition, accommodation and allowances at public universities and TVET colleges. $CHECK",
            "National Student Financial Aid Scheme", "South African citizen; household income within the NSFAS threshold", "", "All fields", "National"),
        Bursary("funza-lushaka", "Funza Lushaka", "Bursary for students who want to become teachers in public schools. $CHECK",
            "Department of Basic Education", "Studying a teaching qualification in a priority subject", "", "Education", "National"),
        Bursary("sasol", "Sasol Bursary", "Covers studies in engineering and science fields linked to Sasol's work. $CHECK",
            "Sasol", "Strong Mathematics and Physical Sciences results", "", "Engineering", "National"),
        Bursary("eskom", "Eskom Bursary", "Funding for engineering and technical studies in the energy sector. $CHECK",
            "Eskom", "Studying engineering at a South African university", "", "Engineering", "National"),
        Bursary("transnet", "Transnet Bursary", "Supports engineering and technical students in transport and logistics. $CHECK",
            "Transnet", "Studying engineering or a related technical field", "", "Engineering", "National"),
        Bursary("deloitte", "Deloitte Bursary", "Supports accounting students on the path to becoming chartered accountants. $CHECK",
            "Deloitte", "Studying towards a CA(SA) accounting degree", "", "Commerce", "National"),
        Bursary("allan-gray", "Allan Gray Orbis Fellowship", "Fellowship for future entrepreneurs, with funding and mentorship. $CHECK",
            "Allan Gray Orbis Foundation", "Grade 12 learners with strong results and entrepreneurial drive", "", "Commerce", "National"),
        Bursary("health", "Health Sciences Bursary", "Example health-sciences bursary for nursing and medical students. $CHECK",
            "Provincial health departments", "Studying nursing, medicine or an allied health course", "", "Health", "National"),
        Bursary("gcra", "Gauteng City Region Academy", "Gauteng provincial bursary for residents studying in scarce-skills fields. $CHECK",
            "Gauteng Provincial Government", "Gauteng resident", "", "All fields", "Gauteng"),
        Bursary("kzn-premier", "KZN Premier's Bursary", "Provincial bursary for KwaZulu-Natal learners. $CHECK",
            "KwaZulu-Natal Provincial Government", "KwaZulu-Natal resident", "", "All fields", "KwaZulu-Natal"),
        Bursary("limpopo", "Limpopo Provincial Bursary", "Provincial bursary for Limpopo learners. $CHECK",
            "Limpopo Provincial Government", "Limpopo resident", "", "All fields", "Limpopo"),
        Bursary("western-cape", "Western Cape Bursary", "Provincial bursary for Western Cape learners. $CHECK",
            "Western Cape Government", "Western Cape resident", "", "Science", "Western Cape"),
        Bursary("legal-aid", "Law Bursary", "Example bursary for LLB students. $CHECK",
            "Law firms and legal bodies", "Studying towards an LLB degree", "", "Law", "National")
    )
}
