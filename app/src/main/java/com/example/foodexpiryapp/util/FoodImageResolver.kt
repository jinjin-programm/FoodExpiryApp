package com.example.foodexpiryapp.util

import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.domain.model.FoodCategory

object FoodImageResolver {

    private val FOOD_DRAWABLE_MAP: Map<String, Int> = mapOf(
        "apple" to R.drawable.food_apple,
        "banana" to R.drawable.food_banana,
        "beans" to R.drawable.food_beans,
        "cake" to R.drawable.food_cake,
        "candy" to R.drawable.food_candy,
        "cereal" to R.drawable.food_cereal,
        "chilli" to R.drawable.food_chilli,
        "chips" to R.drawable.food_chips,
        "chocolate" to R.drawable.food_chocolate,
        "coffee" to R.drawable.food_coffee,
        "corn" to R.drawable.food_corn,
        "fish" to R.drawable.food_fish,
        "flour" to R.drawable.food_flour,
        "grape" to R.drawable.food_grape,
        "honey" to R.drawable.food_honey,
        "jam" to R.drawable.food_jam,
        "juice" to R.drawable.food_juice,
        "lemon" to R.drawable.food_lemon,
        "milk" to R.drawable.food_milk,
        "nuts" to R.drawable.food_nuts,
        "oil" to R.drawable.food_oil,
        "orange" to R.drawable.food_orange,
        "pasta" to R.drawable.food_pasta,
        "pineapple" to R.drawable.food_pineapple,
        "rice" to R.drawable.food_rice,
        "soda" to R.drawable.food_soda,
        "spices" to R.drawable.food_spices,
        "sugar" to R.drawable.food_sugar,
        "tea" to R.drawable.food_tea,
        "tomato" to R.drawable.food_tomato,
        "tomato sauce" to R.drawable.food_tomato_sauce,
        "tomato_sauce" to R.drawable.food_tomato_sauce,
        "vinegar" to R.drawable.food_vinegar,
        "water" to R.drawable.food_water,
        "watermelon" to R.drawable.food_watermelon,
        "bread" to R.drawable.food_bread,
        "egg" to R.drawable.food_egg,
        "eggs" to R.drawable.food_egg,
        "cheese" to R.drawable.food_cheese,
        "butter" to R.drawable.food_butter,
        "yogurt" to R.drawable.food_yogurt,
        "chicken" to R.drawable.food_chicken,
        "beef" to R.drawable.food_beef,
        "pork" to R.drawable.food_pork,
        "salmon" to R.drawable.food_salmon,
        "shrimp" to R.drawable.food_shrimp,
        "carrot" to R.drawable.food_carrot,
        "carrots" to R.drawable.food_carrot,
        "potato" to R.drawable.food_potato,
        "potatoes" to R.drawable.food_potato,
        "onion" to R.drawable.food_onion,
        "onions" to R.drawable.food_onion,
        "garlic" to R.drawable.food_garlic,
        "broccoli" to R.drawable.food_broccoli,
        "spinach" to R.drawable.food_spinach,
        "lettuce" to R.drawable.food_lettuce,
        "cucumber" to R.drawable.food_cucumber,
        "mushroom" to R.drawable.food_mushroom,
        "mushrooms" to R.drawable.food_mushroom,
        "pepper" to R.drawable.food_pepper,
        "peppers" to R.drawable.food_pepper,
        "bell pepper" to R.drawable.food_pepper,
        "strawberry" to R.drawable.food_strawberry,
        "strawberries" to R.drawable.food_strawberry,
        "blueberry" to R.drawable.food_blueberry,
        "blueberries" to R.drawable.food_blueberry,
        "mango" to R.drawable.food_mango,
        "kiwi" to R.drawable.food_kiwi,
        "avocado" to R.drawable.food_avocado,
        "pear" to R.drawable.food_pear,
        "peach" to R.drawable.food_peach,
        "pizza" to R.drawable.food_pizza,
        "burger" to R.drawable.food_burger,
        "hamburger" to R.drawable.food_burger,
        "sandwich" to R.drawable.food_sandwich,
        "soup" to R.drawable.food_soup,
        "salad" to R.drawable.food_salad,
        "tofu" to R.drawable.food_tofu,
        "cream" to R.drawable.food_cream,
        "ice cream" to R.drawable.food_ice_cream,
        "ice_cream" to R.drawable.food_ice_cream,
        "cookie" to R.drawable.food_cookie,
        "cookies" to R.drawable.food_cookie,
        "donut" to R.drawable.food_donut,
        "doughnut" to R.drawable.food_donut,
        "pretzel" to R.drawable.food_pretzel,
        "popcorn" to R.drawable.food_popcorn,
        "cracker" to R.drawable.food_cracker,
        "crackers" to R.drawable.food_cracker,
        "ketchup" to R.drawable.food_ketchup,
        "mustard" to R.drawable.food_mustard,
        "mayonnaise" to R.drawable.food_mayonnaise,
        "mayo" to R.drawable.food_mayonnaise,
        "soy sauce" to R.drawable.food_soy_sauce,
        "soy_sauce" to R.drawable.food_soy_sauce,
        "peanut butter" to R.drawable.food_peanut_butter,
        "peanut_butter" to R.drawable.food_peanut_butter,
        "oatmeal" to R.drawable.food_oatmeal,
        "pancake" to R.drawable.food_pancake,
        "pancakes" to R.drawable.food_pancake,
        "bagel" to R.drawable.food_bagel,
        "croissant" to R.drawable.food_croissant,
        "muffin" to R.drawable.food_muffin,
        "tortilla" to R.drawable.food_tortilla,
        "cottage cheese" to R.drawable.food_cottage_cheese,
        "cottage_cheese" to R.drawable.food_cottage_cheese,
        "cream cheese" to R.drawable.food_cream_cheese,
        "cream_cheese" to R.drawable.food_cream_cheese,
        "coconut" to R.drawable.food_coconut,
        "pomegranate" to R.drawable.food_pomegranate,
        "cherry" to R.drawable.food_cherry,
        "cherries" to R.drawable.food_cherry,
        "plum" to R.drawable.food_plum,
        "celery" to R.drawable.food_celery,
        "zucchini" to R.drawable.food_zucchini,
        "eggplant" to R.drawable.food_eggplant,
        "ginger" to R.drawable.food_ginger,
        "bacon" to R.drawable.food_bacon,
        "sausage" to R.drawable.food_sausage,
        "ham" to R.drawable.food_ham,
        "turkey" to R.drawable.food_turkey,
    )

    private val CATEGORY_FALLBACK: Map<FoodCategory, Int> = mapOf(
        FoodCategory.DAIRY to R.drawable.cat_dairy,
        FoodCategory.MEAT to R.drawable.cat_meat,
        FoodCategory.VEGETABLES to R.drawable.cat_vegetables,
        FoodCategory.FRUITS to R.drawable.cat_fruits,
        FoodCategory.GRAINS to R.drawable.cat_grains,
        FoodCategory.BEVERAGES to R.drawable.cat_beverages,
        FoodCategory.SNACKS to R.drawable.cat_snacks,
        FoodCategory.CONDIMENTS to R.drawable.cat_condiments,
        FoodCategory.FROZEN to R.drawable.cat_frozen,
        FoodCategory.LEFTOVERS to R.drawable.cat_leftovers,
        FoodCategory.OTHER to R.drawable.cat_other,
    )

    fun getFoodImage(foodName: String, category: FoodCategory): Int {
        val normalizedName = foodName.trim().lowercase()
        FOOD_DRAWABLE_MAP[normalizedName]?.let { return it }

        for ((key, drawable) in FOOD_DRAWABLE_MAP) {
            if (normalizedName.contains(key) || key.contains(normalizedName)) {
                return drawable
            }
        }

        return CATEGORY_FALLBACK[category] ?: R.drawable.cat_other
    }

    fun hasSpecificImage(foodName: String): Boolean {
        val normalizedName = foodName.trim().lowercase()
        if (FOOD_DRAWABLE_MAP.containsKey(normalizedName)) return true
        for (key in FOOD_DRAWABLE_MAP.keys) {
            if (normalizedName.contains(key) || key.contains(normalizedName)) return true
        }
        return false
    }
}
