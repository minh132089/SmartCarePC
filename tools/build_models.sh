#!/bin/sh
# Xử lý lại toàn bộ model: Blender (xoá logo, xoay, đổi đơn vị) rồi nén cho web.
# Chạy từ thư mục gốc dự án: npm run models
set -e
for key in $(python3 -c "import json;print(' '.join(json.load(open('tools/models.json'))))"); do
  blender -b --python tools/process_model.py -- tools/models.json "$key" 2>&1 | grep -E "RESULT|Error|Traceback" || true
done
# Dựng 4 vỏ case, kích thước đọc thẳng từ src/data/builds.ts
node -e "import('./src/data/builds.ts').then(m=>console.log(JSON.stringify(m.BUILDS.map(b=>({id:b.id,ff:b.formFactor,case:b.case,psu:b.psu,rear:b.rearFan,accent:b.accent})))))" > assets-src/builds.json
blender -b --python tools/build_cases.py -- assets-src/builds.json assets-src/processed 2>&1 | grep -E "RESULT|Error|Traceback" || true

# CPU và 2 loại tản nhiệt, dùng lại model quạt vừa xử lý
blender -b --python tools/build_parts.py -- assets-src/processed 2>&1 | grep -E "RESULT|Error|Traceback" || true

# Bộ linh kiện máy Gaming, dựng theo bản vẽ của Codex (design/gaming/)
blender -b --python tools/build_gaming.py -- assets-src/gaming 2>&1 | grep -E "RESULT|Error|Traceback" || true

mkdir -p public/models
# Bộ Gaming có nhiều node quay (cánh quạt) và node ẩn/hiện, nên chỉ nén hình khối
for f in assets-src/gaming/g_*.glb; do
  npx gltf-transform meshopt "$f" "public/models/$(basename "$f")"
done
for f in assets-src/processed/*.glb; do
  case "$(basename "$f")" in
    # Vỏ case: chỉ nén hình khối. Bước optimize đầy đủ làm hỏng độ trong suốt của tấm lưới đục lỗ.
    case_*) npx gltf-transform meshopt "$f" "public/models/$(basename "$f")" ;;
    # Model có bộ phận chuyển động: optimize gộp mesh và dời gốc node, làm cánh quạt quay lệch tâm
    fan.glb|aio_radiator.glb) npx gltf-transform meshopt "$f" "public/models/$(basename "$f")" ;;
    *) npx gltf-transform optimize "$f" "public/models/$(basename "$f")" --compress meshopt --texture-compress webp --texture-size 1024 ;;
  esac
done
