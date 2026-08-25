package com.example.data.local

data class UkFoodProduct(
    val id: String,
    val name: String,
    val supermarketOrBrand: String,
    val category: String,
    val caloriesPer100g: Int,
    val defaultServing: String,
    val caloriesPerServing: Int,
    val proteinGrams: Float = 0f,
    val carbsGrams: Float = 0f,
    val fatGrams: Float = 0f,
    val barcode: String? = null
)

object UkFoodCatalog {
    val items: List<UkFoodProduct> = listOf(
        // ================= TESCO =================
        UkFoodProduct("tesco_1", "Tesco Chicken & Bacon Caesar Wrap", "Tesco", "Sandwiches & Wraps", 225, "1 wrap (185g)", 416, 21.2f, 38.5f, 18.2f, "5054775981201"),
        UkFoodProduct("tesco_2", "Tesco Meal Deal Triple Sandwich", "Tesco", "Sandwiches & Wraps", 240, "1 pack (240g)", 576, 24.0f, 62.0f, 25.0f, "5054775981202"),
        UkFoodProduct("tesco_3", "Tesco British Semi-Skimmed Milk", "Tesco", "Dairy & Milk", 48, "1 glass (200ml)", 96, 7.2f, 9.6f, 3.6f, "5000436001012"),
        UkFoodProduct("tesco_4", "Tesco British Whole Milk", "Tesco", "Dairy & Milk", 64, "1 glass (200ml)", 128, 7.0f, 9.4f, 7.2f, "5000436001029"),
        UkFoodProduct("tesco_5", "Tesco British Skimmed Milk", "Tesco", "Dairy & Milk", 35, "1 glass (200ml)", 70, 7.2f, 10.0f, 0.2f, "5000436001036"),
        UkFoodProduct("tesco_6", "Tesco Greek Style Natural Yogurt", "Tesco", "Dairy & Yogurts", 120, "1 pot (150g)", 180, 7.5f, 6.0f, 14.5f, "5054775010041"),
        UkFoodProduct("tesco_7", "Tesco 0% Fat Greek Style Yogurt", "Tesco", "Dairy & Yogurts", 57, "1 pot (150g)", 85, 15.0f, 6.2f, 0.2f, "5054775010058"),
        UkFoodProduct("tesco_8", "Tesco Finest Wood Fired Pepperoni Pizza", "Tesco", "Ready Meals & Pizza", 272, "1/2 pizza (215g)", 585, 23.5f, 64.0f, 25.0f, "5054775123456"),
        UkFoodProduct("tesco_9", "Tesco Lasagne Ready Meal (400g)", "Tesco", "Ready Meals", 142, "1 pack (400g)", 568, 28.0f, 54.0f, 24.0f, "5054775234567"),
        UkFoodProduct("tesco_10", "Tesco Chicken Tikka Masala with Pilau Rice", "Tesco", "Ready Meals", 138, "1 pack (450g)", 621, 32.0f, 78.0f, 18.0f, "5054775345678"),
        UkFoodProduct("tesco_11", "Tesco British Chicken Breast Portions", "Tesco", "Meat & Poultry", 106, "1 breast (150g)", 159, 36.0f, 0.0f, 1.8f, "5054775456789"),
        UkFoodProduct("tesco_12", "Tesco White Sourdough Bloomer", "Tesco", "Bakery", 235, "1 slice (45g)", 106, 4.2f, 21.0f, 0.6f, "5054775567890"),
        UkFoodProduct("tesco_13", "Tesco Toastie Thick Slice White Bread", "Tesco", "Bakery", 228, "1 slice (44g)", 100, 3.8f, 19.5f, 0.8f, "5054775678901"),
        UkFoodProduct("tesco_14", "Tesco Wholemeal Medium Bread", "Tesco", "Bakery", 215, "1 slice (40g)", 86, 4.0f, 16.0f, 1.0f, "5054775789012"),
        UkFoodProduct("tesco_15", "Tesco Free Range Large Eggs", "Tesco", "Dairy & Eggs", 131, "1 egg (60g)", 79, 7.5f, 0.1f, 5.5f, "5054775890123"),
        UkFoodProduct("tesco_16", "Tesco Rolled Porridge Oats", "Tesco", "Breakfast & Cereals", 370, "1 bowl (40g)", 148, 4.8f, 24.8f, 3.2f, "5054775901234"),
        UkFoodProduct("tesco_17", "Tesco Microwave Basmati Rice", "Tesco", "Pasta & Rice", 160, "1/2 pouch (125g)", 200, 4.2f, 42.0f, 1.5f, "5054775912345"),
        UkFoodProduct("tesco_18", "Tesco Meal Deal Snack Sausage Roll", "Tesco", "Snacks & Savouries", 320, "1 roll (65g)", 208, 6.0f, 18.0f, 12.5f, "5054775923456"),
        UkFoodProduct("tesco_19", "Tesco Pure Smooth Orange Juice", "Tesco", "Drinks", 42, "1 glass (200ml)", 84, 1.4f, 19.0f, 0.2f, "5054775934567"),
        UkFoodProduct("tesco_20", "Tesco Ready Salted Crisps 6-Pack", "Tesco", "Snacks & Crisps", 520, "1 bag (25g)", 130, 1.5f, 13.0f, 8.0f, "5054775945678"),

        // ================= SAINSBURY'S =================
        UkFoodProduct("sains_1", "Sainsbury's Taste the Difference Wood Fired Margherita", "Sainsbury's", "Ready Meals & Pizza", 245, "1/2 pizza (210g)", 514, 21.0f, 60.0f, 20.0f, "0123456789011"),
        UkFoodProduct("sains_2", "Sainsbury's Meal Deal Prawn Mayo Sandwich", "Sainsbury's", "Sandwiches & Wraps", 205, "1 pack (175g)", 359, 14.5f, 38.0f, 16.5f, "0123456789012"),
        UkFoodProduct("sains_3", "Sainsbury's Chicken & Bacon Salad Bowl", "Sainsbury's", "Salads & Fresh", 115, "1 bowl (260g)", 299, 26.0f, 8.5f, 17.0f, "0123456789013"),
        UkFoodProduct("sains_4", "Sainsbury's British Semi-Skimmed Milk", "Sainsbury's", "Dairy & Milk", 49, "1 glass (200ml)", 98, 7.2f, 9.6f, 3.6f, "0123456789014"),
        UkFoodProduct("sains_5", "Sainsbury's 5% Fat Lean Beef Mince", "Sainsbury's", "Meat & Poultry", 131, "1 portion (125g)", 164, 26.5f, 0.0f, 6.2f, "0123456789015"),
        UkFoodProduct("sains_6", "Sainsbury's Bakery All Butter Croissants", "Sainsbury's", "Bakery", 412, "1 croissant (55g)", 227, 4.6f, 24.2f, 12.1f, "0123456789016"),
        UkFoodProduct("sains_7", "Sainsbury's Scottish Porridge Oats", "Sainsbury's", "Breakfast & Cereals", 374, "1 bowl (40g)", 150, 4.4f, 24.0f, 3.4f, "0123456789017"),
        UkFoodProduct("sains_8", "Sainsbury's Fairtrade Bananas", "Sainsbury's", "Fruit & Veg", 90, "1 banana (110g)", 99, 1.2f, 22.5f, 0.3f, "0123456789018"),
        UkFoodProduct("sains_9", "Sainsbury's Chunky Tomato & Basil Soup", "Sainsbury's", "Soups", 48, "1/2 pot (300g)", 144, 2.7f, 18.0f, 6.0f, "0123456789019"),
        UkFoodProduct("sains_10", "Sainsbury's Taste the Difference Sourdough", "Sainsbury's", "Bakery", 240, "1 slice (45g)", 108, 4.0f, 22.0f, 0.7f, "0123456789020"),

        // ================= ASDA =================
        UkFoodProduct("asda_1", "ASDA Extra Special Aberdeen Angus Beef Lasagne", "ASDA", "Ready Meals", 155, "1 pack (400g)", 620, 31.0f, 52.0f, 30.0f, "5057172000011"),
        UkFoodProduct("asda_2", "ASDA Just Essentials Baked Beans in Tomato Sauce", "ASDA", "Canned & Tins", 78, "1/2 can (200g)", 156, 9.2f, 24.8f, 0.6f, "5057172000012"),
        UkFoodProduct("asda_3", "ASDA Bakery White Baps / Bread Rolls", "ASDA", "Bakery", 248, "1 roll (65g)", 161, 5.5f, 30.0f, 1.8f, "5057172000013"),
        UkFoodProduct("asda_4", "ASDA Stonebaked Double Pepperoni Pizza", "ASDA", "Ready Meals & Pizza", 265, "1/2 pizza (190g)", 504, 22.0f, 55.0f, 21.0f, "5057172000014"),
        UkFoodProduct("asda_5", "ASDA Fresh British Chicken Breast Fillets", "ASDA", "Meat & Poultry", 108, "1 fillet (150g)", 162, 35.5f, 0.0f, 2.0f, "5057172000015"),
        UkFoodProduct("asda_6", "ASDA Diet Cola (2 Litre / Can)", "ASDA", "Drinks", 1, "1 can (330ml)", 3, 0.0f, 0.0f, 0.0f, "5057172000016"),
        UkFoodProduct("asda_7", "ASDA Mild Cheddar Cheese", "ASDA", "Dairy & Cheese", 416, "1 portion (30g)", 125, 7.6f, 0.1f, 10.4f, "5057172000017"),
        UkFoodProduct("asda_8", "ASDA Crunchy Peanut Butter (No Added Sugar)", "ASDA", "Spreads & Jams", 595, "1 tbsp (15g)", 89, 4.2f, 1.8f, 7.5f, "5057172000018"),

        // ================= M&S (MARKS & SPENCER) =================
        UkFoodProduct("ms_1", "M&S Count On Us Chicken Tikka Masala", "M&S", "Ready Meals", 94, "1 pack (380g)", 357, 34.0f, 41.0f, 6.1f, "0012345678991"),
        UkFoodProduct("ms_2", "M&S Plant Kitchen Sweet Potato Curry", "M&S", "Ready Meals", 98, "1 pack (380g)", 372, 8.5f, 52.0f, 12.0f, "0012345678992"),
        UkFoodProduct("ms_3", "M&S Percy Pig Fruity Gummy Sweets", "M&S", "Confectionery", 345, "1 portion (30g)", 104, 2.0f, 24.0f, 0.1f, "0012345678993"),
        UkFoodProduct("ms_4", "M&S Egg Salad Sandwich on Malted Bread", "M&S", "Sandwiches & Wraps", 200, "1 pack (180g)", 360, 16.0f, 36.0f, 16.0f, "0012345678994"),
        UkFoodProduct("ms_5", "M&S Gastropub Chunky Chips", "M&S", "Sides & Potato", 215, "1 portion (150g)", 323, 4.5f, 45.0f, 13.5f, "0012345678995"),
        UkFoodProduct("ms_6", "M&S High Protein Chocolate Pudding", "M&S", "Desserts & Protein", 78, "1 pot (200g)", 156, 20.0f, 10.0f, 3.0f, "0012345678996"),

        // ================= MORRISONS =================
        UkFoodProduct("morr_1", "Morrisons The Best British Pork Sausages", "Morrisons", "Meat & Sausages", 260, "2 sausages (110g)", 286, 17.0f, 8.0f, 21.0f, "5010724000011"),
        UkFoodProduct("morr_2", "Morrisons Meal Deal BLT Sandwich", "Morrisons", "Sandwiches & Wraps", 230, "1 pack (180g)", 414, 18.0f, 39.0f, 20.0f, "5010724000012"),
        UkFoodProduct("morr_3", "Morrisons Market Street Jumbo Sausage Roll", "Morrisons", "Bakery & Hot Food", 310, "1 roll (120g)", 372, 9.5f, 32.0f, 22.0f, "5010724000013"),

        // ================= ALDI & LIDL =================
        UkFoodProduct("aldi_1", "Aldi Brooklea 20g Protein Pudding Chocolate", "Aldi", "Dairy & Protein", 76, "1 pot (200g)", 152, 20.0f, 10.4f, 3.0f, "4088600000011"),
        UkFoodProduct("aldi_2", "Aldi Brooklea Protein Pouch Vanilla", "Aldi", "Dairy & Protein", 75, "1 pouch (200g)", 150, 20.0f, 11.0f, 2.8f, "4088600000012"),
        UkFoodProduct("aldi_3", "Aldi Village Bakery Soft White Medium", "Aldi", "Bakery", 225, "1 slice (40g)", 90, 3.6f, 18.0f, 0.8f, "4088600000013"),
        UkFoodProduct("lidl_1", "Lidl Bakery Pastel de Nata Custard Tart", "Lidl", "Bakery", 310, "1 tart (60g)", 186, 3.2f, 22.5f, 9.2f, "2000000000011"),
        UkFoodProduct("lidl_2", "Lidl Deluxe Scottish Smoked Salmon", "Lidl", "Fish & Seafood", 185, "1 portion (50g)", 93, 11.5f, 0.5f, 5.0f, "2000000000012"),

        // ================= POPULAR UK BRANDS & STAPLES =================
        UkFoodProduct("heinz_1", "Heinz Baked Beanz in Tomato Sauce", "Heinz", "Canned Staples", 81, "1/2 can (207g)", 168, 9.7f, 26.7f, 0.4f, "5000157024671"),
        UkFoodProduct("heinz_2", "Heinz Cream of Tomato Soup", "Heinz", "Canned Staples", 51, "1/2 can (200g)", 102, 1.6f, 12.6f, 4.4f, "5000157074492"),
        UkFoodProduct("heinz_3", "Heinz Tomato Ketchup", "Heinz", "Condiments & Sauces", 102, "1 tbsp (15g)", 15, 0.2f, 3.5f, 0.0f, "5000157004529"),
        UkFoodProduct("warb_1", "Warburtons Toastie Thick White Bread", "Warburtons", "Bakery", 238, "1 slice (45g)", 107, 4.2f, 20.0f, 1.0f, "5010044000701"),
        UkFoodProduct("warb_2", "Warburtons Crumpets (Pack of 6)", "Warburtons", "Bakery", 176, "1 crumpet (55g)", 97, 4.1f, 18.5f, 0.5f, "5010044000305"),
        UkFoodProduct("warb_3", "Warburtons Soft White Sandwich Thins", "Warburtons", "Bakery", 235, "1 thin (43g)", 100, 4.5f, 18.0f, 0.9f, "5010044004501"),
        UkFoodProduct("weet_1", "Weetabix Original Whole Wheat Biscuits", "Weetabix", "Breakfast & Cereals", 362, "2 biscuits (37.5g)", 136, 4.5f, 26.0f, 0.8f, "5010029000101"),
        UkFoodProduct("weet_2", "Ready Brek Original Smooth Porridge", "Weetabix", "Breakfast & Cereals", 374, "1 bowl (30g)", 112, 3.6f, 18.3f, 2.6f, "5010029000202"),
        UkFoodProduct("cad_1", "Cadbury Dairy Milk Chocolate Bar (45g)", "Cadbury", "Confectionery", 534, "1 bar (45g)", 240, 3.3f, 25.5f, 13.5f, "7622210287123"),
        UkFoodProduct("cad_2", "Cadbury Twirl Chocolate Bar (43g)", "Cadbury", "Confectionery", 530, "1 pack (43g)", 228, 3.0f, 24.0f, 13.0f, "7622210287130"),
        UkFoodProduct("walk_1", "Walkers Ready Salted Potato Crisps", "Walkers", "Snacks & Crisps", 522, "1 bag (25g)", 131, 1.5f, 13.2f, 7.9f, "5000328123456"),
        UkFoodProduct("walk_2", "Walkers Cheese & Onion Crisps", "Walkers", "Snacks & Crisps", 518, "1 bag (25g)", 130, 1.6f, 13.0f, 7.8f, "5000328234567"),
        UkFoodProduct("walk_3", "Walkers Sensations Thai Sweet Chilli", "Walkers", "Snacks & Crisps", 498, "1 bag (40g)", 199, 2.8f, 24.0f, 10.0f, "5000328345678"),
        UkFoodProduct("greg_1", "Greggs Sausage Roll (Hot/Cold)", "Greggs", "Bakery & Pastry", 328, "1 roll (100g)", 328, 9.4f, 24.0f, 21.6f, "5060000000011"),
        UkFoodProduct("greg_2", "Greggs Vegan Sausage Roll", "Greggs", "Bakery & Pastry", 311, "1 roll (100g)", 311, 12.0f, 26.0f, 17.5f, "5060000000012"),
        UkFoodProduct("greg_3", "Greggs Steak Bake", "Greggs", "Bakery & Pastry", 285, "1 bake (140g)", 399, 14.0f, 32.0f, 24.0f, "5060000000013"),
        UkFoodProduct("pret_1", "Pret A Manger Cheddar & Pickle Baguette", "Pret", "Sandwiches & Baguettes", 260, "1 baguette (210g)", 546, 21.0f, 65.0f, 21.0f, "5060000000021"),
        UkFoodProduct("pret_2", "Pret A Manger Tuna & Cucumber Baguette", "Pret", "Sandwiches & Baguettes", 230, "1 baguette (220g)", 506, 26.0f, 62.0f, 16.0f, "5060000000022"),
        UkFoodProduct("nand_1", "Nando's 1/2 Chicken Peri-Peri (Medium)", "Nando's", "Restaurant Food", 175, "1/2 chicken (320g)", 560, 68.0f, 2.0f, 31.0f, "5060000000031"),
        UkFoodProduct("nand_2", "Nando's Peri-Peri Chips (Regular)", "Nando's", "Sides", 230, "1 portion (160g)", 368, 5.0f, 48.0f, 17.0f, "5060000000032"),
        UkFoodProduct("inn_1", "Innocent Strawberry & Banana Smoothie", "Innocent", "Drinks", 54, "1 bottle (250ml)", 135, 1.2f, 28.5f, 0.3f, "5038862123456"),
        UkFoodProduct("alp_1", "Alpro Oat No Sugars Plant Milk", "Alpro", "Dairy Alternatives", 40, "1 glass (200ml)", 80, 0.4f, 11.2f, 3.0f, "5411188123456"),
        UkFoodProduct("luco_1", "Lucozade Energy Original (330ml Can)", "Lucozade", "Drinks", 37, "1 can (330ml)", 122, 0.0f, 29.7f, 0.0f, "5054267000011"),
        UkFoodProduct("coke_1", "Diet Coke (330ml Can)", "Coca-Cola", "Drinks", 1, "1 can (330ml)", 3, 0.0f, 0.0f, 0.0f, "5449000000996"),
        UkFoodProduct("coke_2", "Coca-Cola Original (330ml Can)", "Coca-Cola", "Drinks", 42, "1 can (330ml)", 139, 0.0f, 35.0f, 0.0f, "5449000000286"),
        UkFoodProduct("york_1", "Yorkshire Tea with Splash of Semi-Skimmed Milk", "Yorkshire Tea", "Hot Drinks", 15, "1 mug (250ml)", 25, 1.5f, 2.0f, 0.8f, "5010357000011"),
        UkFoodProduct("quorn_1", "Quorn Mince (Meat Free)", "Quorn", "Vegetarian & Vegan", 105, "1 portion (100g)", 105, 14.5f, 4.5f, 2.0f, "5019520000011"),
        UkFoodProduct("muller_1", "Müller Light Vanilla with Chocolate Sprinkles", "Müller", "Dairy & Yogurts", 54, "1 pot (160g)", 86, 7.5f, 10.4f, 1.2f, "4025500000011"),
        UkFoodProduct("mcvi_1", "McVitie's Milk Chocolate Digestives", "McVitie's", "Biscuits", 493, "1 biscuit (16.7g)", 82, 1.1f, 10.7f, 3.9f, "5000168000011")
    )
}
