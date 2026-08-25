package com.example.ui.fasting

import androidx.compose.ui.graphics.Color

data class StageBiomarkers(
    val bloodSugar: String,
    val insulin: String,
    val ketones: String,
    val autophagy: String,
    val growthHormone: String
)

data class MetabolicState(
    val id: String,
    val title: String,
    val scientificTitle: String,
    val hourStart: Float,
    val hourEnd: Float?, // null if open-ended (48h+)
    val hoursRangeLabel: String,
    val icon: String,
    val accentColor: Color,
    val shortSummary: String,
    val physiologicalProcess: String,
    val primaryFuelSource: String,
    val biomarkers: StageBiomarkers,
    val keyBenefits: List<String>,
    val tips: String
)

object FastingMetabolicStages {
    val ALL_STAGES = listOf(
        MetabolicState(
            id = "fed_state",
            title = "Blood Sugar Rising & Digestion",
            scientificTitle = "Anabolic Fed State & Glycogen Synthesis",
            hourStart = 0f,
            hourEnd = 4f,
            hoursRangeLabel = "0 – 4 Hours",
            icon = "🍽️",
            accentColor = Color(0xFF60A5FA), // Blue
            shortSummary = "Digesting recent meals and transporting glucose and amino acids into cells.",
            physiologicalProcess = "Following food intake, circulating blood glucose rises. The pancreas secretes insulin to transport glucose into cells for immediate energy and store excess in the liver and muscle tissue as glycogen.",
            primaryFuelSource = "Ingested glucose and dietary macronutrients",
            biomarkers = StageBiomarkers(
                bloodSugar = "Rising ↗",
                insulin = "High / Spiking ↗",
                ketones = "0.0 mmol/L (Baseline)",
                autophagy = "Inhibited (mTOR on)",
                growthHormone = "Baseline"
            ),
            keyBenefits = listOf(
                "Active nutrient absorption into cells",
                "Replenishment of liver and muscle glycogen stores",
                "Amino acids utilised for muscle and tissue protein synthesis"
            ),
            tips = "Drink plain water. Hunger cues in this window are usually habitual routines or conditioned reflexes rather than metabolic hunger."
        ),
        MetabolicState(
            id = "post_absorptive",
            title = "Blood Sugar Drops & Glycogen Burn",
            scientificTitle = "Post-Absorptive State & Glycogenolysis",
            hourStart = 4f,
            hourEnd = 8f,
            hoursRangeLabel = "4 – 8 Hours",
            icon = "📉",
            accentColor = Color(0xFF38BDF8), // Light Blue
            shortSummary = "Digestion finishes, insulin falls to baseline, and the liver breaks down stored glycogen.",
            physiologicalProcess = "Gastrointestinal tract completes digestion. Circulating insulin drops toward baseline, signaling the liver to break down glycogen stores into glucose (glycogenolysis) to keep blood sugar stable.",
            primaryFuelSource = "Stored liver glycogen (carbohydrate stores)",
            biomarkers = StageBiomarkers(
                bloodSugar = "Declining to Normal ↘",
                insulin = "Falling toward Baseline ↘",
                ketones = "0.1 mmol/L (Trace)",
                autophagy = "Dormant",
                growthHormone = "Baseline"
            ),
            keyBenefits = listOf(
                "Insulin returns to resting baseline",
                "Gastrointestinal tract rests from active digestion",
                "Blood glucose stabilizes without meal spikes"
            ),
            tips = "If you feel a mild afternoon dip or hunger pang, sip cold water or black coffee. The sensation typically passes in 10-15 minutes."
        ),
        MetabolicState(
            id = "fat_mobilisation",
            title = "Early Fat Burning & Glycogen Depletion",
            scientificTitle = "Lipolysis Activation & Gluconeogenesis",
            hourStart = 8f,
            hourEnd = 12f,
            hoursRangeLabel = "8 – 12 Hours",
            icon = "🔥",
            accentColor = Color(0xFFFBBF24), // Amber
            shortSummary = "Liver glycogen runs low; hormone-sensitive lipase activates to release stored body fat.",
            physiologicalProcess = "With liver glycogen reserves significantly diminished, the body activates hormone-sensitive lipase (HSL). Triglycerides from adipose tissue are cleaved into free fatty acids and glycerol to fuel muscles.",
            primaryFuelSource = "Body fat stores & free fatty acids (transitioning)",
            biomarkers = StageBiomarkers(
                bloodSugar = "Stable Basal 🟢",
                insulin = "Low Baseline 📉",
                ketones = "0.2 – 0.5 mmol/L (Mild)",
                autophagy = "Priming",
                growthHormone = "Rising (+50%) ↗"
            ),
            keyBenefits = listOf(
                "Hormone-sensitive lipase activates fat release",
                "Stored body fat mobilised as fuel",
                "Digestive inflammation decreases substantially"
            ),
            tips = "Overnight sleep usually spans this entire stage, meaning you wake up already in fat-burning mode!"
        ),
        MetabolicState(
            id = "ketosis_onset",
            title = "Metabolic Shift & Ketosis Entry",
            scientificTitle = "Ketogenesis & Accelerated Fat Oxidation",
            hourStart = 12f,
            hourEnd = 18f,
            hoursRangeLabel = "12 – 18 Hours",
            icon = "⚡",
            accentColor = Color(0xFF34D399), // Emerald Green
            shortSummary = "The liver produces ketone bodies (BHB) for clean fuel, boosting focus and fat burning.",
            physiologicalProcess = "With low insulin and depleted glycogen, the liver ramps up beta-oxidation of fatty acids, creating ketone bodies (acetoacetate and beta-hydroxybutyrate). Ketones cross the blood-brain barrier for high-efficiency neuronal energy.",
            primaryFuelSource = "Ketone bodies & free fatty acids",
            biomarkers = StageBiomarkers(
                bloodSugar = "Low Stable 🟢",
                insulin = "Minimal / Suppressed 📉",
                ketones = "0.5 – 1.5 mmol/L (Nutritional Ketosis)",
                autophagy = "Initiating 🧬",
                growthHormone = "+100% – +200% ↗"
            ),
            keyBenefits = listOf(
                "Nutritional ketosis activated (BHB & Acetoacetate)",
                "Enhanced mental clarity and sustained energy",
                "Hunger hormone (ghrelin) stabilizes, reducing cravings",
                "Accelerated abdominal and visceral fat oxidation"
            ),
            tips = "Add a pinch of natural salt (electrolytes) to your water. This is the cornerstone window for 16:8 intermittent fasting."
        ),
        MetabolicState(
            id = "autophagy",
            title = "Autophagy & Cellular Cleanup",
            scientificTitle = "Autophagy Activation & Proteostasis",
            hourStart = 18f,
            hourEnd = 24f,
            hoursRangeLabel = "18 – 24 Hours",
            icon = "🧬",
            accentColor = Color(0xFFA78BFA), // Purple
            shortSummary = "Cells recycle damaged components, clearing out senescent cells and misfolded proteins.",
            physiologicalProcess = "AMP-activated protein kinase (AMPK) increases while mTOR is suppressed. Cells initiate autophagy ('self-eating'), delivering damaged organelles, oxidized proteins, and cellular debris to lysosomes for recycling into fresh building blocks.",
            primaryFuelSource = "Ketones and cellular recycled constituents",
            biomarkers = StageBiomarkers(
                bloodSugar = "Depleted Baseline 🟢",
                insulin = "Near-Zero 📉",
                ketones = "1.5 – 2.5 mmol/L (Elevated)",
                autophagy = "Peak Cellular Cleanup 🧬🔥",
                growthHormone = "+300% Surge 🚀"
            ),
            keyBenefits = listOf(
                "Cellular housekeeping and damaged organelle recycling",
                "Downregulation of systemic inflammation markers",
                "Enhanced mitochondrial biogenesis and quality",
                "Longevity pathways activated (AMPK & Sirtuins)"
            ),
            tips = "Stay well hydrated. A brisk walk or light movement stimulates AMPK and boosts autophagy further."
        ),
        MetabolicState(
            id = "deep_ketosis_hgh",
            title = "Deep Ketosis & Growth Hormone Surge",
            scientificTitle = "Peak Human Growth Hormone (HGH) & Deep Ketosis",
            hourStart = 24f,
            hourEnd = 48f,
            hoursRangeLabel = "24 – 48 Hours",
            icon = "💪",
            accentColor = Color(0xFFF43F5E), // Rose Red
            shortSummary = "HGH increases up to 5x to preserve muscle while fat oxidation operates at maximum capacity.",
            physiologicalProcess = "To protect lean muscle tissue and maintain bone mineral density during prolonged fasting, the pituitary gland surges Human Growth Hormone (HGH) secretion by up to 500%. Liver gluconeogenesis runs steadily from glycerol and amino acids.",
            primaryFuelSource = "Deep ketone concentrations & fatty acids",
            biomarkers = StageBiomarkers(
                bloodSugar = "Endogenous Gluconeogenesis 🟢",
                insulin = "Baseline Floor 📉",
                ketones = "2.5 – 3.5 mmol/L (Deep Ketosis)",
                autophagy = "Sustained Deep Recycling 🧬",
                growthHormone = "Up to +500% Peak 👑"
            ),
            keyBenefits = listOf(
                "Human Growth Hormone (HGH) rises up to 500%",
                "Protection of lean muscle mass and bone density",
                "Maximum rate of fat loss and insulin sensitivity",
                "Significant reduction in blood glucose variability"
            ),
            tips = "Prioritize electrolytes (sodium, potassium, magnesium). Avoid strenuous anaerobic weightlifting; stick to mobility and walking."
        ),
        MetabolicState(
            id = "immune_regeneration",
            title = "Immune Reset & Stem Cell Renewal",
            scientificTitle = "Hematopoietic Stem Cell Regeneration & Prolonged Autophagy",
            hourStart = 48f,
            hourEnd = null,
            hoursRangeLabel = "48 – 72+ Hours",
            icon = "🛡️",
            accentColor = Color(0xFFF97316), // Orange
            shortSummary = "Damaged white blood cells are recycled, signaling stem cells to regenerate fresh immune cells.",
            physiologicalProcess = "Extended fasting induces degradation of older, damaged immune cells (white blood cells) and lowers circulating IGF-1. Refeeding triggers hematopoietic stem cells to generate a rejuvenated immune population.",
            primaryFuelSource = "Sustained high-level ketone oxidation",
            biomarkers = StageBiomarkers(
                bloodSugar = "Stable Basal (Liver Ketogenesis) 🟢",
                insulin = "Baseline Floor 📉",
                ketones = "3.5+ mmol/L (Maximum Fasting Ketosis)",
                autophagy = "Deep Stem Cell Cleansing 🛡️",
                growthHormone = "Elevated Anabolic Reserve"
            ),
            keyBenefits = listOf(
                "Recycling of damaged and aged immune cells",
                "Hematopoietic stem cell stimulation for immune renewal",
                "Maximum reduction in cellular IGF-1 and systemic inflammation",
                "Profound improvement in long-term metabolic flexibility"
            ),
            tips = "When breaking a fast of 48h+, refeed gently with bone broth, steamed vegetables, or eggs before eating a full meal."
        )
    )

    fun getCurrentStage(hoursElapsed: Float): MetabolicState {
        return ALL_STAGES.lastOrNull { hoursElapsed >= it.hourStart } ?: ALL_STAGES.first()
    }

    fun getNextStage(hoursElapsed: Float): MetabolicState? {
        val currentIndex = ALL_STAGES.indexOfLast { hoursElapsed >= it.hourStart }
        return if (currentIndex >= 0 && currentIndex < ALL_STAGES.size - 1) {
            ALL_STAGES[currentIndex + 1]
        } else {
            null
        }
    }
}

