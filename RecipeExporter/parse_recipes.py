#!/usr/bin/env python3
"""
Recipe Exporter — Python-скрипт для анализа экспортированных рецептов Minecraft 1.12.2

Использование:
    python parse_recipes.py all_recipes.json
    python parse_recipes.py all_recipes.json --type smelting
    python parse_recipes.py all_recipes.json --item minecraft:diamond
    python parse_recipes.py all_recipes.json --output recipes.csv
"""

import json
import sys
import argparse
from pathlib import Path
from collections import defaultdict
from typing import List, Dict, Any, Optional


def load_recipes(filepath: str) -> Dict[str, Any]:
    """Загружает JSON с рецептами"""
    with open(filepath, 'r', encoding='utf-8') as f:
        return json.load(f)


def get_output_item(recipe: Dict[str, Any]) -> Optional[str]:
    """Получает выходной предмет из рецепта"""
    output = recipe.get('output')
    if output and isinstance(output, dict):
        return output.get('item', '')
    return None


def get_input_items(recipe: Dict[str, Any]) -> List[str]:
    """Получает входящие предметы из рецепта"""
    ingredients = recipe.get('ingredients', [])
    items = []
    for ing in ingredients:
        if isinstance(ing, dict):
            item = ing.get('item', '')
            if item:
                items.append(item)
    return items


def filter_by_type(recipes: List[Dict], recipe_type: str) -> List[Dict]:
    """Фильтрует рецепты по типу"""
    if recipe_type == 'smelting':
        return [r for r in recipes if r.get('type') == 'smelting']
    elif recipe_type == 'crafting':
        return [r for r in recipes if r.get('type') != 'smelting']
    return recipes


def filter_by_item(recipes: List[Dict], item: str) -> List[Dict]:
    """Фильтрует рецепты, содержащие указанный предмет"""
    result = []
    for recipe in recipes:
        # Проверяем выходной предмет
        output = get_output_item(recipe)
        if output and item in output:
            result.append(recipe)
            continue
        
        # Проверяем входящие предметы
        inputs = get_input_items(recipe)
        if any(item in inp for inp in inputs):
            result.append(recipe)
    
    return result


def count_recipes(recipes: List[Dict]) -> Dict[str, int]:
    """Подсчитывает количество рецептов по типам"""
    counts = defaultdict(int)
    for recipe in recipes:
        if recipe.get('type') == 'smelting':
            counts['smelting'] += 1
        else:
            counts['crafting'] += 1
    return dict(counts)


def get_unique_items(recipes: List[Dict]) -> Dict[str, int]:
    """Получает список уникальных предметов и их частоту в рецептах"""
    item_counts = defaultdict(int)
    for recipe in recipes:
        # Добавляем выходной предмет
        output = get_output_item(recipe)
        if output:
            item_counts[output] += 1
        
        # Добавляем входящие предметы
        for inp in get_input_items(recipe):
            item_counts[inp] += 1
    
    return dict(sorted(item_counts.items(), key=lambda x: x[1], reverse=True))


def export_csv(recipes: List[Dict], output_path: str):
    """Экспортирует рецепты в CSV"""
    import csv
    
    with open(output_path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['Тип', 'Входящие', 'Выходной', 'Опыт'])
        
        for recipe in recipes:
            recipe_type = recipe.get('type', 'crafting')
            inputs = ', '.join(get_input_items(recipe))
            output = get_output_item(recipe) or ''
            xp = recipe.get('experience', 0)
            
            writer.writerow([recipe_type, inputs, output, xp])
    
    print(f"Экспортировано {len(recipes)} рецептов в {output_path}")


def print_summary(data: Dict[str, Any]):
    """Выводит сводку по рецептам"""
    recipes = data.get('recipes', [])
    
    print(f"\n{'='*60}")
    print(f"  Recipe Exporter — Сводка")
    print(f"{'='*60}")
    print(f"  Мод: {data.get('mod', 'N/A')}")
    print(f"  Версия: {data.get('version', 'N/A')}")
    print(f"  Minecraft: {data.get('minecraft_version', 'N/A')}")
    print(f"  Всего рецептов: {len(recipes)}")
    
    # Подсчёт по типам
    counts = count_recipes(recipes)
    print(f"\n  По типам:")
    for rtype, count in counts.items():
        print(f"    {rtype}: {count}")
    
    # Топ-10 самых популярных предметов
    unique_items = get_unique_items(recipes)
    print(f"\n  Топ-10 предметов в рецептах:")
    for i, (item, count) in enumerate(list(unique_items.items())[:10], 1):
        print(f"    {i}. {item} — {count} раз(а)")
    
    print(f"{'='*60}\n")


def main():
    parser = argparse.ArgumentParser(description='Анализ рецептов Minecraft 1.12.2')
    parser.add_argument('input', help='Путь к JSON-файлу с рецептами')
    parser.add_argument('--type', choices=['crafting', 'smelting'], 
                       help='Фильтр по типу рецепта')
    parser.add_argument('--item', help='Фильтр по предмету (частичное совпадение)')
    parser.add_argument('--output', help='Экспорт в CSV')
    parser.add_argument('--summary', action='store_true', 
                       help='Вывести сводку')
    
    args = parser.parse_args()
    
    # Загрузка рецептов
    print(f"Загрузка рецептов из {args.input}...")
    data = load_recipes(args.input)
    recipes = data.get('recipes', [])
    
    if not recipes:
        print("Ошибка: файл не содержит рецептов!")
        sys.exit(1)
    
    # Применение фильтров
    if args.type:
        recipes = filter_by_type(recipes, args.type)
        print(f"После фильтрации по типу '{args.type}': {len(recipes)} рецептов")
    
    if args.item:
        recipes = filter_by_item(recipes, args.item)
        print(f"После фильтрации по предмету '{args.item}': {len(recipes)} рецептов")
    
    # Вывод сводки
    if args.summary:
        print_summary(data)
    
    # Экспорт в CSV
    if args.output:
        export_csv(recipes, args.output)
    
    # Вывод первых рецептов
    print(f"\nПервые {min(5, len(recipes))} рецептов:")
    print("-" * 60)
    for i, recipe in enumerate(recipes[:5], 1):
        recipe_type = recipe.get('type', 'crafting')
        output = get_output_item(recipe)
        inputs = get_input_items(recipe)
        
        print(f"\n  {i}. [{recipe_type}]")
        print(f"     Выход: {output}")
        print(f"     Вход: {', '.join(inputs)}")
    
    print(f"\nВсего рецептов: {len(recipes)}")


if __name__ == '__main__':
    main()
