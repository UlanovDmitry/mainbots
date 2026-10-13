# ForgeGradle 2.3 + CFR: Полное руководство по декомпиляции Minecraft 1.12.2

## Оглавление

1. [ForgeGradle 2.3](#1-forgegradle-23)
2. [CFR (Class File Reader)](#2-cfr-class-file-reader)
3. [Требования](#3-требования)
4. [Установка](#4-установка)
5. [Пошаговый процесс декомпиляции](#5-пошаговый-процесс-декомпиляции)
6. [Команды для извлечения рецептов](#6-конкретные-команды-для-вашей-задачи)
7. [Полезные команды Gradle](#7-полезные-команды-gradle)
8. [Типичные проблемы и решения](#8-типичные-проблемы-и-решения)

---

## 1. ForgeGradle 2.3

### Что это?

**ForgeGradle** — это Gradle-плагин, который автоматизирует весь процесс настройки среды разработки для модов Minecraft на Forge. Для 1.12.2 используется версия **2.3**.

### Ресурсы и документация

| Ресурс | URL |
|---|---|
| **GitHub FG 2.3** | https://github.com/MinecraftForge/ForgeGradle/tree/2.3 |
| **GitHub FG (текущий)** | https://github.com/MinecraftForge/ForgeGradle |
| **MCPConfig (маппинги)** | https://github.com/MinecraftForge/MCPConfig |
| **Docs (новые версии)** | https://mcforge.readthedocs.io/ |
| **Forge Files** | https://files.minecraftforge.net/ |
| **Forge Discord** | https://discord.minecraftforge.net |

> **Важно:** Документация на mcforge.readthedocs.io ориентирована на **новые** версии Forge (1.16+). Для 1.12.2 + FG 2.3 документация минимальна — основная информация в README и issues на GitHub.

---

## 2. CFR (Class File Reader)

### Что это?

CFR — Java-декомпилятор, написанный на Java. Отлично работает с Java 8 (Minecraft 1.12.2).

### Ресурсы

| Ресурс | URL |
|---|---|
| **GitHub CFR (форк)** | https://github.com/leibnizhu/cfr |
| **Оригинал CFR** | http://benf.org/other/cfr/ |
| **Скачать JAR** | https://bitbucket.org/ben/mc-repo/downloads/ |

### Установка

```powershell
# Скачайте cfr-0.152.jar (или новее)
# Положите в удобную папку, например:
C:\tools\cfr\cfr.jar
```

---

## 3. Требования

```
┌─────────────────────────────────────────────────┐
│               ТРЕБОВАНИЯ                         │
├─────────────────────────────────────────────────┤
│ • Java 8 (JDK 1.8.x) — ОБЯЗАТЕЛЬНО              │
│   • Oracle JDK 8 или OpenJDK 8                  │
│   • ForgeGradle 2.3 НЕ работает с Java 11+      │
│ • Gradle 3.x (встроен в FG через gradlew)       │
│ • Git                                             │
│ • 4+ ГБ RAM                                      │
│ • ~2-3 ГБ свободного места                       │
└─────────────────────────────────────────────────┘
```

---

## 4. Установка ForgeGradle 2.3 для 1.12.2

### Способ 1: Через готовый MDK (рекомендуется)

```powershell
# 1. Скачайте Forge MDK для 1.12.2
# Перейдите на https://files.minecraftforge.net/
# Выберите 1.12.2 → скачайте MDK

# 2. Или через Maven напрямую:
# https://maven.minecraftforge.net/net/minecraftforge/forge/1.12.2-14.23.5.2860/forge-1.12.2-14.23.5.2860-mdk.zip

# 3. Распакуйте в пустую папку
mkdir C:\fpga\mainbots\fg-workspace
# Распакуйте MDK туда
```

### Способ 2: Ручная сборка проекта

```powershell
# 1. Создайте структуру проекта
mkdir C:\fpga\mainbots\fg-project
cd C:\fpga\mainbots\fg-project

# 2. settings.gradle
# (содержимое ниже)

# 3. build.gradle
# (содержимое ниже)

# 4. gradle/wrapper/ — скопируйте из MDK

# 5. Запустите первичную инициализацию
.\gradlew.bat setupDecompWorkspace --refresh-dependencies
```

### Файл `settings.gradle` для FG 2.3

```groovy
rootProject.name = 'decompile-project'
```

### Файл `build.gradle` для 1.12.2 + FG 2.3

```groovy
buildscript {
    repositories {
        maven { url 'https://files.minecraftforge.net/maven' }
        maven { url 'https://repo.maven.apache.org/maven2' }
    }
    dependencies {
        classpath 'net.minecraftforge.gradle:ForgeGradle:2.3-SNAPSHOT'
    }
}

apply plugin: 'net.minecraftforge.gradle.forge'

// Версия Forge для 1.12.2
version = '1.0'
group = 'decompile'
archivesBaseName = 'decompile'

sourceCompatibility = 1.8
targetCompatibility = 1.8

minecraft {
    version = '1.12.2-14.23.5.2860'
    runDir = 'run'
    
    // Если нужны дополнительные библиотеки модов
    // replace '@VERSION@', '@MCVERSION@' в mcmod.info
    replace '@VERSION@', project.version
    replace '@MCVERSION@', project.version
}

dependencies {
    // Подключить IC2 как деобфусцированную библиотеку
    // deobfCompile fileTree(dir: 'libs', include: ['*.jar'])
}

processResources {
    from(sourceSets.main.resources.srcDirs) {
        include '**/*.info'
        rename '(.*)@(.*)@(.*)', '$1' + project.version + '$3'
    }
    from(sourceSets.main.resources.srcDirs) {
        exclude '**/*.info'
    }
}
```

---

## 5. Пошаговый процесс декомпиляции

### Шаг 1: Инициализация и декомпиляция Minecraft

```powershell
cd C:\fpga\mainbots\fg-project

# Первый запуск — скачает всё (может занять 10-30 минут)
.\gradlew.bat setupDecompWorkspace --refresh-dependencies

# Результат:
# src/minecraft/net/minecraft/... — декомпилированный Minecraft
# src/minecraft/mcp/... — классы MCP
```

### Шаг 2: Просмотр декомпилированного кода

```powershell
# Декомпилированный код появится здесь:
# src/minecraft/net/minecraft/

# Примеры путей:
# src/minecraft/net/minecraft/util/RegistryDefault.java
# src/minecraft/net/minecraft/inventory/recipe/IRecipe.java
# src/minecraft/net/minecraft/forge/oredict/ShapedOreRecipe.java
```

### Шаг 3: Декомпиляция модов (IC2, OpenComputers)

```powershell
# Положите .jar файлы модов в папку libs
mkdir libs
# Скопируйте IC2 и OpenComputers .jar в libs/

# Добавьте в build.gradle:
# dependencies {
#     deobfCompile fileTree(dir: 'libs', include: ['*.jar'])
# }

# Затем:
.\gradlew.bat deobfCompile --refresh-dependencies

# Результат:
# build/deobfMcModInfo.json
# build/libs/deobf-*.jar
```

### Шаг 4: Использование CFR напрямую для модов

```powershell
# Если нужно быстро посмотреть код мода без FG:
java -jar C:\tools\cfr\cfr.jar IC2-2.8.172.jar --showbr true --decimal true

# Это создаст IC2-2.8.172.java — полный декомпилированный исходник

# Для OpenComputers:
java -jar C:\tools\cfr\cfr.jar OpenComputers-MC1.12.2-1.7.7.172.jar
```

---

## 6. Конкретные команды для вашей задачи

```powershell
# ============================================
# ПОЛНЫЙ ПЕЙСПЕЙН: Извлечение рецептов
# ============================================

# 1. Создайте workspace
mkdir C:\fpga\mainbots\decompile-workspace
cd C:\fpga\mainbots\decompile-workspace

# 2. Скачайте и распакуйте Forge MDK 1.12.2
#    https://files.minecraftforge.net/net/minecraftforge/forge/index_1.12.2.html

# 3. Инициализация (первый раз — долго!)
.\gradlew.bat setupDecompWorkspace

# 4. Код Minecraft появится в src/minecraft/
#    Ищите рецепты здесь:
#    - net/minecraft/util/RecipeSorter.java
#    - net/minecraft/forge/oredict/*.java
#    - net/minecraft/inventory/*.java

# 5. Декомпиляция модов через CFR (быстрый способ)
java -jar C:\tools\cfr\cfr.jar %USERPROFILE%\.gradle\caches\modules-2\files-2.1\...\IC2*.jar --outputdir C:\fpga\mainbots\decompiled\ic2

java -jar C:\tools\cfr\cfr.jar %USERPROFILE%\.gradle\caches\modules-2\files-2.1\...\OpenComputers*.jar --outputdir C:\fpga\mainbots\decompiled\oc

# 6. Найдите классы рецептов:
#    IC2:
#    - ic2/core/recipe/*.java
#    - ic2/core/util/Recipe.java
#    OC:
#    - li/cil/oc/common/item/*.java
```

---

## 7. Полезные команды Gradle

```powershell
# Инициализация workspace
.\gradlew.bat setupDecompWorkspace

# Декомпиляция + ренейминг
.\gradlew.bat deobfCompile

# Создание деобфусцированного JAR
.\gradlew.bat deobfJar

# Создание JAR для загрузки
.\gradlew.bat build

# Очистка
.\gradlew.bat clean

# Показать все задачи
.\gradlew.bat tasks
```

---

## 8. Типичные проблемы и решения

| Проблема | Решение |
|---|---|
| **Java version error** | Убедитесь, что `JAVA_HOME` указывает на JDK 8: `java -version` |
| **Gradle sync failed** | Запустите `--refresh-dependencies` |
| **Out of memory** | Добавьте `org.gradle.jvmargs=-Xmx3G` в `gradle.properties` |
| **Mappings mismatch** | Попробуйте `mappings = 'snapshot_20171003'` вместо `stable_39` |
| **CFR не декомпилирует** | Попробуйте `--showbr true --decimal true` |

---

## Итоговая рекомендация

Для извлечения рецептов IC2 и OpenComputers самый быстрый путь:

```
1. CFR напрямую → декомпилировать .jar модов
2. ForgeGradle 2.3 → получить декомпилированный Minecraft
3. Написать Python/Java скрипт → парсить рецепты из исходников
```

CFR даст вам читаемый Java-код модов за минуты. ForgeGradle — полный декомпилированный Minecraft за 10-30 минут (первый запуск).

---

## Полезные пути для поиска рецептов

### Vanilla Minecraft (после декомпиляции через FG)
- `src/minecraft/net/minecraft/util/RecipeSorter.java` — регистратор рецептов
- `src/minecraft/net/minecraft/forge/oredict/ShapedOreRecipe.java` — shapeless крафты
- `src/minecraft/net/minecraft/forge/oredict/ShapelessOreRecipe.java` — shapeless крафты
- `src/minecraft/net/minecraft/inventory/` — базовые классы инвентаря

### IC2 (Industrial Craft 2)
- `ic2/core/recipe/` — базовые классы рецептов
- `ic2/core/recipe/MachineRecipe.java` — рецепты машин
- `ic2/core/recipe/CannerRecipe.java` — Canner
- `ic2/core/recipe/MaceratorRecipe.java` — Macerator
- `ic2/core/recipe/CompressorRecipe.java` — Compressor
- `ic2/core/recipe/ExtractorRecipe.java` — Extractor
- `assets/ic2/recipes/` — JSON-рецепты в ресурспаке

### OpenComputers
- `li/cil/oc/common/item/` — определение компонентов
- Рецепты крафта OC-компонентов — в vanilla-формате через `GameRegistry.addShapedRecipe`
