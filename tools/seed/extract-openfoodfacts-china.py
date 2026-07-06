#!/usr/bin/env python3
"""Stream-process Open Food Facts JSONL.gz to extract Chinese products."""
import gzip
import json
import csv
import sys
import io
from urllib.request import urlopen

JSONL_URL = "https://static.openfoodfacts.org/data/openfoodfacts-products.jsonl.gz"
OUTPUT_CSV = "data/seed/openfoodfacts-china-products.csv"
MAX_PRODUCTS = 5000

OUTPUT_FIELDS = [
    "name", "item_type", "category", "subcategory", "brand",
    "barcode", "alias", "search_keywords", "tags", "source", "audit_status",
]

KNOWN_BRANDS = [
    "旺旺", "康师傅", "统一", "可口可乐", "百事", "元气森林", "农夫山泉",
    "伊利", "蒙牛", "光明", "乐事", "上好佳", "好丽友", "奥利奥", "雀巢",
    "德芙", "百草味", "三只松鼠", "良品铺子", "来伊份", "卫龙", "喜之郎",
    "王老吉", "加多宝", "红牛", "东鹏", "六个核桃", "养元", "娃哈哈",
    "汇源", "美汁源", "果粒橙", "脉动", "宝矿力", "尖叫", "雪花", "青岛",
    "哈尔滨", "江小白", "海天", "李锦记", "老干妈", "乌江", "洽洽",
    "双汇", "雨润", "思念", "三全", "安井", "湾仔码头", "五芳斋",
]


def is_chinese_product(product):
    countries = product.get("countries_tags", [])
    if any("china" in c.lower() for c in countries):
        return True
    if any("taiwan" in c.lower() for c in countries):
        return True

    brands = (product.get("brands") or "").lower()
    for brand in KNOWN_BRANDS:
        if brand.lower() in brands:
            return True

    langs = product.get("lc") or ""
    if langs == "zh":
        return True

    return False


def extract_name(product):
    for field in ["product_name_zh", "product_name", "generic_name_zh", "generic_name"]:
        val = product.get(field)
        if val and val.strip():
            return val.strip()
    return ""


def extract_category(product):
    categories = (product.get("categories") or "").split(",")
    mapping = {
        "chinese": "中式正餐",
        "snacks": "零食",
        "sweets": "零食",
        "beverages": "饮料",
        "dairies": "饮料",
        "fruits": "水果",
        "vegetables": "水果",
        "meats": "中式正餐",
        "cereals": "方便速食",
        "pasta": "方便速食",
        "soups": "方便速食",
        "sauces": "调味品",
        "desserts": "甜品冰品",
        "bakery": "烘焙糕点",
        "bread": "烘焙糕点",
        "cheeses": "烘焙糕点",
        "seafood": "中式正餐",
        "plant-based": "方便速食",
        "eggs": "中式正餐",
        "fats": "调味品",
    }
    for cat in categories:
        cat_clean = cat.strip().lower().replace("-", " ").replace("_", " ")
        for key, mapped in mapping.items():
            if key in cat_clean:
                return mapped
    return "零食"


def extract_subcategory(product):
    categories = (product.get("categories") or "").split(",")
    sub_mapping = {
        "potato": "薯片",
        "chips": "薯片",
        "crisps": "薯片",
        "chocolate": "巧克力",
        "cookie": "饼干",
        "biscuit": "饼干",
        "cake": "蛋糕",
        "instant noodle": "方便面",
        "noodle": "面食",
        "carbonated": "碳酸饮料",
        "soda": "碳酸饮料",
        "cola": "碳酸饮料",
        "juice": "果汁",
        "milk": "乳饮料",
        "yogurt": "乳饮料",
        "water": "矿泉水",
        "coffee": "咖啡",
        "tea drink": "茶饮料",
        "ice cream": "冰淇淋",
        "candy": "糖果",
        "gum": "糖果",
        "sauce": "调味酱",
        "pickle": "腌菜",
        "tofu": "豆制品",
        "rice": "米面主食",
        "bread": "面包",
        "pie": "派",
        "puffed": "膨化食品",
        "puff": "膨化食品",
        "sausage": "肉制品",
        "ham": "肉制品",
        "dumpling": "速冻食品",
        "frozen": "速冻食品",
    }
    for cat in categories:
        cat_clean = cat.strip().lower().replace("-", " ").replace("_", " ")
        for key, mapped in sub_mapping.items():
            if key in cat_clean:
                return mapped
    return ""


def extract_search_keywords(name, brand, category, subcategory):
    parts = [name]
    if brand:
        parts.append(brand)
    if category:
        parts.append(category)
    if subcategory:
        parts.append(subcategory)
    seen = set()
    tokens = []
    for p in parts:
        for t in p.replace(",", " ").replace("/", " ").split():
            t = t.strip()
            if t and t not in seen:
                seen.add(t)
                tokens.append(t)
    return ",".join(tokens)


def process():
    count = 0
    skipped_no_name = 0
    skipped_not_china = 0

    out_file = io.open(OUTPUT_CSV, "w", encoding="utf-8-sig", newline="")
    writer = csv.DictWriter(out_file, fieldnames=OUTPUT_FIELDS)
    writer.writeheader()

    print(f"Downloading {JSONL_URL} ...", file=sys.stderr)
    req = urlopen(JSONL_URL)
    decompressed = gzip.GzipFile(fileobj=req)

    for line_bytes in decompressed:
        line = line_bytes.decode("utf-8", errors="replace").strip()
        if not line:
            continue

        product = json.loads(line)
        barcode = product.get("code", "").strip()

        name = extract_name(product)
        if not name:
            skipped_no_name += 1
            continue

        if not is_chinese_product(product):
            skipped_not_china += 1
            continue

        brand = (product.get("brands") or "").strip()
        category = extract_category(product)
        subcategory = extract_subcategory(product)
        search_keywords = extract_search_keywords(name, brand, category, subcategory)

        writer.writerow({
            "name": name,
            "item_type": "packaged_product",
            "category": category,
            "subcategory": subcategory,
            "brand": brand,
            "barcode": barcode,
            "alias": "",
            "search_keywords": search_keywords,
            "tags": "",
            "source": "external_api",
            "audit_status": "approved",
        })
        count += 1

        if count % 500 == 0:
            print(f"  Extracted {count} products...", file=sys.stderr)
        if count >= MAX_PRODUCTS:
            break

    out_file.close()
    req.close()

    print(f"\nDone! Extracted {count} products to {OUTPUT_CSV}", file=sys.stderr)
    print(f"Skipped (no name): {skipped_no_name}", file=sys.stderr)
    print(f"Skipped (not Chinese): {skipped_not_china}", file=sys.stderr)


if __name__ == "__main__":
    process()
