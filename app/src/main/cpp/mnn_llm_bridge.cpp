#include <jni.h>
#include <android/log.h>
#include <string>
#include <sstream>
#include <fstream>
#include <memory>
#include <mutex>

#include "llm/llm.hpp"

#define TAG "MnnLlmBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

using MNN::Transformer::Llm;
using MNN::Transformer::ChatMessage;
using MNN::Transformer::ChatMessages;
using MNN::Transformer::MultimodalPrompt;

struct LlmInstance {
    std::unique_ptr<Llm> llm;
    std::string last_response;
    std::string few_shot_examples;
    bool is_loaded = false;
};

static std::mutex g_llm_mutex;

static std::string stripThinkingProcess(const std::string& text);
static std::string extractJson(const std::string& text);
static std::string escapeJson(const std::string& text);

static std::string buildFewShotExamples() {
    std::string examples =
        "A large, round/oval fruit with green rind and dark green stripes output:{\"name_en\":\"Watermelon\"}\n"
        "A medium, round fruit with smooth red skin and a green stem output:{\"name_en\":\"Tomato\"}\n"
        "A curved, elongated yellow fruit with a thick peel that browns when ripe output:{\"name_en\":\"Banana\"}\n"
        "A round fruit with orange skin, slightly dimpled texture output:{\"name_en\":\"Orange\"}\n"
        "A round fruit with bright red skin, small green leaves on top output:{\"name_en\":\"Apple\"}\n"
        "A small, round fruit with dark purple/black skin and a tiny crown output:{\"name_en\":\"Blueberry\"}\n"
        "A heart-shaped red fruit with tiny yellow seeds on the surface output:{\"name_en\":\"Strawberry\"}\n"
        "A cluster of small, round berries in red, green, or purple on a stem output:{\"name_en\":\"Grapes\"}\n"
        "A pear-shaped fruit with green or yellow skin, smooth texture output:{\"name_en\":\"Pear\"}\n"
        "A fuzzy brown oval fruit with bright green flesh inside output:{\"name_en\":\"Kiwi\"}\n"
        "A round citrus with thick yellow rind, slightly bumpy output:{\"name_en\":\"Lemon\"}\n"
        "A small, round citrus with green rind and sour taste output:{\"name_en\":\"Lime\"}\n"
        "A large, spiky oval fruit with a crown of sharp leaves on top output:{\"name_en\":\"Pineapple\"}\n"
        "A round fruit with orange-brown fuzzy skin and a large pit inside output:{\"name_en\":\"Peach\"}\n"
        "A smooth-skinned round fruit with purple/red skin and a crease output:{\"name_en\":\"Plum\"}\n"
        "A small, round red fruit with a single large pit and a thin stem output:{\"name_en\":\"Cherry\"}\n"
        "A round fruit with orange-yellow flesh and a netted tan rind output:{\"name_en\":\"Cantaloupe\"}\n"
        "A round fruit with green-yellow skin and sweet orange flesh output:{\"name_en\":\"Mango\"}\n"
        "A brown, hard-shelled spherical fruit with white flesh and water inside output:{\"name_en\":\"Coconut\"}\n"
        "A small, red berry with a hollow center and a green cap output:{\"name_en\":\"Raspberry\"}\n"
        "A dark purple/black aggregate berry with tiny drupelets output:{\"name_en\":\"Blackberry\"}\n"
        "A long, slender green vegetable with bumpy skin and seeds inside output:{\"name_en\":\"Cucumber\"}\n"
        "A long, orange root vegetable with green leafy tops output:{\"name_en\":\"Carrot\"}\n"
        "A round, layered bulb with papery brown skin and white/purple flesh output:{\"name_en\":\"Onion\"}\n"
        "A small, round white bulb with a milder flavor, green leaves attached output:{\"name_en\":\"Shallot\"}\n"
        "A cluster of cloves wrapped in papery white/purple skin output:{\"name_en\":\"Garlic\"}\n"
        "A tight cluster of green florets on thick stalks output:{\"name_en\":\"Broccoli\"}\n"
        "A white, dense head of undeveloped florets surrounded by green leaves output:{\"name_en\":\"Cauliflower\"}\n"
        "Small, round green spheres growing in pods or loose output:{\"name_en\":\"Peas\"}\n"
        "Long, thin green pods with small seeds inside output:{\"name_en\":\"Green Beans\"}\n"
        "A yellow or white kernel-covered cylindrical cob wrapped in green husks output:{\"name_en\":\"Corn\"}\n"
        "A large, orange spherical vegetable often carved for Halloween output:{\"name_en\":\"Pumpkin\"}\n"
        "A long, curved green or yellow squash with soft skin output:{\"name_en\":\"Zucchini\"}\n"
        "A bell-shaped vegetable with a blocky shape, available in green, red, yellow output:{\"name_en\":\"Bell Pepper\"}\n"
        "A long, wrinkled green chili pepper with mild to medium heat output:{\"name_en\":\"Anaheim Pepper\"}\n"
        "A small, bright red chili pepper, very hot, pointed tip output:{\"name_en\":\"Bird's Eye Chili\"}\n"
        "A large, green leafy vegetable with thick white stalks and dark leaves output:{\"name_en\":\"Bok Choy\"}\n"
        "A round or oval brown-skinned tuber with starchy white flesh output:{\"name_en\":\"Potato\"}\n"
        "A long, cylindrical orange sweet potato with reddish-brown skin output:{\"name_en\":\"Sweet Potato\"}\n"
        "A brown, hairy root vegetable with a strong earthy aroma output:{\"name_en\":\"Yam\"}\n"
        "A white, round radish with a crisp texture and peppery taste output:{\"name_en\":\"Daikon\"}\n"
        "A small, round red radish with a white tip and green leaves output:{\"name_en\":\"Radish\"}\n"
        "A purple, egg-shaped vegetable with a green cap and spongy flesh output:{\"name_en\":\"Eggplant\"}\n"
        "A green, oval vegetable with a dense texture and mild flavor output:{\"name_en\":\"Chayote\"}\n"
        "A cluster of thick, white spear-shaped stalks with a purple tip output:{\"name_en\":\"Asparagus\"}\n"
        "A round, artichoke-like bud with tightly packed green leaves output:{\"name_en\":\"Artichoke\"}\n"
        "A leafy green with thick, crinkled leaves and a strong flavor output:{\"name_en\":\"Kale\"}\n"
        "A round, compact head of pale green leaves with a crisp texture output:{\"name_en\":\"Cabbage\"}\n"
        "A small, round green cabbage with a sweeter taste output:{\"name_en\":\"Brussels Sprouts\"}\n"
        "A dark green leafy vegetable with a slightly bitter taste, used in salads output:{\"name_en\":\"Arugula\"}\n"
        "A long, thin green onion with a small white bulb and hollow green tops output:{\"name_en\":\"Scallion\"}\n"
        "A flat, oval bean with a green pod and edible seeds output:{\"name_en\":\"Fava Bean\"}\n"
        "A white, spongy fungus with a delicate flavor, shaped like a cloud output:{\"name_en\":\"Snow Fungus\"}\n"
        "A brown, umbrella-shaped fungus with a meaty texture output:{\"name_en\":\"Shiitake Mushroom\"}\n"
        "A small, white button-shaped mushroom with a mild flavor output:{\"name_en\":\"White Mushroom\"}\n"
        "A large, flat brown mushroom with a rich flavor output:{\"name_en\":\"Portobello Mushroom\"}\n"
        "A golden, needle-like fungus often used in soups output:{\"name_en\":\"Enoki Mushroom\"}\n"
        "A white, thick-stemmed mushroom with a delicate flavor output:{\"name_en\":\"King Oyster Mushroom\"}\n"
        "A dark brown, wrinkled fungus with a strong umami taste output:{\"name_en\":\"Wood Ear Mushroom\"}\n"
        "A flat, round patty of raw ground beef output:{\"name_en\":\"Ground Beef\"}\n"
        "A whole, raw chicken breast with skin and bone output:{\"name_en\":\"Chicken Breast\"}\n"
        "A raw chicken leg quarter with skin and bone output:{\"name_en\":\"Chicken Leg\"}\n"
        "A whole raw chicken wing with skin and bone output:{\"name_en\":\"Chicken Wing\"}\n"
        "A whole raw chicken, plucked and cleaned output:{\"name_en\":\"Whole Chicken\"}\n"
        "A thick, boneless cut of raw beef steak with marbling output:{\"name_en\":\"Ribeye Steak\"}\n"
        "A lean, boneless cut of raw beef with a tender texture output:{\"name_en\":\"Beef Tenderloin\"}\n"
        "A cut of raw beef with a T-shaped bone and meat on both sides output:{\"name_en\":\"T-Bone Steak\"}\n"
        "A slab of raw pork ribs with bones and meat output:{\"name_en\":\"Pork Ribs\"}\n"
        "A raw, thick cut of pork loin with a layer of fat output:{\"name_en\":\"Pork Chop\"}\n"
        "A raw, boneless cut of pork with a pale pink color output:{\"name_en\":\"Pork Tenderloin\"}\n"
        "A whole raw fish with silver scales and clear eyes output:{\"name_en\":\"Whole Fish\"}\n"
        "A raw fish fillet with orange-pink flesh and white stripes output:{\"name_en\":\"Salmon Fillet\"}\n"
        "A raw fish fillet with white flesh and a mild flavor output:{\"name_en\":\"Cod Fillet\"}\n"
        "A raw, flat oval-shaped fish with both eyes on one side output:{\"name_en\":\"Flounder\"}\n"
        "A raw, silver-skinned fish with a strong ocean smell output:{\"name_en\":\"Mackerel\"}\n"
        "A raw, pinkish fish with a slender body and small scales output:{\"name_en\":\"Trout\"}\n"
        "A raw shellfish with a hard, dark blue-black hinged shell output:{\"name_en\":\"Mussel\"}\n"
        "A raw shellfish with a rough, grey hinged shell and a white interior output:{\"name_en\":\"Oyster\"}\n"
        "A raw shellfish with a fan-shaped, ridged shell in various colors output:{\"name_en\":\"Scallop\"}\n"
        "A raw crustacean with a hard, segmented shell, large claws, and long antennae output:{\"name_en\":\"Lobster\"}\n"
        "A raw crustacean with a curved body, hard shell, and ten legs output:{\"name_en\":\"Shrimp\"}\n"
        "A raw crustacean with a hard shell, claws, and a broad body output:{\"name_en\":\"Crab\"}\n"
        "A raw mollusk with a spiral, conical shell output:{\"name_en\":\"Conch\"}\n"
        "A raw cephalopod with a torpedo-shaped body, fins, and tentacles output:{\"name_en\":\"Squid\"}\n"
        "A raw cephalopod with eight tentacles and a bulbous head output:{\"name_en\":\"Octopus\"}\n"
        "A white liquid in a clear glass or plastic bottle output:{\"name_en\":\"Milk\"}\n"
        "A yellow block with a smooth, waxy texture, sometimes wrapped in foil output:{\"name_en\":\"Butter\"}\n"
        "A firm, pale yellow block with a smooth or crumbly texture output:{\"name_en\":\"Cheese\"}\n"
        "A white, creamy substance in a small plastic cup with a foil lid output:{\"name_en\":\"Yogurt\"}\n"
        "A white, thick liquid poured from a carton, sometimes with fruit pieces output:{\"name_en\":\"Smoothie\"}\n"
        "A glass of orange-yellow liquid with pulp visible output:{\"name_en\":\"Orange Juice\"}\n"
        "A brown, fizzy liquid in a can or bottle output:{\"name_en\":\"Cola\"}\n"
        "A clear, carbonated liquid in a green glass bottle with bubbles output:{\"name_en\":\"Sparkling Water\"}\n"
        "A small cup of dark brown liquid with a crema on top output:{\"name_en\":\"Espresso\"}\n"
        "A tall glass of dark brown liquid with ice cubes output:{\"name_en\":\"Iced Coffee\"}\n"
        "A cup of greenish-yellow liquid with steam rising output:{\"name_en\":\"Green Tea\"}\n"
        "A cup of dark brown liquid with a milky swirl output:{\"name_en\":\"Milk Tea\"}\n"
        "A slice of white bread with a soft, airy texture output:{\"name_en\":\"White Bread\"}\n"
        "A slice of brown bread with visible grains and seeds output:{\"name_en\":\"Whole Wheat Bread\"}\n"
        "A round, flat bread with a pocket inside, lightly toasted output:{\"name_en\":\"Pita Bread\"}\n"
        "A long, thin, cylindrical pasta made from wheat output:{\"name_en\":\"Spaghetti\"}\n"
        "A small, tube-shaped pasta with ridges output:{\"name_en\":\"Penne Pasta\"}\n"
        "A flat, wide ribbon pasta with ruffled edges output:{\"name_en\":\"Lasagna Noodle\"}\n"
        "A small, bow-tie shaped pasta output:{\"name_en\":\"Farfalle\"}\n"
        "A small, shell-shaped pasta with ridges output:{\"name_en\":\"Conchiglie\"}\n"
        "A white, fluffy grain cooked in a bowl, sticky or separate output:{\"name_en\":\"Rice\"}\n"
        "A small, round golden grain cooked in a bowl, chewy texture output:{\"name_en\":\"Quinoa\"}\n"
        "A bowl of dry, flake-shaped cereal pieces in various colors output:{\"name_en\":\"Breakfast Cereal\"}\n"
        "A bowl of cooked, creamy beige porridge output:{\"name_en\":\"Oatmeal\"}\n"
        "A hard-boiled egg with a white shell, sometimes peeled revealing white and yellow output:{\"name_en\":\"Boiled Egg\"}\n"
        "A fried egg with a runny yellow yolk and crispy white edges output:{\"name_en\":\"Fried Egg\"}\n"
        "A scrambled yellow egg mixture, fluffy and soft output:{\"name_en\":\"Scrambled Eggs\"}\n"
        "A folded egg omelet with fillings visible, golden brown output:{\"name_en\":\"Omelette\"}\n"
        "A thin, round cake made from batter, golden brown, often stacked output:{\"name_en\":\"Pancake\"}\n"
        "A golden brown, grid-patterned round pastry with deep indentations output:{\"name_en\":\"Waffle\"}\n"
        "A slice of crispy, cured pork belly with streaks of fat output:{\"name_en\":\"Bacon\"}\n"
        "A small, cylindrical sausage made of ground meat in a casing output:{\"name_en\":\"Sausage\"}\n"
        "A slice of processed ham, pink and smooth output:{\"name_en\":\"Ham\"}\n"
        "A thin slice of cured salmon, orange with white fat lines output:{\"name_en\":\"Smoked Salmon\"}\n"
        "A round, flat patty of cooked ground meat in a bun output:{\"name_en\":\"Hamburger\"}\n"
        "A long, filled bread roll with meat, vegetables, and sauce output:{\"name_en\":\"Sub Sandwich\"}\n"
        "A slice of bread topped with tomato sauce, cheese, and various toppings output:{\"name_en\":\"Pizza\"}\n"
        "A triangular or cylindrical roll of rice and seaweed with fillings output:{\"name_en\":\"Sushi Roll\"}\n"
        "A piece of raw fish draped over a ball of vinegared rice output:{\"name_en\":\"Nigiri Sushi\"}\n"
        "A thin slice of raw fish served without rice output:{\"name_en\":\"Sashimi\"}\n"
        "A bowl of white rice topped with assorted raw fish and vegetables output:{\"name_en\":\"Poke Bowl\"}\n"
        "A deep-fried roll filled with ground meat and vegetables, golden brown output:{\"name_en\":\"Spring Roll\"}\n"
        "A crescent-shaped, deep-fried pastry filled with spiced meat or vegetables output:{\"name_en\":\"Samosa\"}\n"
        "A folded tortilla filled with meat, cheese, and vegetables, lightly browned output:{\"name_en\":\"Quesadilla\"}\n"
        "A rolled tortilla filled with meat, beans, and rice, covered in sauce output:{\"name_en\":\"Burrito\"}\n"
        "A folded corn tortilla filled with meat, onion, and cilantro output:{\"name_en\":\"Taco\"}\n"
        "A bowl of crispy tortilla chips topped with melted cheese and toppings output:{\"name_en\":\"Nachos\"}\n"
        "A bowl of creamy, mashed avocado with lime and onion output:{\"name_en\":\"Guacamole\"}\n"
        "A red, chunky dip made from tomatoes, onions, and chili output:{\"name_en\":\"Salsa\"}\n"
        "A bowl of blended chickpeas with tahini, garlic, and lemon output:{\"name_en\":\"Hummus\"}\n"
        "A deep-fried ball of ground chickpeas, brown and crispy output:{\"name_en\":\"Falafel\"}\n"
        "A skewer of grilled meat and vegetables, charred edges output:{\"name_en\":\"Kebab\"}\n"
        "A plate of grilled lamb chops with bone and char marks output:{\"name_en\":\"Lamb Chops\"}\n"
        "A whole roasted chicken with golden brown skin and herbs output:{\"name_en\":\"Roast Chicken\"}\n"
        "A slice of roasted beef with a pink center and browned crust output:{\"name_en\":\"Roast Beef\"}\n"
        "A bowl of noodles in broth with sliced meat and green onions output:{\"name_en\":\"Ramen\"}\n"
        "A bowl of wheat noodles in broth with pork slices and seaweed output:{\"name_en\":\"Udon\"}\n"
        "A bowl of thin rice noodles in clear broth with herbs output:{\"name_en\":\"Pho\"}\n"
        "A plate of stir-fried wide rice noodles with soy sauce and egg output:{\"name_en\":\"Pad See Ew\"}\n"
        "A plate of stir-fried thin rice noodles with tamarind sauce and peanuts output:{\"name_en\":\"Pad Thai\"}\n"
        "A plate of stir-fried vegetables and meat with a glossy brown sauce output:{\"name_en\":\"Stir-Fry\"}\n"
        "A white, cubed soybean curd in a light broth, soft and jiggly output:{\"name_en\":\"Tofu\"}\n"
        "A golden-brown, deep-fried tofu with a crispy skin output:{\"name_en\":\"Fried Tofu\"}\n"
        "A sheet of dried seaweed, dark green/black, crisp and thin output:{\"name_en\":\"Nori\"}\n"
        "A green, fuzzy-skinned fruit with black seeds and tangy green flesh output:{\"name_en\":\"Kiwano\"}\n"
        "A reddish-purple fruit with a crown-like top and white, sweet flesh inside output:{\"name_en\":\"Dragon Fruit\"}\n"
        "A small, oval fruit with brown, rough skin and sweet, grainy flesh output:{\"name_en\":\"Sapodilla\"}\n"
        "A green, prickly-skinned fruit with a strong, pungent aroma and creamy yellow flesh output:{\"name_en\":\"Durian\"}\n"
        "A large, green or yellow fruit with a spiky exterior and sweet, fibrous flesh output:{\"name_en\":\"Jackfruit\"}\n"
        "A round, dark purple fruit with a hard shell and a clove of white flesh inside output:{\"name_en\":\"Mangosteen\"}\n"
        "A small, yellow-green fruit with a tart flavor and smooth skin output:{\"name_en\":\"Star Fruit\"}\n"
        "A brown, round fruit with a translucent, gelatinous flesh and a dark seed output:{\"name_en\":\"Lychee\"}\n"
        "A small, fuzzy brown fruit with a translucent, sweet flesh similar to lychee output:{\"name_en\":\"Rambutan\"}\n"
        "A cluster of dark red, scaly fruits with white, sweet flesh and a large seed output:{\"name_en\":\"Longan\"}\n"
        "A brown, teardrop-shaped pod containing a sweet, sticky pulp and seeds output:{\"name_en\":\"Tamarind\"}\n"
        "A wrinkled, yellow citrus fruit with a distinct, sour taste and bumpy rind output:{\"name_en\":\"Yuzu\"}\n"
        "A small, round orange fruit with a loose, easy-to-peel skin output:{\"name_en\":\"Mandarin Orange\"}\n"
        "A bright orange fruit with a dimpled skin, smaller than an orange output:{\"name_en\":\"Clementine\"}\n"
        "A large, yellow citrus with a thick rind and mildly sweet flavor output:{\"name_en\":\"Pomelo\"}\n"
        "A dark green, wrinkled fruit with a tart, astringent flesh output:{\"name_en\":\"Soursop\"}\n"
        "A red, prickly pear-shaped cactus fruit with green flesh and black seeds output:{\"name_en\":\"Prickly Pear\"}\n"
        "A small, red berry with a tart taste, often in clusters output:{\"name_en\":\"Cranberry\"}\n"
        "A black, shiny berry with a slightly tart flavor output:{\"name_en\":\"Black Currant\"}\n"
        "A red, translucent berry with a sour taste, used in pies output:{\"name_en\":\"Red Currant\"}\n"
        "A yellow-green berry with a papery husk and sweet-tart flavor output:{\"name_en\":\"Gooseberry\"}\n"
        "A purple, bell-shaped flower bud with a tangy, sour taste output:{\"name_en\":\"Roselle\"}\n"
        "A long, green, ridged squash with soft flesh and edible skin output:{\"name_en\":\"Luffa\"}\n"
        "A long, thin, green bean with a round cross-section and crisp texture output:{\"name_en\":\"Yardlong Bean\"}\n"
        "A flat, broad bean pod with a velvety texture and large seeds output:{\"name_en\":\"Broad Bean\"}\n"
        "A round, flat bean with a creamy texture and nutty flavor output:{\"name_en\":\"Lima Bean\"}\n"
        "A small, green seed with a slightly sweet taste, often eaten as a snack output:{\"name_en\":\"Edamame\"}\n"
        "A brown, dried bean with a kidney shape and reddish-brown color output:{\"name_en\":\"Kidney Bean\"}\n"
        "A small, black, shiny bean with a creamy white interior output:{\"name_en\":\"Black Bean\"}\n"
        "A small, greenish-brown lentil with a peppery flavor output:{\"name_en\":\"Puy Lentil\"}\n"
        "A split yellow pea, dried and used in soups and dals output:{\"name_en\":\"Yellow Split Pea\"}\n"
        "A whole, dried chickpea with a beige color and nutty taste output:{\"name_en\":\"Chickpea\"}\n"
        "A small, white seed with a nutty flavor, used in baking output:{\"name_en\":\"Poppy Seed\"}\n"
        "A tiny, brown seed with a gelatinous coating when wet, used in chia pudding output:{\"name_en\":\"Chia Seed\"}\n"
        "A golden-brown seed with a nutty flavor, used in bagels and Asian desserts output:{\"name_en\":\"Sesame Seed\"}\n"
        "A striped, oval seed with a mild, nutty flavor output:{\"name_en\":\"Sunflower Seed\"}\n"
        "A green, teardrop-shaped seed with a crunchy texture and mild sweetness output:{\"name_en\":\"Pumpkin Seed\"}\n"
        "A brown, almond-shaped nut with a hard shell and creamy kernel output:{\"name_en\":\"Almond\"}\n"
        "A brown, wrinkled nut with a brain-like shape and rich flavor output:{\"name_en\":\"Walnut\"}\n"
        "A smooth, tan nut with a crescent shape and buttery taste output:{\"name_en\":\"Cashew\"}\n"
        "A hard, brown nut with a cap, often roasted and eaten output:{\"name_en\":\"Hazelnut\"}\n"
        "A smooth, brown nut with a slightly sweet taste output:{\"name_en\":\"Pecan\"}\n"
        "A green, oval nut with a brown, fibrous husk and a hard shell inside output:{\"name_en\":\"Pistachio\"}\n"
        "A large, brown nut with a hard shell and a creamy white kernel output:{\"name_en\":\"Brazil Nut\"}\n"
        "A round, dark brown nut with a hard shell and a bitter taste, used in cooking output:{\"name_en\":\"Chestnut\"}\n"
        "A small, round, green spice with a warm, slightly bitter taste output:{\"name_en\":\"Coriander Seed\"}\n"
        "A brown, star-shaped spice with a strong, licorice-like flavor output:{\"name_en\":\"Star Anise\"}\n"
        "A long, brown stick of bark with a sweet, warm aroma output:{\"name_en\":\"Cinnamon Stick\"}\n"
        "A small, dark brown pod containing tiny black seeds with a strong aroma output:{\"name_en\":\"Cardamom Pod\"}\n"
        "A dried flower bud with a nail-like shape and strong, pungent flavor output:{\"name_en\":\"Clove\"}\n"
        "A small, dark brown bean with a rich, complex flavor, used in chocolate output:{\"name_en\":\"Cocoa Bean\"}";
    return examples;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_foodexpiryapp_inference_mnn_MnnLlmNative_nativeCreateLlm(
        JNIEnv *env, jobject thiz, jstring modelDir, jint threadNum, jstring memoryMode) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);

    const char *dir = env->GetStringUTFChars(modelDir, nullptr);
    const char *mode = env->GetStringUTFChars(memoryMode, nullptr);
    LOGI("nativeCreateLlm: modelDir=%s, threadNum=%d, memoryMode=%s", dir, threadNum, mode);

    try {
        auto* instance = new LlmInstance();

        std::string config_path = std::string(dir) + "/config.json";
        instance->llm.reset(Llm::createLLM(config_path));
        if (!instance->llm) {
            LOGE("nativeCreateLlm: createLLM failed for %s", config_path.c_str());
            env->ReleaseStringUTFChars(modelDir, dir);
            env->ReleaseStringUTFChars(memoryMode, mode);
            delete instance;
            return 0;
        }

std::string config_json = "{";
        config_json += "\"thread_num\":" + std::to_string(threadNum) + ",";
        config_json += "\"use_mmap\":true,";
        config_json += "\"tmp_path\":\"" + std::string(dir) + "\",";
        config_json += "\"memory_mode\":\"" + std::string(mode) + "\",";

        if (std::string(mode) == "low") {
            config_json += "\"precision\":\"high\",";
        } else {
            config_json += "\"precision\":\"high\",";
        }

        // Disable Qwen3 thinking mode via jinja context (correct path per llm_demo.cpp)
        // This prevents long chain-of-thought reasoning and returns JSON directly
        config_json += "\"jinja\":{\"context\":{\"enable_thinking\":false}},";
        // Use synchronous mode for single-request inference (no streaming)
        config_json += "\"async\":false";
        config_json += "}";

        LOGI("nativeCreateLlm: config=%s", config_json.c_str());
        instance->llm->set_config(config_json);

        LOGI("nativeCreateLlm: loading model...");
        instance->is_loaded = instance->llm->load();
        if (!instance->is_loaded) {
            LOGE("nativeCreateLlm: load() failed");
            env->ReleaseStringUTFChars(modelDir, dir);
            env->ReleaseStringUTFChars(memoryMode, mode);
            delete instance;
            return 0;
        }

        instance->few_shot_examples = buildFewShotExamples();

        env->ReleaseStringUTFChars(modelDir, dir);
        env->ReleaseStringUTFChars(memoryMode, mode);

        LOGI("nativeCreateLlm: success (instance=%p, examples=%zu chars)", instance, instance->few_shot_examples.length());
        return reinterpret_cast<jlong>(instance);
    } catch (const std::exception& e) {
        LOGE("nativeCreateLlm: exception: %s", e.what());
        env->ReleaseStringUTFChars(modelDir, dir);
        env->ReleaseStringUTFChars(memoryMode, mode);
        return 0;
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_foodexpiryapp_inference_mnn_MnnLlmNative_nativeRunInference(
        JNIEnv *env, jobject thiz, jlong nativeHandle, jstring imagePath) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);

    auto* instance = reinterpret_cast<LlmInstance*>(nativeHandle);
    if (!instance || !instance->llm || !instance->is_loaded) {
        LOGE("nativeRunInference: invalid handle or model not loaded");
        return env->NewStringUTF("{\"name\":\"Error\",\"name_zh\":\"錯誤\",\"confidence\":0.0}");
    }

    const char *img_path = env->GetStringUTFChars(imagePath, nullptr);
    LOGI("nativeRunInference: imagePath=%s", img_path);

    try {
        std::string system_prompt =
            "You are a food identification assistant. Look at the image and output the specific food name.\n"
            "Describe what you see visually, then output ONLY JSON with name_en.\n\n"
            "Examples:\n" + instance->few_shot_examples;

        std::string user_content = "<img>" + std::string(img_path) + "</img>\n"
            "Describe this food visually, then output: {\"name_en\":\"SpecificFoodName\"}";

        // Build MultimodalPrompt with system+user content containing <img> tag.
        // No images map populated — fallback path will load from file path.
        MultimodalPrompt mm_prompt;
        mm_prompt.prompt_template = system_prompt + "\n\n" + user_content;

        std::stringstream response_stream;

        LOGI("nativeRunInference: using MultimodalPrompt API with <img>%s</img>", img_path);
        instance->llm->response(mm_prompt, &response_stream, "\xef\xb8\xae", 128);

        std::string response_str = response_stream.str();
        LOGI("nativeRunInference: response=%s (len=%zu)", response_str.c_str(), response_str.length());

        env->ReleaseStringUTFChars(imagePath, img_path);

        std::string json_result = extractJson(response_str);

        if (json_result.empty()) {
            json_result = "{\"name\":\"Unknown\",\"name_zh\":\"未知\",\"confidence\":0.0,\"raw_response\":\"" +
                          escapeJson(response_str) + "\"}";
        }

        instance->last_response = response_str;
        return env->NewStringUTF(json_result.c_str());
    } catch (const std::exception& e) {
        LOGE("nativeRunInference: exception: %s", e.what());
        env->ReleaseStringUTFChars(imagePath, img_path);
        return env->NewStringUTF("{\"name\":\"Error\",\"name_zh\":\"錯誤\",\"confidence\":0.0}");
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_foodexpiryapp_inference_mnn_MnnLlmNative_nativeRunInferenceWithHint(
        JNIEnv *env, jobject thiz, jlong nativeHandle, jstring imagePath, jstring hint) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);

    auto* instance = reinterpret_cast<LlmInstance*>(nativeHandle);
    if (!instance || !instance->llm || !instance->is_loaded) {
        LOGE("nativeRunInferenceWithHint: invalid handle or model not loaded");
        return env->NewStringUTF("{\"name\":\"Error\",\"name_zh\":\"錯誤\",\"confidence\":0.0}");
    }

    const char *img_path = env->GetStringUTFChars(imagePath, nullptr);
    const char *hint_str = env->GetStringUTFChars(hint, nullptr);
    LOGI("nativeRunInferenceWithHint: imagePath=%s, hint=%s", img_path, hint_str);

    try {
        std::string system_prompt =
            "You are a food identification assistant. The previous answer was too generic: \"";
        system_prompt += hint_str;
        system_prompt += "\". That is a CATEGORY, not a specific food.\n\n"
            "Look at the image MORE CAREFULLY. Describe what you see, then output the SPECIFIC food name.\n\n"
            "Examples of specific foods by category:\n"
            "Vegetables: Tomato, Carrot, Cucumber, Cabbage, Broccoli, Bell Pepper, Eggplant, Kale, Spinach, Potato, Zucchini\n"
            "Fruits: Apple, Banana, Orange, Grape, Watermelon, Mango, Strawberry, Blueberry, Peach, Cherry, Kiwi\n"
            "Meat: Chicken Breast, Ribeye Steak, Pork Chop, Ground Beef, Bacon, Sausage, Lamb Chops\n"
            "Seafood: Salmon Fillet, Shrimp, Tuna, Crab, Lobster, Squid, Cod Fillet, Mackerel\n"
            "Dairy: Milk, Butter, Cheese, Yogurt, Cream, Eggs\n"
            "Grains: Rice, Bread, Spaghetti, Oatmeal, Noodles, Quinoa, Flour Tortilla\n\n"
            "Reference examples:\n" + instance->few_shot_examples + "\n\n"
            "Now describe this image and output: {\"name_en\":\"SpecificFoodName\"}";

        std::string user_content = "<img>" + std::string(img_path) + "</img>\n"
            "What SPECIFIC food is this? Describe it visually, then output JSON.";

        MultimodalPrompt mm_prompt;
        mm_prompt.prompt_template = system_prompt + "\n\n" + user_content;

        std::stringstream response_stream;

        LOGI("nativeRunInferenceWithHint: retrying with refined prompt");
        instance->llm->response(mm_prompt, &response_stream, "\xef\xb8\xae", 128);

        std::string response_str = response_stream.str();
        LOGI("nativeRunInferenceWithHint: response=%s (len=%zu)", response_str.c_str(), response_str.length());

        env->ReleaseStringUTFChars(imagePath, img_path);
        env->ReleaseStringUTFChars(hint, hint_str);

        std::string json_result = extractJson(response_str);

        if (json_result.empty()) {
            json_result = "{\"name\":\"Unknown\",\"name_zh\":\"未知\",\"confidence\":0.0,\"raw_response\":\"" +
                          escapeJson(response_str) + "\"}";
        }

        instance->last_response = response_str;
        return env->NewStringUTF(json_result.c_str());
    } catch (const std::exception& e) {
        LOGE("nativeRunInferenceWithHint: exception: %s", e.what());
        env->ReleaseStringUTFChars(imagePath, img_path);
        env->ReleaseStringUTFChars(hint, hint_str);
        return env->NewStringUTF("{\"name\":\"Error\",\"name_zh\":\"錯誤\",\"confidence\":0.0}");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_foodexpiryapp_inference_mnn_MnnLlmNative_nativeDestroyLlm(
        JNIEnv *env, jobject thiz, jlong nativeHandle) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);

    auto* instance = reinterpret_cast<LlmInstance*>(nativeHandle);
    if (!instance) {
        LOGE("nativeDestroyLlm: invalid handle");
        return;
    }

    try {
        if (instance->llm) {
            instance->llm->reset();
        }
        LOGI("nativeDestroyLlm: destroyed instance=%p", instance);
        delete instance;
    } catch (const std::exception& e) {
        LOGE("nativeDestroyLlm: exception: %s", e.what());
        delete instance;
    }
}

static std::string extractJson(const std::string& text) {
    int brace_depth = 0;
    int start = -1;
    for (size_t i = 0; i < text.length(); i++) {
        if (text[i] == '{') {
            if (start == -1) start = (int)i;
            brace_depth++;
        } else if (text[i] == '}') {
            brace_depth--;
            if (brace_depth == 0 && start != -1) {
                return text.substr(start, i - start + 1);
            }
        }
    }

    brace_depth = 0;
    start = -1;
    for (size_t i = 0; i < text.length(); i++) {
        if (text[i] == '[') {
            if (start == -1) start = (int)i;
            brace_depth++;
        } else if (text[i] == ']') {
            brace_depth--;
            if (brace_depth == 0 && start != -1) {
                std::string inner = text.substr(start + 1, i - start - 1);
                std::string json_obj = extractJson(inner);
                if (!json_obj.empty()) return json_obj;
            }
        }
    }

    return "";
}

static std::string escapeJson(const std::string& text) {
    std::string result;
    result.reserve(text.size() + 10);
    for (char c : text) {
        switch (c) {
            case '"': result += "\\\""; break;
            case '\\': result += "\\\\"; break;
            case '\n': result += "\\n"; break;
            case '\r': result += "\\r"; break;
            case '\t': result += "\\t"; break;
            default:
                if (static_cast<unsigned char>(c) < 32) {
                    char buf[8];
                    snprintf(buf, sizeof(buf), "\\u%04x", static_cast<unsigned char>(c));
                    result += buf;
                } else {
                    result += c;
                }
        }
    }
    return result;
}
