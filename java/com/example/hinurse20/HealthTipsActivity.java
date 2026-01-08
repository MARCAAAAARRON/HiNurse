package com.example.hinurse20;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class HealthTipsActivity extends BaseActivity {
    private RecyclerView recyclerTips;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_tips);

        // Setup back button
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> finish());
        }

        recyclerTips = findViewById(R.id.recyclerTips);
        setupGrid();
    }

    private void setupGrid() {
        recyclerTips.setHasFixedSize(true);
        recyclerTips.setLayoutManager(new GridLayoutManager(this, 2));

        List<com.example.hinurse20.models.HealthTip> items = new ArrayList<>();
        items.add(new com.example.hinurse20.models.HealthTip(
                "Stay Hydrated",
                "Drink at least 8 glasses of water daily to maintain optimal body function.",
                R.drawable.img,
                "https://www.who.int/news-room/fact-sheets/detail/healthy-diet"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Get Enough Sleep",
                "Aim for 7-9 hours of quality sleep to support mental and physical health.",
                R.drawable.img1,
                "https://www.sleepfoundation.org/how-sleep-works"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Exercise Regularly",
                "Do at least 30 minutes of moderate activity daily for a healthy heart.",
                R.drawable.img2,
                "https://www.cdc.gov/physicalactivity/basics/index.htm"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Eat Balanced Meals",
                "Include fruits, vegetables, lean protein, and whole grains in your diet.",
                R.drawable.img3,
                "https://www.myplate.gov/"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Wash Hands Frequently",
                "Hand hygiene helps prevent the spread of germs and infections.",
                R.drawable.img4,
                "https://www.cdc.gov/handwashing/when-how-handwashing.html"
        ));

        // Additional tips
        items.add(new com.example.hinurse20.models.HealthTip(
                "Manage Stress",
                "Practice deep breathing or meditation for 5–10 minutes daily to reduce stress.",
                R.drawable.img5,
                "https://www.cdc.gov/mentalhealth/learn/index.htm"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Limit Added Sugar",
                "Reduce sugary drinks and snacks to help maintain a healthy weight and energy.",
                R.drawable.img6,
                "https://www.who.int/news-room/fact-sheets/detail/healthy-diet"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Quit Smoking",
                "Avoid tobacco in all forms to lower risk of heart disease, cancer, and stroke.",
                R.drawable.img7,
                "https://www.who.int/teams/health-promotion/tobacco-control"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Routine Checkups",
                "Schedule regular health screenings and dental visits for early detection.",
                R.drawable.img8,
                "https://www.cdc.gov/family/checkup/index.htm"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Protect Your Skin",
                "Use sunscreen (SPF 30+) and protective clothing to prevent UV damage.",
                R.drawable.img9,
                "https://www.cdc.gov/cancer/skin/basic_info/sun-safety.htm"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Stay Active During Day",
                "Break up sitting time—stand, stretch, or walk for a few minutes each hour.",
                R.drawable.img10,
                "https://www.who.int/news-room/fact-sheets/detail/physical-activity"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Eat More Fiber",
                "Include beans, whole grains, fruits, and vegetables to aid digestion and heart health.",
                R.drawable.img11,
                "https://www.hsph.harvard.edu/nutritionsource/carbohydrates/fiber/"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Stay Up-To-Date on Vaccines",
                "Follow recommended vaccinations to protect yourself and others.",
                R.drawable.img12,
                "https://www.cdc.gov/vaccines/schedules/index.html"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Healthy Portion Sizes",
                "Use smaller plates and check labels to avoid overeating.",
                R.drawable.img13,
                "https://www.nhs.uk/live-well/eat-well/food-portions/"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Stay Socially Connected",
                "Maintain supportive relationships to boost mental and emotional well‑being.",
                R.drawable.img14,
                "https://www.nih.gov/news-events/nih-research-matters/social-connection-boosts-mental-health"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Limit Alcohol",
                "If you drink, do so in moderation to reduce long-term health risks.",
                R.drawable.img15,
                "https://www.cdc.gov/alcohol/basics/alcohol-use.htm"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Practice Good Posture",
                "Keep your back straight and shoulders relaxed to prevent neck and back pain.",
                R.drawable.img16,
                "https://www.nhs.uk/live-well/exercise/why-sitting-too-much-is-bad-for-us/"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Cook at Home More",
                "Preparing meals at home helps control ingredients, salt, and portion sizes.",
                R.drawable.img17,
                "https://www.hsph.harvard.edu/nutritionsource/healthy-eating-plate/"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Add Strength Training",
                "Include muscle‑strengthening activities at least 2 days per week.",
                R.drawable.img18,
                "https://www.cdc.gov/physicalactivity/basics/older_adults/index.htm"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Eat Healthy Fats",
                "Choose nuts, seeds, avocado, and olive oil; limit trans and saturated fats.",
                R.drawable.img19,
                "https://www.hsph.harvard.edu/nutritionsource/what-should-you-eat/fats-and-cholesterol/"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Stay Safe with Food",
                "Wash produce, cook meats thoroughly, and avoid cross‑contamination.",
                R.drawable.img20,
                "https://www.cdc.gov/foodsafety/keep-food-safe.html"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Limit Salt Intake",
                "Taste food before salting and choose low‑sodium options to support heart health.",
                R.drawable.img21,
                "https://www.who.int/news-room/fact-sheets/detail/salt-reduction"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Get Enough Protein",
                "Include lean proteins like fish, poultry, beans, and legumes to support muscles.",
                R.drawable.img22,
                "https://www.nhlbi.nih.gov/health/educational/wecan/eat-right/portion-distortion.htm"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Prioritize Sleep Routine",
                "Keep a regular bedtime and limit screens before sleep for better rest.",
                R.drawable.img23,
                "https://www.sleepfoundation.org/healthy-sleep-tips"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Hydrate During Exercise",
                "Drink water before, during, and after workouts to replace fluids.",
                R.drawable.img24,
                "https://www.acefitness.org/resources/everyone/blog/7636/how-to-stay-hydrated-during-exercise/"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Mindful Eating",
                "Eat slowly, notice hunger/fullness cues, and avoid distractions at meals.",
                R.drawable.img25,
                "https://www.cdc.gov/healthyweight/losing_weight/eating_habits.html"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Keep a Water Bottle",
                "Carry a reusable bottle to make sipping water easy throughout the day.",
                R.drawable.img26,
                "https://www.who.int/news-room/fact-sheets/detail/healthy-diet"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Choose Whole Grains",
                "Swap white rice/bread for brown rice, oats, or whole‑wheat options.",
                R.drawable.img27,
                "https://www.hsph.harvard.edu/nutritionsource/whole-grains/"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Add Colorful Veggies",
                "Aim for a variety of colors to get a range of vitamins and antioxidants.",
                R.drawable.img28,
                "https://www.myplate.gov/eat-healthy/vegetables"
        ));
        items.add(new com.example.hinurse20.models.HealthTip(
                "Healthy Snacks",
                "Choose fruit, yogurt, nuts, or veggies with hummus instead of chips or candy.",
                R.drawable.img29,
                "https://www.cdc.gov/healthyweight/healthy_eating/snacks.html"
        ));

        com.example.hinurse20.adapters.HealthTipsAdapter adapter =
                new com.example.hinurse20.adapters.HealthTipsAdapter(this, items);
        recyclerTips.setAdapter(adapter);
    }
}
