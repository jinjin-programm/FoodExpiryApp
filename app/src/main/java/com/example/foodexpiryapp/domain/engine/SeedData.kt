package com.example.foodexpiryapp.domain.engine

import com.example.foodexpiryapp.data.local.database.ShelfLifeEntity

object SeedData {

    fun getSeedEntries(): List<ShelfLifeEntity> {
        val now = System.currentTimeMillis()
        return entries.map { (name, data) ->
            ShelfLifeEntity(
                foodName = name,
                shelfLifeDays = data.days,
                category = data.category,
                location = data.location,
                source = "manual",
                hitCount = 0,
                createdAt = now,
                updatedAt = now
            )
        }
    }

    private data class EntryData(
        val days: Int,
        val category: String,
        val location: String
    )

    private val entries: Map<String, EntryData> = mapOf(

        // ==================== DAIRY & EGGS ====================
        "milk" to EntryData(7, "DAIRY", "FRIDGE"),
        "\u5976" to EntryData(7, "DAIRY", "FRIDGE"),
        "\u9bae\u5976" to EntryData(7, "DAIRY", "FRIDGE"),
        "\u9bae\u4e73" to EntryData(7, "DAIRY", "FRIDGE"),
        "cheese" to EntryData(14, "DAIRY", "FRIDGE"),
        "\u829d\u58eb" to EntryData(14, "DAIRY", "FRIDGE"),
        "\u8d77\u53f8" to EntryData(14, "DAIRY", "FRIDGE"),
        "soft cheese" to EntryData(7, "DAIRY", "FRIDGE"),
        "hard cheese" to EntryData(21, "DAIRY", "FRIDGE"),
        "yogurt" to EntryData(14, "DAIRY", "FRIDGE"),
        "\u9178\u5976" to EntryData(14, "DAIRY", "FRIDGE"),
        "\u512a\u683c" to EntryData(14, "DAIRY", "FRIDGE"),
        "butter" to EntryData(90, "DAIRY", "FRIDGE"),
        "\u5976\u6cb9" to EntryData(90, "DAIRY", "FRIDGE"),
        "\u725b\u6cb9" to EntryData(90, "DAIRY", "FRIDGE"),
        "cream" to EntryData(7, "DAIRY", "FRIDGE"),
        "sour cream" to EntryData(14, "DAIRY", "FRIDGE"),
        "cottage cheese" to EntryData(7, "DAIRY", "FRIDGE"),
        "cream cheese" to EntryData(14, "DAIRY", "FRIDGE"),
        "egg" to EntryData(35, "DAIRY", "FRIDGE"),
        "\u86cb" to EntryData(35, "DAIRY", "FRIDGE"),
        "\u96de\u86cb" to EntryData(35, "DAIRY", "FRIDGE"),
        "mayonnaise" to EntryData(60, "CONDIMENTS", "FRIDGE"),

        // ==================== MEAT & POULTRY ====================
        "chicken" to EntryData(2, "MEAT", "FRIDGE"),
        "\u96de" to EntryData(3, "MEAT", "FRIDGE"),
        "\u96de\u8089" to EntryData(3, "MEAT", "FRIDGE"),
        "beef" to EntryData(3, "MEAT", "FRIDGE"),
        "\u725b\u8089" to EntryData(3, "MEAT", "FRIDGE"),
        "steak" to EntryData(3, "MEAT", "FRIDGE"),
        "ground beef" to EntryData(2, "MEAT", "FRIDGE"),
        "pork" to EntryData(3, "MEAT", "FRIDGE"),
        "\u8c6c" to EntryData(3, "MEAT", "FRIDGE"),
        "\u8c6c\u8089" to EntryData(3, "MEAT", "FRIDGE"),
        "ham" to EntryData(7, "MEAT", "FRIDGE"),
        "bacon" to EntryData(7, "MEAT", "FRIDGE"),
        "sausage" to EntryData(7, "MEAT", "FRIDGE"),
        "hot dog" to EntryData(7, "MEAT", "FRIDGE"),
        "deli meat" to EntryData(5, "MEAT", "FRIDGE"),
        "salami" to EntryData(14, "MEAT", "FRIDGE"),
        "pepperoni" to EntryData(21, "MEAT", "FRIDGE"),
        "turkey" to EntryData(2, "MEAT", "FRIDGE"),

        // ==================== SEAFOOD ====================
        "fish" to EntryData(2, "MEAT", "FRIDGE"),
        "\u9b5a" to EntryData(2, "MEAT", "FRIDGE"),
        "seafood" to EntryData(2, "MEAT", "FRIDGE"),
        "shrimp" to EntryData(2, "MEAT", "FRIDGE"),
        "\u8766" to EntryData(2, "MEAT", "FRIDGE"),
        "crab" to EntryData(2, "MEAT", "FRIDGE"),
        "lobster" to EntryData(2, "MEAT", "FRIDGE"),
        "salmon" to EntryData(2, "MEAT", "FRIDGE"),
        "tuna" to EntryData(2, "MEAT", "FRIDGE"),
        "cod" to EntryData(2, "MEAT", "FRIDGE"),
        "oyster" to EntryData(2, "MEAT", "FRIDGE"),

        // ==================== FRUITS ====================
        "apple" to EntryData(21, "FRUITS", "COUNTER"),
        "\u82f9\u679c" to EntryData(21, "FRUITS", "COUNTER"),
        "pear" to EntryData(14, "FRUITS", "COUNTER"),
        "banana" to EntryData(5, "FRUITS", "COUNTER"),
        "\u9999\u8549" to EntryData(5, "FRUITS", "COUNTER"),
        "orange" to EntryData(21, "FRUITS", "COUNTER"),
        "\u6a59" to EntryData(21, "FRUITS", "COUNTER"),
        "\u6a58\u5b50" to EntryData(21, "FRUITS", "COUNTER"),
        "grapefruit" to EntryData(21, "FRUITS", "COUNTER"),
        "lemon" to EntryData(21, "FRUITS", "COUNTER"),
        "lime" to EntryData(21, "FRUITS", "COUNTER"),
        "grape" to EntryData(7, "FRUITS", "FRIDGE"),
        "strawberry" to EntryData(3, "FRUITS", "FRIDGE"),
        "blueberry" to EntryData(7, "FRUITS", "FRIDGE"),
        "raspberry" to EntryData(3, "FRUITS", "FRIDGE"),
        "watermelon" to EntryData(7, "FRUITS", "FRIDGE"),
        "melon" to EntryData(7, "FRUITS", "FRIDGE"),
        "pineapple" to EntryData(5, "FRUITS", "COUNTER"),
        "mango" to EntryData(7, "FRUITS", "COUNTER"),
        "peach" to EntryData(5, "FRUITS", "COUNTER"),
        "cherry" to EntryData(7, "FRUITS", "FRIDGE"),
        "kiwi" to EntryData(7, "FRUITS", "COUNTER"),
        "avocado" to EntryData(5, "FRUITS", "COUNTER"),

        // ==================== VEGETABLES ====================
        "tomato" to EntryData(7, "VEGETABLES", "COUNTER"),
        "\u756a\u8304" to EntryData(7, "VEGETABLES", "COUNTER"),
        "\u897f\u7ea2\u67ff" to EntryData(5, "VEGETABLES", "COUNTER"),
        "lettuce" to EntryData(5, "VEGETABLES", "FRIDGE"),
        "\u751f\u83dc" to EntryData(5, "VEGETABLES", "FRIDGE"),
        "spinach" to EntryData(5, "VEGETABLES", "FRIDGE"),
        "kale" to EntryData(5, "VEGETABLES", "FRIDGE"),
        "cabbage" to EntryData(14, "VEGETABLES", "FRIDGE"),
        "broccoli" to EntryData(5, "VEGETABLES", "FRIDGE"),
        "\u897f\u5170\u82b1" to EntryData(5, "VEGETABLES", "FRIDGE"),
        "cauliflower" to EntryData(5, "VEGETABLES", "FRIDGE"),
        "carrot" to EntryData(21, "VEGETABLES", "FRIDGE"),
        "\u80e1\u841d\u8353" to EntryData(21, "VEGETABLES", "FRIDGE"),
        "\u7ea2\u841d\u8353" to EntryData(21, "VEGETABLES", "FRIDGE"),
        "potato" to EntryData(30, "VEGETABLES", "PANTRY"),
        "sweet potato" to EntryData(30, "VEGETABLES", "PANTRY"),
        "onion" to EntryData(30, "VEGETABLES", "PANTRY"),
        "garlic" to EntryData(90, "VEGETABLES", "PANTRY"),
        "cucumber" to EntryData(7, "VEGETABLES", "FRIDGE"),
        "zucchini" to EntryData(7, "VEGETABLES", "FRIDGE"),
        "pepper" to EntryData(14, "VEGETABLES", "FRIDGE"),
        "bell pepper" to EntryData(14, "VEGETABLES", "FRIDGE"),
        "celery" to EntryData(14, "VEGETABLES", "FRIDGE"),
        "corn" to EntryData(3, "VEGETABLES", "FRIDGE"),
        "mushroom" to EntryData(7, "VEGETABLES", "FRIDGE"),
        "eggplant" to EntryData(7, "VEGETABLES", "COUNTER"),
        "asparagus" to EntryData(5, "VEGETABLES", "FRIDGE"),
        "green beans" to EntryData(5, "VEGETABLES", "FRIDGE"),
        "green onion" to EntryData(14, "VEGETABLES", "FRIDGE"),
        "radish" to EntryData(14, "VEGETABLES", "FRIDGE"),
        "beet" to EntryData(21, "VEGETABLES", "FRIDGE"),
        "squash" to EntryData(14, "VEGETABLES", "FRIDGE"),

        // ==================== BAKERY & BREAD ====================
        "bread" to EntryData(7, "GRAINS", "PANTRY"),
        "\u9eb5\u5305" to EntryData(7, "GRAINS", "PANTRY"),
        "baguette" to EntryData(2, "GRAINS", "PANTRY"),
        "croissant" to EntryData(3, "GRAINS", "PANTRY"),
        "cake" to EntryData(5, "GRAINS", "FRIDGE"),
        "pastry" to EntryData(5, "GRAINS", "FRIDGE"),
        "cookie" to EntryData(30, "SNACKS", "PANTRY"),
        "\u9905\u4e7e" to EntryData(30, "SNACKS", "PANTRY"),
        "cracker" to EntryData(90, "GRAINS", "PANTRY"),
        "donut" to EntryData(3, "GRAINS", "PANTRY"),
        "bagel" to EntryData(7, "GRAINS", "PANTRY"),
        "tortilla" to EntryData(7, "GRAINS", "PANTRY"),
        "muffin" to EntryData(7, "GRAINS", "PANTRY"),
        "\u767d\u98ef" to EntryData(2, "LEFTOVERS", "FRIDGE"),

        // ==================== GRAINS & PASTA ====================
        "rice" to EntryData(730, "GRAINS", "PANTRY"),
        "\u7c73" to EntryData(730, "GRAINS", "PANTRY"),
        "pasta" to EntryData(730, "GRAINS", "PANTRY"),
        "noodles" to EntryData(730, "GRAINS", "PANTRY"),
        "\u9eb5" to EntryData(5, "GRAINS", "PANTRY"),
        "\u610f\u5927\u5229\u9eb5" to EntryData(730, "GRAINS", "PANTRY"),
        "quinoa" to EntryData(365, "GRAINS", "PANTRY"),
        "oats" to EntryData(365, "GRAINS", "PANTRY"),
        "oatmeal" to EntryData(365, "GRAINS", "PANTRY"),
        "cereal" to EntryData(90, "GRAINS", "PANTRY"),
        "granola" to EntryData(30, "GRAINS", "PANTRY"),
        "barley" to EntryData(365, "GRAINS", "PANTRY"),
        "couscous" to EntryData(365, "GRAINS", "PANTRY"),

        // ==================== BEVERAGES ====================
        "juice" to EntryData(7, "BEVERAGES", "FRIDGE"),
        "\u679c\u6c41" to EntryData(7, "BEVERAGES", "FRIDGE"),
        "tea" to EntryData(7, "BEVERAGES", "PANTRY"),
        "\u8336" to EntryData(7, "BEVERAGES", "PANTRY"),
        "coffee" to EntryData(14, "BEVERAGES", "PANTRY"),
        "\u5496\u5561" to EntryData(14, "BEVERAGES", "PANTRY"),
        "soda" to EntryData(90, "BEVERAGES", "PANTRY"),
        "water" to EntryData(180, "BEVERAGES", "PANTRY"),
        "smoothie" to EntryData(3, "BEVERAGES", "FRIDGE"),
        "wine" to EntryData(14, "BEVERAGES", "FRIDGE"),
        "beer" to EntryData(2, "BEVERAGES", "FRIDGE"),

        // ==================== SNACKS ====================
        "chips" to EntryData(30, "SNACKS", "PANTRY"),
        "chocolate" to EntryData(180, "SNACKS", "PANTRY"),
        "\u5de7\u514b\u529b" to EntryData(180, "SNACKS", "PANTRY"),
        "candy" to EntryData(365, "SNACKS", "PANTRY"),
        "popcorn" to EntryData(90, "SNACKS", "PANTRY"),
        "pretzel" to EntryData(90, "SNACKS", "PANTRY"),
        "nuts" to EntryData(180, "SNACKS", "PANTRY"),
        "peanut" to EntryData(180, "SNACKS", "PANTRY"),
        "almond" to EntryData(180, "SNACKS", "PANTRY"),
        "cashew" to EntryData(180, "SNACKS", "PANTRY"),
        "walnut" to EntryData(180, "SNACKS", "PANTRY"),
        "trail mix" to EntryData(90, "SNACKS", "PANTRY"),
        "granola bar" to EntryData(90, "SNACKS", "PANTRY"),

        // ==================== CONDIMENTS & SAUCES ====================
        "soy sauce" to EntryData(365, "CONDIMENTS", "PANTRY"),
        "\u91ac\u6cb9" to EntryData(365, "CONDIMENTS", "PANTRY"),
        "ketchup" to EntryData(180, "CONDIMENTS", "FRIDGE"),
        "mustard" to EntryData(180, "CONDIMENTS", "FRIDGE"),
        "oil" to EntryData(365, "CONDIMENTS", "PANTRY"),
        "\u6cb9" to EntryData(365, "CONDIMENTS", "PANTRY"),
        "vinegar" to EntryData(730, "CONDIMENTS", "PANTRY"),
        "honey" to EntryData(3650, "CONDIMENTS", "PANTRY"),
        "jam" to EntryData(180, "CONDIMENTS", "FRIDGE"),
        "peanut butter" to EntryData(90, "CONDIMENTS", "PANTRY"),
        "salsa" to EntryData(14, "CONDIMENTS", "FRIDGE"),
        "hot sauce" to EntryData(180, "CONDIMENTS", "PANTRY"),
        "barbecue sauce" to EntryData(180, "CONDIMENTS", "FRIDGE"),
        "teriyaki sauce" to EntryData(180, "CONDIMENTS", "FRIDGE"),
        "relish" to EntryData(365, "CONDIMENTS", "FRIDGE"),
        "tahini" to EntryData(180, "CONDIMENTS", "PANTRY"),
        "nutella" to EntryData(180, "CONDIMENTS", "PANTRY"),

        // ==================== CANNED & PACKAGED ====================
        "canned food" to EntryData(730, "OTHER", "PANTRY"),
        "soup" to EntryData(4, "OTHER", "FRIDGE"),
        "broth" to EntryData(4, "OTHER", "FRIDGE"),
        "pickle" to EntryData(365, "CONDIMENTS", "FRIDGE"),
        "olive" to EntryData(365, "CONDIMENTS", "PANTRY"),

        // ==================== FROZEN ====================
        "frozen food" to EntryData(90, "FROZEN", "FREEZER"),
        "ice cream" to EntryData(180, "FROZEN", "FREEZER"),
        "frozen pizza" to EntryData(60, "FROZEN", "FREEZER"),
        "pizza" to EntryData(4, "LEFTOVERS", "FRIDGE"),

        // ==================== LEFTOVERS ====================
        "leftover" to EntryData(4, "LEFTOVERS", "FRIDGE"),
        "leftovers" to EntryData(4, "LEFTOVERS", "FRIDGE"),
        "takeout" to EntryData(4, "LEFTOVERS", "FRIDGE"),

        // ==================== HERBS & SPICES ====================
        "basil" to EntryData(10, "VEGETABLES", "FRIDGE"),
        "cilantro" to EntryData(10, "VEGETABLES", "FRIDGE"),
        "parsley" to EntryData(10, "VEGETABLES", "FRIDGE"),
        "mint" to EntryData(10, "VEGETABLES", "FRIDGE"),
        "rosemary" to EntryData(14, "VEGETABLES", "FRIDGE"),
        "thyme" to EntryData(14, "VEGETABLES", "FRIDGE"),
        "spice" to EntryData(730, "CONDIMENTS", "PANTRY")
    )
}
