# Recipe Exporter — Мод для экспорта всех рецептов Minecraft 1.12.2

## Описание

Этот мод собирает **все рецепты** из игры (ванильные + модовые) и сохраняет их в JSON-файл.
Работает через Forge API, без декомпиляции.

## Возможности

- ✅ Экспорт всех рецептов крафта (Shaped, Shapeless, OreDictionary)
- ✅ Экспорт рецептов плавки (Furnace)
- ✅ Поддержка модовых рецептов (IC2, OpenComputers и др.)
- ✅ Команда `/exportrecipes` для экспорта в игре
- ✅ Подробная информация: предметы, количество, NBT, опыт плавки

## Установка

### 1. Сборка мода

```powershell
cd C:\fpga\mainbots\RecipeExporter

# Инициализация ForgeGradle
.\gradlew.bat setupDecompWorkspace

# Сборка мода
.\gradlew.bat build
```

Готовый JAR будет в: `build/libs/RecipeExporter-1.0.jar`

### 2. Установка в Minecraft

1. Скопируйте `RecipeExporter-1.0.jar` в папку `mods` вашего Forge-клиента
2. Запустите Minecraft с Forge 1.12.2

## Использование

### В игре

```
# Экспорт всех рецептов
/exportrecipes

# Экспорт с именем файла
/exportrecipes my_recipes
```

Файл сохранится в:
```
.minecraft/recipe_exporter/all_recipes.json
```

### Через консоль сервера

```
/exportrecipes server_export
```

## Формат JSON

```json
{
    "mod": "Recipe Exporter",
    "version": "1.0.0",
    "minecraft_version": "1.12.2",
    "total_recipes": 1234,
    "recipes": [
        {
            "group": "",
            "output": {
                "item": "minecraft:diamond",
                "registryName": "minecraft:diamond",
                "count": 1,
                "meta": 0
            },
            "ingredients": [
                {"item": "minecraft:iron_ingot", "count": 1, "meta": 0},
                {"item": "minecraft:iron_ingot", "count": 1, "meta": 0}
            ],
            "pattern": [
                "III",
                "III",
                "III"
            ],
            "key": {
                "I": [
                    {"item": "minecraft:iron_ingot", "count": 1, "meta": 0}
                ]
            }
        },
        {
            "type": "smelting",
            "input": {
                "item": "minecraft:iron_ore",
                "count": 1,
                "meta": 0
            },
            "output": {
                "item": "minecraft:iron_ingot",
                "count": 1,
                "meta": 0
            },
            "experience": 0.7
        }
    ]
}
```

## Структура проекта

```
RecipeExporter/
├── build.gradle              # Конфиг Gradle + ForgeGradle 2.3
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── recipeexporter/
│       │               └── RecipeExporter.java  # Основной код мода
│       └── resources/
│           └── mcmod.info   # Метаданные мода
└── README.md
```

## Технические детали

### Как работает сбор рецептов

1. **При инициализации мода** (`FMLInitializationEvent`):
   - Получаем `IForgeRegistry<IRecipe>` — все зарегистрированные рецепты
   - Проходим по каждому рецепту через `registry.getValuesCollection()`
   - Используем рефлексию для извлечения ингредиентов и паттернов

2. **Для каждого рецепта извлекаем**:
   - Выходящий предмет (`IRecipe.getRecipeOutput()`)
   - Ингредиенты (`getIngredientStacks()` / reflection)
   - Паттерн крафта (`getRecipePattern()` для shaped)
   - Ключ символов (`getRecipeItems()` для shaped)
   - Остаточные предметы (`getRemainingItems()`)

3. **Для плавки**:
   - Используем `FurnaceRecipes.getInstance()`
   - Получаем `smeltingList` через рефлексию
   - Добавляем опыт плавки

### Поддерживаемые типы рецептов

| Тип | Источник | Статус |
|---|---|---|
| Vanilla Shaped | `ShapedRecipes` | ✅ |
| Vanilla Shapeless | `ShapelessRecipes` | ✅ |
| OreDictionary Shaped | `ShapedOreRecipe` | ✅ |
| OreDictionary Shapeless | `ShapelessOreRecipe` | ✅ |
| Smelting | `FurnaceRecipes` | ✅ |
| Modded (Forge IRecipe) | Любой мод | ✅ |
| IC2 Machine Recipes | `ic2.core.recipe` | ✅ |
| Custom Mod Recipes | Любой мод | ✅ |

## Возможные проблемы

| Проблема | Решение |
|---|---|
| Рецепт не экспортируется | Проверьте консоль на ошибки |
| Неполные ингредиенты | Мод использует рефлексию для извлечения |
| Parsed recipes < expected | Некоторые моды используют кастомные форматы |
| Gradle error | Убедитесь, что Java 8 установлена |

## Расширение

Для добавления поддержки специфических модов (IC2, Thermal Expansion и др.):

```java
// Добавьте в collectAllRecipes():
if (recipe.getClass().getName().contains("ic2")) {
    extractIC2RecipeData(recipe, data);
}

private void extractIC2RecipeData(IRecipe recipe, Map<String, Object> data) {
    try {
        // IC2 использует свои классы рецептов
        Class<?> recipeClass = Class.forName("ic2.core.recipe.Recipe");
        // ... извлечение данных
    } catch (Exception e) {
        // IC2 не установлен
    }
}
```

## Анализ экспортированных рецептов

После экспорта используйте Python-скрипт для анализа:

```powershell
# Сводка по всем рецептам
python parse_recipes.py all_recipes.json --summary

# Только рецепты плавки
python parse_recipes.py all_recipes.json --type smelting

# Рецепты с алмазами
python parse_recipes.py all_recipes.json --item minecraft:diamond

# Экспорт в CSV
python parse_recipes.py all_recipes.json --output recipes.csv
```

## Лицензия

MIT — свободно для использования и модификации.
