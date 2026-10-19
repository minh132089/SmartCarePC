# Bàn giao dự án PC SmartCare (cho agent khác đọc)

Cập nhật 2026-10-03. Đọc file này trước, rồi mới đọc code.

## Dự án là gì

Web app 3D giúp người **không rành công nghệ** nhìn và tập lắp một bộ PC. Là công cụ nghiên cứu cho bài BTEC Unit 16 của nhóm 5 sinh viên (proposal ở `~/Downloads/Research Proposal form - Chu Huy Minh (fixed).docx`). Giao diện tiếng Việt, kiểu Apple sáng và sạch, có animation. Không dùng logo hay tên hãng thật.

Stack: Vite 8, React 19, TypeScript, three, @react-three/fiber + drei + postprocessing, @react-spring/three, zustand, Tailwind v4.

Chạy: `cd ~/Documents/PCsmartcare && npm run dev` (cổng 5173). Kiểm tra: `npx tsc -b && npx oxlint src && npm run build`.

## Các loại máy

Thanh bên có **6 họ case** (chọn theo dáng) và máy **Gaming (đầy đủ)**. Không gắn cố định họ nào với nhu cầu nào: người dùng sẽ làm backend AI tư vấn sau. Bốn build cũ trong `src/data/builds.ts` (Văn phòng, Học tập, Đồ họa) vẫn còn trong code nhưng không hiện trên thanh chọn.

| Họ | Thân (W × H × D) | Bo | Tản | GPU | Nguồn |
|---|---|---|---|---|---|
| standard_tower | 230 × 460 × 430 | ATX | tháp 155 | 280 ngang | ATX |
| compact_tower | 210 × 380 × 370 | mATX | tháp 155 | 240 ngang | ATX |
| showcase_dual_chamber | 300 × 450 × 450 | ATX | AIO 240 | 280 dọc + riser | ATX sau khay |
| mini_sff | 180 × 250 × 350 | ITX | thấp 47 | 240 sandwich + riser | SFX |
| slim_console | 100 × 360 × 330 | ITX | thấp 47 | 240 tầng trên + riser | SFX |
| full_tower | 250 × 620 × 550 | ATX | AIO 360 | 280 ngang | ATX |

## Cách đã làm 6 họ case

Concept gốc của người dùng ở `design/case-families/`. Codex viết đặc tả tổng `design/families/00_families_spec.md` (nguồn số liệu chính) và 22 tờ vẽ (`design/families/0N_<họ>/00_overview`, `01_case`, `parts/*`, mỗi tờ có `.md` ghi số mm). Chạy lại bằng `design/families/run_all_sheets.sh`.

| Thứ | File |
|---|---|
| Số liệu dùng chung cho Blender và app | `src/data/families.json`, `src/data/boards.json` (bo mATX, ITX) |
| Dựng model | `tools/build_families.py` (dùng lại hàm của `build_gaming.py`), ra `assets-src/families/f_*.glb` → nén vào `public/models/f_*.glb` |
| Ảnh 8 góc để soát | `blender -b --python tools/render_views.py -- <file.glb> <ảnh.png>` → `design/families/review/views/` (lưới đục lỗ alpha hiện trong suốt trong ảnh này, trong app vẫn hiện đúng) |
| Kiểm tra va chạm | `blender -b --python tools/check_family.py -- <họ ...>`, phải 0 dòng "VA CHẠM" |
| Lắp ráp trong app | `src/data/families.ts`, `src/scene/FamilyRig.tsx` (dây đi sát khay qua `alongTray`) |
| Xem cận cảnh | `src/data/familyHotspots.ts`, `FamilyPartModel` trong `src/ui/Inspector.tsx` |
| Ảnh app | `design/families/review/app/`, `design/families/review/inspect/` |

## Cách đã làm bộ Gaming

Quy trình: một AI thiết kế (Codex, qua `codex exec`) vẽ blueprint và ảnh từng linh kiện + file mô tả mm vào `design/gaming/`. Sau đó dựng 3D trong Blender bằng code, đưa vào app, tự kiểm tra bằng công cụ va chạm và ảnh chụp. Gần cuối phiên người dùng bảo **tự kiểm tra, không cần hỏi ChatGPT/Codex nữa**.

| Thứ | File |
|---|---|
| Bản vẽ + mô tả | `design/gaming/00_blueprint.*`, `01..13_*.png/.md` (13 = nóc case, 12 = mặt trước kính + ARGB, 11 = cổng sau I/O) |
| Nhận xét review | `design/gaming/review/*.md` |
| Ảnh chụp app | `design/gaming/screens/` |
| Dựng model (Blender, mm, trục app) | `tools/build_gaming.py`, thư viện `tools/glib.py` |
| Kiểm tra va chạm | `tools/check_collisions.py` |
| Chụp app bằng Chrome headless | `tools/shot.mjs` |
| Nguồn GLB (gitignore) | `assets-src/gaming/g_*.glb`, bản nén ở `public/models/g_*.glb` |
| Lắp ráp trong app | `src/scene/GamingRig.tsx`, số đo `src/data/gaming.ts` |
| Chú thích "Xem cận cảnh" | `src/data/gamingHotspots.ts`, `src/ui/Inspector.tsx` |

Dựng lại model: `blender -b --python tools/build_gaming.py -- assets-src/gaming [tên ...]`, rồi nén **chỉ meshopt**: `npx gltf-transform meshopt assets-src/gaming/g_X.glb public/models/g_X.glb`. Lệnh `npm run models` cũng làm cả bước này.

## Bộ đồ nghề lắp máy

Rà theo các bước lắp, những thứ người lắp phải cầm mà chưa có model nằm trong `design/tools/00_assembly_needs.md`. Codex vẽ 9 tờ trong `design/tools/` (chạy lại bằng `design/tools/run_all.sh`).
Dựng bằng `tools/build_tools.py`, gồm tua vít, dây chống tĩnh điện, keo tản nhiệt, khay vít 9 ngăn, dây rút, dây nguồn AC, ăng-ten Wi-Fi, kìm cắt và thảm 600 × 400 (`t_kit.glb`).
Trong app: `src/scene/ToolKit.tsx` bày thảm trên sàn trước máy khi tách rời hoặc khi chọn mục "Đồ nghề lắp máy" (PartId `tools`); chú thích nằm trong `src/data/toolHotspots.ts`.

## Mặt lưng, dây, tách lắp (2026-10-04)

- Mặt lưng làm lại theo tờ Codex `design/rear/*.png|md`: `rear_panel()` trong `tools/build_families.py` (standard / compact / full), phần lưng của `case_showcase_dual_chamber` và `case()` trong `build_gaming.py`. Mini SFF chưa có tờ (Codex hết lượt dùng tới 10/10/2026); chạy lại `design/rear/run_rear.sh mini_sff "<mô tả>"`.
- Tách / lắp chạy từng bước theo `src/data/assembly.ts` (PSU → CPU → RAM → SSD → tản → GPU → quạt → dây); `Part` nhận prop `step`, `src/ui/AssemblySteps.tsx` hiện tên bước.
- Dây sau khay gom về một trục dọc (`trunk()` trong `FamilyRig.tsx`), quạt trước vòng qua mép khay. Đã thêm dây hub ARGB + nguồn hub, dây quạt két → CPU_FAN, USB-C → Type-E, đầu cắm trên mọi header.
- Kiểm tra linh kiện lòi ra ngoài vỏ: `node tools/check_overflow.mjs` (cần dev server). Chân ăng-ten bo và công tắc nguồn nhô qua lưng vài mm là đúng thiết kế.
- Họ slim_console đã bỏ khỏi app (`FAMILY_IDS`), dữ liệu và model vẫn giữ.

## Hiệu năng cho máy yếu (2026-10-04)

- Canvas chạy `frameloop="demand"`: đứng yên không vẽ. `src/perf/RenderControl.tsx` đánh thức cảnh ~0,9 s sau mỗi thay đổi store và vẽ liên tục khi "Bật máy" (quạt chỉ quay khi bật). Khung chính dừng hẳn khi mở Xem cận cảnh hoặc cuộn ra khỏi màn hình.
- 3 mức chất lượng `src/perf/quality.ts` (Cao / Vừa / Nhẹ: dpr, N8AO, Bloom, sàn phản chiếu, bóng). Chế độ Tự động đoán theo GPU / CPU / RAM rồi tự hạ khi đo < 28 khung/giây. Nút chọn ở thanh bên, lưu localStorage.
- Hiệu ứng mới cần chuyển động liên tục thì phải gọi `invalidate()` (hoặc đổi store) nếu không sẽ đứng hình.

## NGUYÊN TẮC CỨNG: hoạt cảnh lắp không được đi xuyên vật

Khi lắp (và tách rời), mọi linh kiện phải đi theo đường mà ngoài đời làm được: **không khối nào đi xuyên qua khối khác**
(vỏ, tấm che, bo mạch, linh kiện đã lắp). Chỉ được chạm trong 10 mm cuối, là đoạn cắm vào khe / socket / ốp vào chỗ bắt vít.
Khi **đã lắp xong** cũng không vật nào được cắm ngang qua thân linh kiện (vd khay giữa của Mini từng cắt ngang quạt nóc;
đã dời quạt ra phía trước, trên ngăn nguồn). `check:paths` kiểm cả hai.

- Đường lắp của từng khối nằm trong `src/data/installPaths.ts` (mã khối `<bước>.<thứ tự>`, vd `fans.2`); Part trong
  `FamilyRig` / `GamingRig` nhận `path` + `explode` qua `xp(...)`.
- Kiểm tra: `npm run dev` rồi `npm run check:paths` (tools/check_paths.mjs, Chrome headless). Còn vi phạm thì thoát mã 1.
  Tìm đường sạch cho khối vi phạm: `npm run check:paths -- --find [máy ...]`. Logic ở `src/scene/pathCheck.ts`.
- Đổi model, vị trí lắp, thứ tự bước hay đường lắp nào cũng phải chạy lại và **không còn vi phạm** mới được commit.

## Quy ước và bẫy quan trọng

- Trục: Blender (x, y, z) ↔ app (x, z, −y). Trong `build_gaming.py` mọi số là **mm theo trục app**: x lưng → trước, y đáy → nóc, z nắp kín → kính.
- Tôn vỏ case dày **3 mm** (bản vẽ gốc ghi 1 mm): quạt sau x −209,5, quạt trước x 208,5, nguồn tâm x −152. Mặt PCB cao 1,6 (bản vẽ giả định 3).
- **Boolean của Blender (EXACT) xóa sạch cả vật nếu các vật cắt chạm hoặc chồng nhau.** Đã dính ở mặt lưng case, cổng RJ45, vít SSD, khối gân nóc. `glib.boolean` giờ báo lỗi nếu mất hết mặt. Dùng `glib.cut_each` khi các vật cắt chạm nhau. Sau khi sửa case phải xem app từ cả 6 phía, không chỉ góc mặc định.
- Nén model có node quay (cánh quạt) hoặc trong suốt: **chỉ meshopt**, không dùng `optimize` (nó gộp mesh, dời gốc node, làm hỏng alpha).
- Đừng thêm hiệu ứng ToneMapping vào composer (renderer đã tone-map), giữ Bloom threshold ~1,6.
- Kính dùng màu **khói tối**, theo ý người dùng, kể cả khi bản vẽ ghi kính trong.
- Nóc case: không boolean khối gân đã gộp (đã làm mất gân). Gân nằm Y 233,45…233,95, dưới đệm vít két (Y 234).
- Dây giấu sau khay: `Route.flatten` ép bó dây vào khe dày ~6,5 mm. Dây ARGB dùng `fitLength()` để chiều dài đúng (IN 450, OUT 80, nối dài 200, dây header 650); dev console in ra chiều dài và số điểm nằm ngoài case (phải bằng 0).

## Việc còn dang dở

- Đường dây nguồn VGA bỏ qua vòng G0 → sau khay → G0 của bản vẽ, dây bơm cắm thẳng AIO_PUMP, ống nước ẩn khi "Tách rời".
- Đầu nối (ổ cắm dây) còn đơn giản: chưa có hốc và khóa chi tiết, chân bộ chia Y (nhánh B chỉ chân 1, 2, 4).
- Đã so đủ 22 tờ Codex của 6 họ với model (2026-10-04). Đã sửa theo bản vẽ: nóc/đáy Mini khe nhôm, cổng USB nằm ngang (Mini, Slim), vít quạt SFX, đế Slim, hub quạt cổng ở mép, thêm dây AC nối dài cho Mini/Slim. Còn khác có chủ ý: đầu quạt ITX ở y −80 (bản vẽ đè lên khe M.2), ống đồng tản tháp không vòng chữ U.
- Nóc mới chưa chạy lại kiểm tra va chạm với dây nối cổng I/O trên nóc (dây USB, nút nguồn, audio chạy từ điểm gom (190; 222; −30)). Cần kiểm tra các dây này khớp vị trí 5 cổng mới (Reset −68, USB −32, Power 0, USB +32, Headset +68).
- Chưa có git repo.

## Ghi chú về cách làm việc với người dùng

Người dùng nói tiếng Việt, hay gửi ảnh chụp màn hình để chỉ lỗi và muốn thấy ảnh kết quả sau mỗi thay đổi. Muốn mô hình chân thực nhưng **không giống hãng cụ thể** (riêng VGA được phép dáng giống card NVIDIA phổ thông, không logo, không chữ). Đừng tự cài hoặc tải gì khi chưa hỏi. Mật khẩu hay đăng nhập thì người dùng tự làm.

## Lịch sử chat

Toàn bộ đoạn chat gốc nằm ở `~/.claude/projects/-Users-yuuki2-Documents/6a5bee9f-71d6-4801-b4d8-68c4e12bf8f0.jsonl` (định dạng JSONL, rất dài). Nếu cần chi tiết quyết định cũ, dùng `grep` trong file này thay vì đọc hết. File này đã tóm đủ phần cần để làm tiếp.
