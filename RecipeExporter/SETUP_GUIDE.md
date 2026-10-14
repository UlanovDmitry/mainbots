# Установка среды разработки для мода Minecraft 1.12.2

## Оглавление

1. [Требования](#1-требования)
2. [Установка Java 8](#2-установка-java-8)
3. [Установка Git](#3-установка-git)
4. [Настройка IntelliJ IDEA](#4-настройка-intellij-idea)
5. [Создание проекта ForgeGradle](#5-создание-проекта-forgegradle)
6. [Генерация run-конфигураций](#6-генерация-run-конфигураций)
7. [Первая сборка мода](#7-первая-сборка-мода)
8. [Решение проблем](#8-решение-проблем)

---

## 1. Требования

```
┌────────────────────────────────────────────────────┐
│                    ТРЕБОВАНИЯ                       │
├────────────────────────────────────────────────────┤
│ ✓ IntelliJ IDEA Community Edition (у вас уже есть) │
│ • Java 8 JDK (обязательно!)                        │
│ • Git                                              │
│ • 4+ ГБ RAM                                        │
│ • ~5 ГБ свободного места                           │
└────────────────────────────────────────────────────┘
```

> **Важно:** ForgeGradle 2.3 для 1.12.2 **НЕ работает** с Java 11, 17 или 21. Нужна строго Java 8.

---

## 2. Установка Java 8

### Шаг 1: Скачайте JDK 8

1. Перейдите на https://adoptium.net/temurin/releases/?version=8
2. Выберите **Windows**, **x64**, **JDK** (не JRE!)
3. Скачайте `.msi` файл (около 170 МБ)

### Шаг 2: Установите JDK 8

1. Запустите скачанный `.msi` файл
2. Примите лицензию, выберите путь установки (запомните его!)
3. Стандартный путь: `C:\Program Files\Eclipse Adoptium\jdk-8.0.xxx-hotspot`

### Шаг 3: Проверьте установку

Откройте PowerShell и выполните:

```powershell
java -version
javac -version
```

Должно вывести что-то вроде:
```
openjdk version "1.8.0_402"
```

### Шаг 4: Настройте JAVA_HOME

```powershell
# Найдите точный путь к вашей установке JDK 8
Get-ChildItem "C:\Program Files\Eclipse Adoptium"

# Установите переменную окружения JAVA_HOME (замените путь на ваш!)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-8.0.402-hotspot"

# Проверьте
echo $env:JAVA_HOME
java -version
```

> **Совет:** Чтобы JAVA_HOME сохранялся после перезагрузки:
> 1. Нажмите `Win + S` → введите "Переменные среды"
> 2. Откройте "Изменение системных переменных среды"
> 3. Нажмите "Переменные среды..."
> 4. В разделе "Системные переменные" → "Создать..."
> 5. Имя: `JAVA_HOME`, Значение: `C:\Program Files\Eclipse Adoptium\jdk-8.0.402-hotspot`

---

## 3. Установка Git

### Шаг 1: Скачайте Git

1. Перейдите на https://git-scm.com/download/win
2. Скачайте установщик

### Шаг 2: Установите Git

1. Запустите установщик
2. На шаге "Choosing the default editor" выберите **VS Code** или **Notepad++** (или оставьте Vim)
3. На шаге "Adjusting your PATH" выберите **Git from the command line** (рекомендуется)
4. На шаге "Configuring the line ending conversions" выберите **Checkout Windows-style**
5. Завершите установку

### Шаг 3: Проверьте установку

```powershell
git --version
```

Должно вывести: `git version 2.x.x.windows.x`

---

## 4. Настройка IntelliJ IDEA

### Шаг 1: Откройте проект

1. Запустите IntelliJ IDEA Community
2. **File → Open** → выберите папку `C:\fpga\mainbots\RecipeExporter`

### Шаг 2: Дождитесь индексации

1. IntelliJ предложит импортировать Gradle-проект
2. Нажмите **Open as Project**
3. Дождитесь окончания индексации (внизу справа прогресс-бар)

### Шаг 3: Настройте JDK для проекта

1. **File → Project Structure** (или `Ctrl + Alt + Shift + S`)
2. **Project** → **SDK**: выберите JDK 1.8
3. **Project language level**: выберите `8 - Diamond, Local variable type inference...`
4. **Modules** → вкладка **Dependencies**:
   - Для `RecipeExporter.main`: выберите SDK 1.8
5. Нажмите **Apply → OK**

### Шаг 4: Настройте Gradle

1. **File → Settings** (или `Ctrl + Alt + S`)
2. **Build, Execution, Deployment → Build Tools → Gradle**
3. **JVM for indexer**: выберите `1.8`
4. **Build and run using**: выберите `IntelliJ IDEA`
5. В разделе **Gradle project**:
   - **JVM arguments**: добавьте `-Xmx2g` (если мало памяти)
6. Нажмите **Apply → OK**

### Шаг 5: Настройте компилятор

1. **File → Settings → Build, Execution, Deployment → Compiler → Java Compiler**
2. В разделе **Project bytecode version**: выберите `8`
3. В таблице ниже, для модуля `RecipeExporter.main`:
   - **Target bytecode version**: `8`
4. Нажмите **Apply → OK**

---

## 5. Создание проекта ForgeGradle

### Способ 1: Использовать готовый RecipeExporter (рекомендуется)

Проект уже создан в `C:\fpga\mainbots\RecipeExporter`. Переходите к разделу 6.

### Способ 2: Создать проект с нуля

#### Шаг 1: Скачайте Forge MDK

1. Перейдите на https://files.minecraftforge.net/net/minecraftforge/forge/index_1.12.2.html
2. Найдите версию `1.12.2-14.23.5.2860` (последняя для 1.12.2)
3. Нажмите **MDK** (Mod Developer Kit)
4. Распакуйте в `C:\fpga\mainbots\my-mod`

#### Шаг 2: Настройте build.gradle

Откройте `build.gradle` в IntelliJ и замените содержимое:

```groovy
buildscript {
    repositories {
        maven { url = 'https://files.minecraftforge.net/maven' }
        maven { url = 'https://repo.maven.apache.org/maven2' }
    }
    dependencies {
        classpath 'net.minecraftforge.gradle:ForgeGradle:2.3-SNAPSHOT'
    }
}

apply plugin: 'net.minecraftforge.gradle.forge'

version = '1.0'
group = 'com.yourname.yourmod'  // Замените на свой пакет
archivesBaseName = 'YourMod'

sourceCompatibility = 1.8
targetCompatibility = 1.8

minecraft {
    version = '1.12.2-14.23.5.2860'
    runDir = 'run'
    mappings = 'stable_39'
    replace '@VERSION@', project.version
    replace '@MCVERSION@', project.version
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

#### Шаг 3: Создайте структуру папок

```
my-mod/
├── build.gradle
├── settings.gradle
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── yourname/
│       │           └── yourmod/
│       │               └── YourMod.java
│       └── resources/
│           └── mcmod.info
```

#### Файл mcmod.info (`src/main/resources/mcmod.info`):

```json
{
    "modinfo": {
        "modId": "yourmod",
        "name": "Your Mod Name",
        "description": "Description of your mod",
        "version": "${VERSION}",
        "mcversion": "${MCVERSION}",
        "authorList": ["YourName"],
        "parent": "",
        "useDependencyInformation": false
    }
}
```

#### Файл YourMod.java (`src/main/java/com/yourname/yourmod/YourMod.java`):

```java
package com.yourname.yourmod;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(
    modid = YourMod.MOD_ID,
    name = YourMod.MOD_NAME,
    version = YourMod.MOD_VERSION,
    acceptedMinecraftVersions = "[1.12.2]"
)
public class YourMod {
    public static final String MOD_ID = "yourmod";
    public static final String MOD_NAME = "Your Mod Name";
    public static final String MOD_VERSION = "1.0.0";

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        System.out.println(MOD_NAME + " загружен!");
    }
}
```

---

## 6. Генерация run-конфигураций

### В IntelliJ IDEA

1. Откройте проект в IntelliJ
2. Откройте **Terminal** (`View → Tool Windows → Terminal` или `Alt + F12`)
3. Выполните:

```powershell
.\gradlew.bat genIntellijRuns
```

4. IntelliJ автоматически создаст run-конфигурации:
   - **Minecraft Client** — запуск клиента с модом
   - **Minecraft Server** — запуск сервера с модом

### Альтернатива: через Terminal

```powershell
# Инициализация (первый раз — скачает все зависимости, 10-30 минут)
.\gradlew.bat setupDecompWorkspace --refresh-dependencies

# Генерация run-конфигураций для IntelliJ
.\gradlew.bat genIntellijRuns
```

---

## 7. Первая сборка мода

### Шаг 1: Инициализация

```powershell
# В папке проекта:
.\gradlew.bat setupDecompWorkspace --refresh-dependencies
```

Первый запуск может занять **10-30 минут**. Gradle скачает:
- Minecraft 1.12.2
- Forge libraries
- MCP mappings
- Зависимости Gradle

### Шаг 2: Сборка

```powershell
.\gradlew.bat build
```

Готовый JAR будет в: `build/libs/YourMod-1.0.jar`

### Шаг 3: Запуск в Minecraft

1. Скопируйте JAR в `.minecraft/mods/`:
   ```powershell
   copy build\libs\YourMod-1.0.jar %USERPROFILE%\.minecraft\mods\
   ```

2. Запустите Minecraft с профилем Forge 1.12.2

3. В главном меню должен появиться ваш мод!

---

## 8. Решение проблем

### Проблема: "Java version is not the required version (1.8)"

**Решение:**
```powershell
# Проверьте версию Java
java -version
javac -version

# Если версия не 1.8, установите JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-8.0.402-hotspot"

# Или через переменные окружения (см. шаг 4 в разделе 2)
```

### Проблема: "Could not resolve net.minecraftforge.gradle:ForgeGradle:2.3-SNAPSHOT"

**Решение:**
```powershell
# Очистите кэш Gradle и попробуйте снова
.\gradlew.bat --refresh-dependencies
```

### Проблема: "Out of memory error"

**Решение:** Создайте файл `gradle.properties` в папке проекта:

```properties
org.gradle.jvmargs=-Xmx3g
org.gradle.parallel=false
```

### Проблема: IntelliJ не видит Java-классы

**Решение:**
1. **File → Invalidate Caches / Restart**
2. Выберите **Invalidate and Restart**
3. После перезапуска: **File → Sync Project with Gradle Files**

### Проблема: "Build failed" с ошибками компиляции

**Решение:**
1. Проверьте, что все настройки bytecode version = 8
2. Проверьте, что `sourceCompatibility = 1.8` и `targetCompatibility = 1.8`
3. Пересоберите: `.\gradlew.bat clean build`

### Проблема: Minecraft не запускается

**Решение:**
1. Проверьте консоль Gradle на ошибки
2. Проверьте `run/logs/latest.log`
3. Убедитесь, что Forge установлен: `.minecraft/versions/1.12.2-Forge`

---

## Быстрый чек-лист

```
□ Установлена Java 8 (JDK 8)
□ Установлен Git
□ JAVA_HOME настроен на JDK 8
□ IntelliJ IDEA Community установлена
□ Проект открыт в IntelliJ
□ JDK 8 выбран в Project Structure
□ Gradle настроен на JVM 1.8
□ Выполнен gradlew setupDecompWorkspace
□ Выполнен gradlew genIntellijRuns
□ Выполнен gradlew build
□ JAR скопирован в .minecraft/mods/
□ Minecraft запускается с модом
```

---

## Полезные команды

```powershell
# Инициализация (первый раз — долго!)
.\gradlew.bat setupDecompWorkspace

# Генерация run-конфигураций
.\gradlew.bat genIntellijRuns

# Сборка мода
.\gradlew.bat build

# Запуск клиента (альтернатива run-конфигурации)
.\gradlew.bat runClient

# Запуск сервера
.\gradlew.bat runServer

# Очистка проекта
.\gradlew.bat clean

# Пересборка
.\gradlew.bat clean build

# Обновить зависимости
.\gradlew.bat --refresh-dependencies
```
