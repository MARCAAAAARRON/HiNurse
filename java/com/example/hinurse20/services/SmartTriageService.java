package com.example.hinurse20.services;

import com.example.hinurse20.models.SymptomAnalysis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SmartTriageService {
    
    // Symptom database with severity and urgency mappings
    private static final Map<String, SymptomData> SYMPTOM_DATABASE = new HashMap<>();
    private static final Map<String, String> ALIASES = new HashMap<>();
    
    static {
        // Initialize symptom database
        SYMPTOM_DATABASE.put("fever", new SymptomData("moderate", "within_24h", "schedule_appointment"));
        SYMPTOM_DATABASE.put("high fever", new SymptomData("high", "immediate", "urgent_care"));
        SYMPTOM_DATABASE.put("severe headache", new SymptomData("high", "immediate", "urgent_care"));
        SYMPTOM_DATABASE.put("chest pain", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("difficulty breathing", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("severe abdominal pain", new SymptomData("high", "immediate", "urgent_care"));
        SYMPTOM_DATABASE.put("nausea", new SymptomData("low", "within_week", "self_care"));
        SYMPTOM_DATABASE.put("vomiting", new SymptomData("moderate", "within_24h", "schedule_appointment"));
        SYMPTOM_DATABASE.put("diarrhea", new SymptomData("low", "within_week", "self_care"));
        SYMPTOM_DATABASE.put("cough", new SymptomData("low", "within_week", "self_care"));
        SYMPTOM_DATABASE.put("sore throat", new SymptomData("low", "within_week", "self_care"));
        SYMPTOM_DATABASE.put("runny nose", new SymptomData("low", "routine", "self_care"));
        SYMPTOM_DATABASE.put("fatigue", new SymptomData("low", "routine", "self_care"));
        SYMPTOM_DATABASE.put("muscle aches", new SymptomData("low", "routine", "self_care"));
        SYMPTOM_DATABASE.put("rash", new SymptomData("moderate", "within_24h", "schedule_appointment"));
        SYMPTOM_DATABASE.put("dizziness", new SymptomData("moderate", "within_24h", "schedule_appointment"));
        SYMPTOM_DATABASE.put("confusion", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("seizure", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("unconsciousness", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("severe bleeding", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("allergic reaction", new SymptomData("urgent", "immediate", "emergency"));

        SYMPTOM_DATABASE.put("headache", new SymptomData("moderate", "within_week", "schedule_appointment"));
        SYMPTOM_DATABASE.put("chest tightness", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("shortness of breath", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("breathlessness", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("wheezing", new SymptomData("high", "within_24h", "urgent_care"));
        SYMPTOM_DATABASE.put("asthma attack", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("palpitations", new SymptomData("moderate", "within_24h", "schedule_appointment"));
        SYMPTOM_DATABASE.put("fainting", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("syncope", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("abdominal pain", new SymptomData("moderate", "within_24h", "schedule_appointment"));
        SYMPTOM_DATABASE.put("back pain", new SymptomData("low", "within_week", "self_care"));
        SYMPTOM_DATABASE.put("dysuria", new SymptomData("moderate", "within_24h", "schedule_appointment"));
        SYMPTOM_DATABASE.put("urinary burning", new SymptomData("moderate", "within_24h", "schedule_appointment"));
        SYMPTOM_DATABASE.put("blood in urine", new SymptomData("high", "within_24h", "urgent_care"));
        SYMPTOM_DATABASE.put("bleeding", new SymptomData("high", "within_24h", "urgent_care"));
        SYMPTOM_DATABASE.put("vomiting blood", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("black stools", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("dehydration", new SymptomData("high", "within_24h", "urgent_care"));
        SYMPTOM_DATABASE.put("severe dehydration", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("facial droop", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("slurred speech", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("weakness one side", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("numbness", new SymptomData("moderate", "within_24h", "schedule_appointment"));
        SYMPTOM_DATABASE.put("tingling", new SymptomData("moderate", "within_24h", "schedule_appointment"));
        SYMPTOM_DATABASE.put("eye pain", new SymptomData("high", "within_24h", "urgent_care"));
        SYMPTOM_DATABASE.put("vision loss", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("ear pain", new SymptomData("low", "within_week", "self_care"));
        SYMPTOM_DATABASE.put("toothache", new SymptomData("low", "within_week", "self_care"));
        SYMPTOM_DATABASE.put("skin infection", new SymptomData("moderate", "within_24h", "schedule_appointment"));
        SYMPTOM_DATABASE.put("open wound", new SymptomData("moderate", "within_24h", "schedule_appointment"));
        SYMPTOM_DATABASE.put("animal bite", new SymptomData("high", "within_24h", "urgent_care"));
        SYMPTOM_DATABASE.put("minor burn", new SymptomData("low", "within_week", "self_care"));
        SYMPTOM_DATABASE.put("severe burn", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("hives", new SymptomData("moderate", "within_24h", "schedule_appointment"));
        SYMPTOM_DATABASE.put("anaphylaxis", new SymptomData("urgent", "immediate", "emergency"));
        SYMPTOM_DATABASE.put("loss of smell", new SymptomData("low", "within_week", "self_care"));
        SYMPTOM_DATABASE.put("poisoning", new SymptomData("urgent", "immediate", "emergency"));

        ALIASES.put("sob", "shortness of breath");
        ALIASES.put("short of breath", "shortness of breath");
        ALIASES.put("breathless", "breathlessness");
        ALIASES.put("dyspnea", "shortness of breath");
        ALIASES.put("tight chest", "chest tightness");
        ALIASES.put("stomach pain", "abdominal pain");
        ALIASES.put("tummy pain", "abdominal pain");
        ALIASES.put("urine burning", "urinary burning");
        ALIASES.put("pee burning", "urinary burning");
        ALIASES.put("passed out", "fainting");
        ALIASES.put("black stool", "black stools");
        ALIASES.put("bloody vomit", "vomiting blood");
        ALIASES.put("hives rash", "hives");
    }
    
    // Self-care tips database
    private static final Map<String, String> SELF_CARE_TIPS = new HashMap<>();
    
    static {
        SELF_CARE_TIPS.put("fever", "Rest, stay hydrated, use fever reducers like acetaminophen. Monitor temperature every 4 hours.");
        SELF_CARE_TIPS.put("nausea", "Eat bland foods, avoid strong smells, stay hydrated with small sips of water.");
        SELF_CARE_TIPS.put("cough", "Stay hydrated, use throat lozenges, try honey and warm tea.");
        SELF_CARE_TIPS.put("sore throat", "Gargle with warm salt water, use throat lozenges, stay hydrated.");
        SELF_CARE_TIPS.put("fatigue", "Get adequate sleep, maintain regular sleep schedule, eat balanced meals.");
        SELF_CARE_TIPS.put("muscle aches", "Apply heat or cold packs, gentle stretching, over-the-counter pain relievers.");
        SELF_CARE_TIPS.put("diarrhea", "Stay hydrated with electrolyte solutions, eat bland foods, avoid dairy.");
        SELF_CARE_TIPS.put("headache", "Rest in a dark room, hydrate, consider acetaminophen if not contraindicated.");
        SELF_CARE_TIPS.put("back pain", "Gentle stretching, heat application, avoid heavy lifting, short walks.");
        SELF_CARE_TIPS.put("runny nose", "Saline nasal rinse, fluids, rest, consider antihistamines if allergic.");
        SELF_CARE_TIPS.put("minor burn", "Cool running water 10–20 minutes, do not apply ice, keep area clean.");
        SELF_CARE_TIPS.put("ear pain", "Warm compress, analgesics as needed, monitor for fever or discharge.");
        SELF_CARE_TIPS.put("toothache", "Rinse with warm salt water, analgesics, avoid very hot/cold foods.");
        SELF_CARE_TIPS.put("loss of smell", "Rest, hydrate, consider home isolation if viral symptoms are present.");
    }
    
    public static SymptomAnalysis analyzeSymptoms(List<String> symptoms) {
        if (symptoms == null || symptoms.isEmpty()) {
            return createDefaultAnalysis();
        }
        
        // Analyze each symptom and determine overall assessment
        String overallSeverity = "low";
        String overallUrgency = "routine";
        String overallRecommendation = "self_care";
        List<String> selfCareTips = new ArrayList<>();
        boolean requiresAppointment = false;
        
        for (String symptom : symptoms) {
            String normalizedSymptom = normalizeSymptom(symptom);
            SymptomData data = SYMPTOM_DATABASE.get(normalizedSymptom);
            
            if (data != null) {
                // Update overall assessment based on most severe symptom
                if (isMoreSevere(data.severity, overallSeverity)) {
                    overallSeverity = data.severity;
                    overallUrgency = data.urgency;
                    overallRecommendation = data.recommendation;
                }
                
                // Collect self-care tips
                String tip = SELF_CARE_TIPS.get(normalizedSymptom);
                if (tip != null && !selfCareTips.contains(tip)) {
                    selfCareTips.add(tip);
                }
                
                // Check if appointment is required
                if (!data.recommendation.equals("self_care")) {
                    requiresAppointment = true;
                }
            }
        }
        
        // Create analysis result
        SymptomAnalysis analysis = new SymptomAnalysis();
        analysis.setSymptoms(symptoms);
        analysis.setSeverity(overallSeverity);
        analysis.setUrgency(overallUrgency);
        analysis.setRecommendation(overallRecommendation);
        analysis.setRequiresAppointment(requiresAppointment);
        analysis.setPriorityLevel(determinePriorityLevel(overallSeverity, overallUrgency));
        analysis.setSuggestedTimeframe(getSuggestedTimeframe(overallUrgency));
        analysis.setSelfCareTips(String.join("\n• ", selfCareTips));
        
        return analysis;
    }

    private static String normalizeSymptom(String s) {
        if (s == null) return "";
        String key = s.toLowerCase().trim();
        String mapped = ALIASES.get(key);
        return mapped != null ? mapped : key;
    }
    
    private static boolean isMoreSevere(String newSeverity, String currentSeverity) {
        Map<String, Integer> severityLevels = new HashMap<>();
        severityLevels.put("low", 1);
        severityLevels.put("moderate", 2);
        severityLevels.put("high", 3);
        severityLevels.put("urgent", 4);
        
        return severityLevels.getOrDefault(newSeverity, 0) > 
               severityLevels.getOrDefault(currentSeverity, 0);
    }
    
    private static String determinePriorityLevel(String severity, String urgency) {
        if (severity.equals("urgent") || urgency.equals("immediate")) {
            return "1";
        } else if (severity.equals("high") || urgency.equals("within_24h")) {
            return "2";
        } else if (severity.equals("moderate")) {
            return "3";
        } else {
            return "4";
        }
    }
    
    private static String getSuggestedTimeframe(String urgency) {
        switch (urgency) {
            case "immediate": return "Seek care immediately";
            case "within_24h": return "Schedule within 24 hours";
            case "within_week": return "Schedule within a week";
            case "routine": return "Schedule at your convenience";
            default: return "Schedule as needed";
        }
    }
    
    private static SymptomAnalysis createDefaultAnalysis() {
        SymptomAnalysis analysis = new SymptomAnalysis();
        analysis.setSeverity("low");
        analysis.setUrgency("routine");
        analysis.setRecommendation("self_care");
        analysis.setRequiresAppointment(false);
        analysis.setPriorityLevel("5");
        analysis.setSuggestedTimeframe("Schedule as needed");
        analysis.setSelfCareTips("Please describe your symptoms for a more accurate assessment.");
        return analysis;
    }
    
    // Helper class for symptom data
    private static class SymptomData {
        String severity;
        String urgency;
        String recommendation;
        
        SymptomData(String severity, String urgency, String recommendation) {
            this.severity = severity;
            this.urgency = urgency;
            this.recommendation = recommendation;
        }
    }
    
    // Get available symptoms for autocomplete
    public static List<String> getAvailableSymptoms() {
        return new ArrayList<>(SYMPTOM_DATABASE.keySet());
    }
    
    // Get self-care tips for specific symptoms
    public static String getSelfCareTipsForSymptoms(List<String> symptoms) {
        List<String> tips = new ArrayList<>();
        for (String symptom : symptoms) {
            String tip = SELF_CARE_TIPS.get(symptom.toLowerCase().trim());
            if (tip != null && !tips.contains(tip)) {
                tips.add(tip);
            }
        }
        return String.join("\n• ", tips);
    }
}





















