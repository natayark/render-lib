import os, glob

base = "phire-ui/locales"
keys_to_add = {
    "item-show-hp-bar": {
        "en-US": "Show HP Bar",
        "fr-FR": "Barre HP",
        "id-ID": "Tampilkan Bar HP",
        "ja-JP": "HP バーを表示",
        "ko-KR": "HP 바 표시",
        "pl-PL": "Pasek HP",
        "ru-RU": "Показать полоску HP",
        "th-TH": "แสดงแถบ HP",
        "vi-VN": "Hiển thị thanh HP",
        "zh-TW": "顯示 HP 血條",
        "zh-CN": "显示 HP 血条",
    },
    "item-show-judgement-detail": {
        "en-US": "Show Judgement Detail",
        "fr-FR": "Détails du jugement",
        "id-ID": "Tampilkan Detail Penilaian",
        "ja-JP": "判定詳細を表示",
        "ko-KR": "판정 상세 표시",
        "pl-PL": "Szczegóły oceny",
        "ru-RU": "Показать детали судейства",
        "th-TH": "แสดงรายละเอียดการตัดสิน",
        "vi-VN": "Hiển thị chi tiết phán đoán",
        "zh-TW": "顯示判定詳情",
        "zh-CN": "显示判定详情",
    },
    "item-render-extra": {
        "en-US": "Extra Rendering",
        "fr-FR": "Rendu supplémentaire",
        "id-ID": "Rendering Tambahan",
        "ja-JP": "追加レンダリング",
        "ko-KR": "추가 렌더링",
        "pl-PL": "Dodatkowe renderowanie",
        "ru-RU": "Дополнительная отрисовка",
        "th-TH": "การเรนเดอร์เพิ่มเติม",
        "vi-VN": "Hiển thị nội dung bổ sung",
        "zh-TW": "顯示額外內容",
        "zh-CN": "显示额外内容 (着色器/特效)",
    },
}

for ftl_path in glob.glob(os.path.join(base, "*/settings.ftl")):
    lang = os.path.basename(os.path.dirname(ftl_path))
    with open(ftl_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    appended = []
    for key, translations in keys_to_add.items():
        if key not in content:
            text = translations.get(lang, translations.get("en-US", key))
            appended.append(f"{key} = {text}")
    
    if appended:
        content = content.rstrip() + "\n" + "\n".join(appended) + "\n"
        with open(ftl_path, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"Updated: {lang} (+{len(appended)} keys)")
    else:
        print(f"OK: {lang}")
