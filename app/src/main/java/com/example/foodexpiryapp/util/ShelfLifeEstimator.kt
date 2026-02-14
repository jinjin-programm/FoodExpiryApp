package com.example.foodexpiryapp.util

import java.time.LocalDate

/**
 * Comprehensive shelf life estimator based on USDA, FDA, and food safety guidelines.
 * Sources: FoodSafety.gov, USDA, FDA, Real Simple food storage guides
 */
object ShelfLifeEstimator {
    
    data class CategoryShelfLife(
        val days: Int,
        val category: String,
        val storageTip: String? = null
    )
    
    // Comprehensive category-based shelf life in days (refrigerated unless noted)
    // Sources: USDA Food Safety Guidelines, FDA, FoodSafety.gov
    private val categoryShelfLifeMap = mapOf(
        // ==================== DAIRY & EGGS ====================
        "dairy" to 7,
        "milk" to 7,
        "cheese" to 14,
        "cheeses" to 14,
        "soft cheese" to 7,
        "hard cheese" to 21,
        "yogurt" to 14,
        "yoghurt" to 14,
        "butter" to 90, // 3 months
        "cream" to 7,
        "sour cream" to 14,
        "cottage cheese" to 7,
        "cream cheese" to 14,
        "eggs" to 35, // 3-5 weeks
        "egg" to 35,
        "mayonnaise" to 60, // 2 months opened
        
        // ==================== MEAT & POULTRY ====================
        "meat" to 3,
        "beef" to 3,
        "steak" to 3,
        "ground beef" to 2,
        "hamburger" to 2,
        "chicken" to 2,
        "poultry" to 2,
        "turkey" to 2,
        "pork" to 3,
        "ham" to 7,
        "bacon" to 7,
        "sausage" to 7,
        "hot dog" to 7,
        "hotdog" to 7,
        "cold cuts" to 5,
        "deli meat" to 5,
        "lunch meat" to 5,
        "salami" to 14,
        "pepperoni" to 21,
        
        // ==================== SEAFOOD ====================
        "fish" to 2,
        "seafood" to 2,
        "shellfish" to 2,
        "shrimp" to 2,
        "crab" to 2,
        "lobster" to 2,
        "salmon" to 2,
        "tuna" to 2,
        "cod" to 2,
        "tilapia" to 2,
        "oyster" to 2,
        "mussel" to 2,
        "clam" to 2,
        
        // ==================== FRUITS ====================
        "fruit" to 7,
        "fruits" to 7,
        "apple" to 21,
        "apples" to 21,
        "pear" to 14,
        "pears" to 14,
        "banana" to 5,
        "bananas" to 5,
        "orange" to 21,
        "oranges" to 21,
        "grapefruit" to 21,
        "lemon" to 21,
        "lemons" to 21,
        "lime" to 21,
        "limes" to 21,
        "grape" to 7,
        "grapes" to 7,
        "berry" to 3,
        "berries" to 3,
        "strawberry" to 3,
        "strawberries" to 3,
        "blueberry" to 7,
        "blueberries" to 7,
        "raspberry" to 3,
        "raspberries" to 3,
        "blackberry" to 3,
        "blackberries" to 3,
        "watermelon" to 7,
        "melon" to 7,
        "cantaloupe" to 7,
        "honeydew" to 7,
        "pineapple" to 5,
        "mango" to 7,
        "peach" to 5,
        "peaches" to 5,
        "plum" to 5,
        "plums" to 5,
        "apricot" to 5,
        "cherry" to 7,
        "cherries" to 7,
        "kiwi" to 7,
        "avocado" to 5,
        
        // ==================== VEGETABLES ====================
        "vegetable" to 7,
        "vegetables" to 7,
        "tomato" to 7,
        "tomatoes" to 7,
        "lettuce" to 5,
        "leafy greens" to 5,
        "spinach" to 5,
        "kale" to 5,
        "arugula" to 5,
        "cabbage" to 14,
        "carrot" to 21,
        "carrots" to 21,
        "potato" to 30,
        "potatoes" to 30,
        "sweet potato" to 30,
        "onion" to 30,
        "onions" to 30,
        "shallot" to 30,
        "garlic" to 90, // 3 months
        "cucumber" to 7,
        "cucumbers" to 7,
        "zucchini" to 7,
        "squash" to 14,
        "pepper" to 14,
        "peppers" to 14,
        "bell pepper" to 14,
        "broccoli" to 5,
        "cauliflower" to 5,
        "asparagus" to 5,
        "celery" to 14,
        "corn" to 3,
        "green beans" to 5,
        "snap peas" to 5,
        "mushroom" to 7,
        "mushrooms" to 7,
        "eggplant" to 7,
        "radish" to 14,
        "beet" to 21,
        "brussels sprouts" to 7,
        "artichoke" to 7,
        "leek" to 14,
        "scallion" to 14,
        "green onion" to 14,
        
        // ==================== BAKERY & BREAD ====================
        "bread" to 7,
        "baguette" to 2,
        "croissant" to 3,
        "muffin" to 7,
        "cake" to 5,
        "pastry" to 5,
        "pie" to 5,
        "cookie" to 30,
        "cookies" to 30,
        "cracker" to 90,
        "crackers" to 90,
        "donut" to 3,
        "bagel" to 7,
        "tortilla" to 7,
        "pita" to 7,
        "roll" to 5,
        "bun" to 5,
        
        // ==================== GRAINS & PASTA ====================
        "pasta" to 4, // cooked pasta
        "noodles" to 4,
        "rice" to 4, // cooked rice
        "quinoa" to 5,
        "barley" to 5,
        "couscous" to 5,
        "oats" to 7, // cooked
        "oatmeal" to 7,
        "grains" to 5,
        "cereal" to 90, // opened
        "granola" to 30,
        
        // ==================== CANNED & PACKAGED ====================
        "canned" to 548, // ~1.5 years for high-acid
        "canned goods" to 730, // 2 years average
        "jar" to 365,
        "preserves" to 365,
        "pickle" to 365,
        "pickles" to 365,
        "olive" to 365,
        "olives" to 365,
        "soup" to 4, // opened/canned opened
        "broth" to 4,
        "stock" to 4,
        
        // ==================== SNACKS ====================
        "snack" to 90,
        "snacks" to 90,
        "chocolate" to 180,
        "candy" to 365,
        "chip" to 30,
        "chips" to 30,
        "popcorn" to 90,
        "pretzel" to 90,
        "nut" to 180,
        "nuts" to 180,
        "peanut" to 180,
        "almond" to 180,
        "cashew" to 180,
        "walnut" to 180,
        "seed" to 180,
        "seeds" to 180,
        "trail mix" to 90,
        
        // ==================== CONDIMENTS & SAUCES ====================
        "condiment" to 180,
        "condiments" to 180,
        "sauce" to 180,
        "ketchup" to 180,
        "mustard" to 180,
        "mayo" to 60,
        "relish" to 365,
        "salsa" to 14,
        "hot sauce" to 180,
        "soy sauce" to 365,
        "worcestershire" to 365,
        "barbecue" to 180,
        "bbq" to 180,
        "teriyaki" to 180,
        "oil" to 365,
        "vinegar" to 730,
        "honey" to 3650, // Almost forever
        "jam" to 180, // 6 months opened
        "jelly" to 180,
        "marmalade" to 180,
        "spread" to 180,
        "nutella" to 180,
        "peanut butter" to 90,
        "almond butter" to 90,
        "tahini" to 180,
        
        // ==================== BEVERAGES ====================
        "juice" to 7,
        "smoothie" to 3,
        "milkshake" to 3,
        "soda" to 2, // opened
        "soft drink" to 2,
        "coffee" to 7, // brewed
        "tea" to 7, // brewed
        "wine" to 14, // opened
        "beer" to 2, // opened
        "sports drink" to 7,
        "energy drink" to 7,
        
        // ==================== FROZEN ====================
        "frozen" to 90, // general frozen food
        "ice cream" to 180,
        "ice-cream" to 180,
        "gelato" to 180,
        "sorbet" to 180,
        "frozen dinner" to 90,
        "tv dinner" to 90,
        "frozen pizza" to 60,
        "pizza" to 4, // refrigerated leftover
        
        // ==================== LEFTOVERS ====================
        "leftover" to 4,
        "leftovers" to 4,
        "takeout" to 4,
        "delivery" to 4,
        
        // ==================== BABY FOOD ====================
        "baby food" to 2,
        "infant formula" to 1,
        "formula" to 1,
        
        // ==================== HERBS & SPICES ====================
        "herb" to 10,
        "herbs" to 10,
        "basil" to 10,
        "cilantro" to 10,
        "parsley" to 10,
        "mint" to 10,
        "dill" to 10,
        "chives" to 10,
        "rosemary" to 14,
        "thyme" to 14,
        "oregano" to 14,
        "spice" to 730,
        "spices" to 730,
        
        // ==================== DEFAULT ====================
        "other" to 7
    )
    
    // Storage tips for different categories
    private val storageTips = mapOf(
        "dairy" to "Keep refrigerated at 40°F (4°C) or below",
        "meat" to "Store on bottom shelf to prevent cross-contamination",
        "fish" to "Use within 1-2 days for best quality",
        "fruit" to "Keep in crisper drawer, don't wash until ready to eat",
        "vegetable" to "Store in crisper drawer with high humidity",
        "bread" to "Store in cool, dry place or freeze for longer storage",
        "leftover" to "Refrigerate within 2 hours of cooking",
        "canned" to "Store in cool, dry pantry; refrigerate after opening",
        "frozen" to "Keep at 0°F (-18°C) or below"
    )
    
    fun estimateShelfLife(categories: List<String>?): CategoryShelfLife {
        if (categories.isNullOrEmpty()) {
            return CategoryShelfLife(7, "Other", storageTips["other"])
        }
        
        // Check each category tag against our map
        for (category in categories) {
            val lowerCategory = category.lowercase().trim()
            
            // Skip empty or generic tags
            if (lowerCategory.isEmpty() || lowerCategory in listOf("foods", "food products", "groceries")) {
                continue
            }
            
            // Try exact match first
            categoryShelfLifeMap[lowerCategory]?.let { days ->
                val tip = findStorageTip(lowerCategory)
                return CategoryShelfLife(days, formatCategoryName(lowerCategory), tip)
            }
            
            // Try partial match (more specific matches first)
            val sortedMatches = categoryShelfLifeMap.entries
                .filter { (key, _) -> 
                    lowerCategory.contains(key) || key.contains(lowerCategory)
                }
                .sortedByDescending { (key, _) -> key.length } // Longer matches are more specific
            
            if (sortedMatches.isNotEmpty()) {
                val (key, days) = sortedMatches.first()
                val tip = findStorageTip(key)
                return CategoryShelfLife(days, formatCategoryName(key), tip)
            }
        }
        
        // Default fallback
        return CategoryShelfLife(7, "Other", storageTips["other"])
    }
    
    fun calculateExpiryDate(daysToAdd: Int): LocalDate {
        return LocalDate.now().plusDays(daysToAdd.toLong())
    }
    
    private fun findStorageTip(category: String): String? {
        // Check for exact match first
        storageTips[category]?.let { return it }
        
        // Check for partial matches
        for ((key, tip) in storageTips) {
            if (category.contains(key) || key.contains(category)) {
                return tip
            }
        }
        
        return null
    }
    
    private fun formatCategoryName(category: String): String {
        return category.split(" ", "-", "_")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }
    
    fun parseCategories(categoriesString: String?): List<String> {
        if (categoriesString.isNullOrBlank()) return emptyList()
        return categoriesString.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
    }
    
    /**
     * Get all supported categories for reference
     */
    fun getSupportedCategories(): List<String> {
        return categoryShelfLifeMap.keys.sorted()
    }
    
    /**
     * Check if a specific category is supported
     */
    fun isCategorySupported(category: String): Boolean {
        val lower = category.lowercase()
        return categoryShelfLifeMap.containsKey(lower) ||
               categoryShelfLifeMap.keys.any { it.contains(lower) || lower.contains(it) }
    }
}