package com.example.recipeexporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedOreRecipe;
import net.minecraft.item.crafting.ShapelessOreRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.common.crafting.IShapelessRecipe;
import net.minecraftforge.common.crafting.IRecipeComponent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Mod(
    modid = RecipeExporter.MOD_ID,
    name = RecipeExporter.MOD_NAME,
    version = RecipeExporter.MOD_VERSION,
    acceptedMinecraftVersions = "[1.12.2]"
)
public class RecipeExporter {

    public static final String MOD_ID = "recipeexporter";
    public static final String MOD_NAME = "Recipe Exporter";
    public static final String MOD_VERSION = "1.0.0";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Map<String, Object>> collectedRecipes = new ArrayList<>();
    private static boolean recipesCollected = false;

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        RecipeCollector.instance().collectAllRecipes();
        recipesCollected = true;
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new ExportRecipesCommand());
    }

    /**
     * Collects all recipes from the game registry
     */
    public static class RecipeCollector {
        private static final RecipeCollector INSTANCE = new RecipeCollector();

        public static RecipeCollector instance() {
            return INSTANCE;
        }

        public void collectAllRecipes() {
            collectedRecipes.clear();

            // Get the IRecipe registry (1.12.2 API)
            IForgeRegistry<IRecipe> registry = net.minecraftforge.fml.common.registry.GameRegistry.findRegistry(IRecipe.class);
            if (registry == null) {
                System.err.println("[RecipeExporter] IRecipe registry is null!");
                return;
            }

            for (IRecipe recipe : registry.getValuesCollection()) {
                try {
                    Map<String, Object> recipeData = extractRecipeData(recipe);
                    if (recipeData != null) {
                        collectedRecipes.add(recipeData);
                    }
                } catch (Exception e) {
                    System.err.println("[RecipeExporter] Error processing recipe: " + recipe.getRegistryName() + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Also collect smelting recipes
            collectSmeltingRecipes();

            System.out.println("[RecipeExporter] Collected " + collectedRecipes.size() + " recipes");
        }

        private Map<String, Object> extractRecipeData(IRecipe recipe) {
            Map<String, Object> data = new HashMap<>();
            
            // Get recipe group
            String group = recipe.getGroup();
            data.put("group", group != null && !group.isEmpty() ? group : null);

            // Get recipe output
            ItemStack output = recipe.getRecipeOutput();
            if (output != null) {
                Map<String, Object> outputData = extractItemStackData(output);
                data.put("output", outputData);
            }

            // Get recipe location
            ResourceLocation registryName = recipe.getRegistryName();
            if (registryName != null) {
                data.put("registryName", registryName.toString());
            }

            // Extract ingredients based on recipe type
            if (recipe.getClass().getPackage().getName().startsWith("net.minecraft")) {
                if (recipe instanceof IShapedRecipe) {
                    extractShapedRecipeData((IShapedRecipe<?>) recipe, data);
                } else if (recipe instanceof IShapelessRecipe) {
                    extractShapelessRecipeData((IShapelessRecipe) recipe, data);
                }
            } else {
                // Для модовых рецептов — базовая информация
                data.put("type", recipe.getClass().getName());
                data.put("modded", true);
            }

            // Get remaining items if available
            try {
                if (!recipe.isSimple()) {
                    return null;
                }
                Method method = recipe.getClass().getMethod("getRemainingItems");
                ItemStack[] remaining = (ItemStack[]) method.invoke(recipe);
                if (remaining != null) {
                    List<Map<String, Object>> remainingList = new ArrayList<>();
                    for (ItemStack stack : remaining) {
                        if (stack != null) {
                            remainingList.add(extractItemStackData(stack));
                        }
                    }
                    data.put("remainingItems", remainingList);
                }
            } catch (Exception e) {
                // Method might not exist in all recipe types
            }

            return data;
        }

        private void extractShapedRecipeData(IShapedRecipe<?> recipe, Map<String, Object> data) {
            List<Map<String, Object>> ingredients = new ArrayList<>();
            
            // Try to get ingredients via IRecipe interface
            try {
                Method method = recipe.getClass().getMethod("getIngredientStacks");
                ItemStack[] stacks = (ItemStack[]) method.invoke(recipe);
                if (stacks != null) {
                    for (ItemStack stack : stacks) {
                        if (stack != null) {
                            ingredients.add(extractItemStackData(stack));
                        }
                    }
                }
            } catch (Exception e) {
                // Try reflection for private fields
                extractShapedIngredientsViaReflection(recipe, ingredients);
            }

            // Get pattern and key for shaped recipes
            try {
                Method getPattern = recipe.getClass().getMethod("getRecipePattern");
                String[] pattern = (String[]) getPattern.invoke(recipe);
                if (pattern != null) {
                    data.put("pattern", pattern);
                }

                Method getKey = recipe.getClass().getMethod("getRecipeItems");
                Map<Character, List<ItemStack>> keyMap = (Map<Character, List<ItemStack>>) getKey.invoke(recipe);
                if (keyMap != null) {
                    Map<String, List<Map<String, Object>>> convertedKey = new LinkedHashMap<>();
                    for (Map.Entry<Character, List<ItemStack>> entry : keyMap.entrySet()) {
                        List<Map<String, Object>> itemStacks = new ArrayList<>();
                        for (ItemStack stack : entry.getValue()) {
                            if (stack != null) {
                                itemStacks.add(extractItemStackData(stack));
                            }
                        }
                        convertedKey.put(String.valueOf(entry.getKey()), itemStacks);
                    }
                    data.put("key", convertedKey);
                }
            } catch (Exception e) {
                // Pattern extraction failed, ingredients already collected
            }

            data.put("ingredients", ingredients);
        }

        private void extractShapedIngredientsViaReflection(IShapedRecipe<?> recipe, List<Map<String, Object>> ingredients) {
            try {
                // Try to find the key/ingredients field
                for (Field field : recipe.getClass().getDeclaredFields()) {
                    if (field.getType() == Map.class || field.getType() == LinkedHashMap.class) {
                        field.setAccessible(true);
                        Object value = field.get(recipe);
                        if (value instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<Character, List<ItemStack>> keyMap = (Map<Character, List<ItemStack>>) value;
                            for (List<ItemStack> stacks : keyMap.values()) {
                                for (ItemStack stack : stacks) {
                                    if (stack != null) {
                                        ingredients.add(extractItemStackData(stack));
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Reflection failed
            }
        }

        private void extractShapelessRecipeData(IShapelessRecipe recipe, Map<String, Object> data) {
            List<Map<String, Object>> ingredients = new ArrayList<>();
            
            try {
                Method method = recipe.getClass().getMethod("getIngredients");
                List<?> ingredientList = (List<?>) method.invoke(recipe);
                if (ingredientList != null) {
                    for (Object obj : ingredientList) {
                        if (obj instanceof net.minecraft.item.crafting.Ingredient) {
                            net.minecraft.item.crafting.Ingredient ing = (net.minecraft.item.crafting.Ingredient) obj;
                            for (ItemStack stack : ing.getMatchingStacks()) {
                                if (stack != null) {
                                    ingredients.add(extractItemStackData(stack));
                                }
                            }
                        } else if (obj instanceof ItemStack) {
                            ingredients.add(extractItemStackData((ItemStack) obj));
                        }
                    }
                }
            } catch (Exception e) {
                // Try alternative method
                try {
                    Method method = recipe.getClass().getMethod("getRecipeItems");
                    List<ItemStack> stacks = (List<ItemStack>) method.invoke(recipe);
                    if (stacks != null) {
                        for (ItemStack stack : stacks) {
                            if (stack != null) {
                                ingredients.add(extractItemStackData(stack));
                            }
                        }
                    }
                } catch (Exception ex) {
                    // Last resort: try reflection
                    extractShapelessIngredientsViaReflection(recipe, ingredients);
                }
            }

            data.put("ingredients", ingredients);
        }

        private void extractShapelessIngredientsViaReflection(IShapelessRecipe recipe, List<Map<String, Object>> ingredients) {
            try {
                for (Field field : recipe.getClass().getDeclaredFields()) {
                    if (field.getType() == List.class || field.getType() == ArrayList.class) {
                        field.setAccessible(true);
                        Object value = field.get(recipe);
                        if (value instanceof List) {
                            for (Object obj : (List<?>) value) {
                                if (obj instanceof ItemStack) {
                                    ItemStack stack = (ItemStack) obj;
                                    if (stack != null) {
                                        ingredients.add(extractItemStackData(stack));
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Reflection failed
            }
        }

        private void collectSmeltingRecipes() {
            FurnaceRecipes furnace = FurnaceRecipes.getInstance();
            
            // Get smelting list via reflection (it's private)
            try {
                Field field = FurnaceRecipes.class.getDeclaredField("smeltingList");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<ItemStack, ItemStack> smeltingList = (Map<ItemStack, ItemStack>) field.get(furnace);
                
                if (smeltingList != null) {
                    for (Map.Entry<ItemStack, ItemStack> entry : smeltingList.entrySet()) {
                        ItemStack input = entry.getKey();
                        ItemStack output = entry.getValue();
                        
                        if (input != null && output != null) {
                            Map<String, Object> smeltData = new HashMap<>();
                            smeltData.put("type", "smelting");
                            smeltData.put("input", extractItemStackData(input));
                            smeltData.put("output", extractItemStackData(output));
                            
                            // Get experience
                            try {
                                Method getXp = FurnaceRecipes.class.getMethod("getSmeltingExperience", ItemStack.class);
                                float xp = (Float) getXp.invoke(furnace, input);
                                smeltData.put("experience", xp);
                            } catch (Exception e) {
                                smeltData.put("experience", 0.0f);
                            }
                            
                            collectedRecipes.add(smeltData);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[RecipeExporter] Error collecting smelting recipes: " + e.getMessage());
            }
        }

        private Map<String, Object> extractItemStackData(ItemStack stack) {
            Map<String, Object> data = new HashMap<>();
            
            if (stack == null) return data;

            // Item name (registry name is more reliable)
            ResourceLocation registryName = net.minecraft.item.Item.REGISTRY.getNameForObject(stack.getItem());
            if (registryName != null) {
                data.put("item", registryName.toString());
                data.put("registryName", registryName.toString());
            } else {
                data.put("item", "unknown");
            }
            
            // Count
            data.put("count", stack.getCount());
            
            // Metadata/Damage value
            data.put("meta", stack.getMetadata());
            
            // NBT tags (if any)
            if (stack.getTagCompound() != null && !stack.getTagCompound().isEmpty()) {
                data.put("nbt", stack.getTagCompound().toString());
            }
            
            return data;
        }
    }

    /**
     * Command to export all collected recipes to a JSON file
     */
    public static class ExportRecipesCommand extends CommandBase {
        
        @Override
        public String getName() {
            return "exportrecipes";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/exportrecipes [filename]";
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (!recipesCollected) {
                sender.sendMessage(new TextComponentString(
                    TextFormatting.RED + "Recipes not yet collected. Please wait..."
                ));
                return;
            }

            String filename = args.length > 0 ? args[0] : "all_recipes";
            if (!filename.endsWith(".json")) {
                filename += ".json";
            }

            File outputDir = new File(Minecraft.getMinecraft().mcDataDir, "recipe_exporter");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            File outputFile = new File(outputDir, filename);

            try {
                // Add summary
                Map<String, Object> exportData = new LinkedHashMap<>();
                exportData.put("mod", MOD_NAME);
                exportData.put("version", MOD_VERSION);
                exportData.put("minecraft_version", "1.12.2");
                exportData.put("total_recipes", collectedRecipes.size());
                exportData.put("recipes", collectedRecipes);

                String json = GSON.toJson(exportData);
                
                try (FileWriter writer = new FileWriter(outputFile)) {
                    writer.write(json);
                }

                sender.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "Exported " + collectedRecipes.size() + 
                    " recipes to: " + outputFile.getAbsolutePath()
                ));

            } catch (IOException e) {
                sender.sendMessage(new TextComponentString(
                    TextFormatting.RED + "Failed to export recipes: " + e.getMessage()
                ));
            }
        }

        @Override
        public List<String> getAliases() {
            return Arrays.asList("er", "recipelist", "recipes");
        }

        @Override
        public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
            return true; // Allow all players
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }
    }
}
