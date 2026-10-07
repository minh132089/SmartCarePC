# Test nhanh toàn bộ API của SmartCarePC backend.
# Cách chạy (backend phải đang chạy):
#   powershell -ExecutionPolicy Bypass -File api-tests\run-api-tests.ps1
#   powershell -ExecutionPolicy Bypass -File api-tests\run-api-tests.ps1 -BaseUrl http://localhost:8084

param([string]$BaseUrl = "http://localhost:8083")

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$script:pass = 0
$script:fail = 0

# Gửi request bằng curl.exe, trả về @{ Status = mã HTTP; Body = JSON đã đọc }
function Call($method, $path, $body) {
    $tmp = [System.IO.Path]::GetTempFileName()
    $curlArgs = @('-s', '-o', $tmp, '-w', '%{http_code}', '-X', $method, "$BaseUrl$path")
    if ($body) {
        $bodyFile = [System.IO.Path]::GetTempFileName()
        [System.IO.File]::WriteAllText($bodyFile, $body)
        $curlArgs += @('-H', 'Content-Type: application/json', '--data-binary', "@$bodyFile")
    }
    $status = [int](& curl.exe @curlArgs)
    $text = [System.IO.File]::ReadAllText($tmp, [System.Text.Encoding]::UTF8)
    Remove-Item $tmp -ErrorAction SilentlyContinue
    if ($bodyFile) { Remove-Item $bodyFile -ErrorAction SilentlyContinue }
    $json = $null
    if ($text) { try { $json = $text | ConvertFrom-Json } catch { } }
    return @{ Status = $status; Body = $json }
}

function Check($name, [bool]$ok, $detail) {
    if ($ok) { $script:pass++; Write-Host "  PASS  $name" -ForegroundColor Green }
    else { $script:fail++; Write-Host "  FAIL  $name  ($detail)" -ForegroundColor Red }
}

function Rules($issues) { @($issues | ForEach-Object { $_.rule }) }

Write-Host "`nTest API tại $BaseUrl`n"

# Kiểm tra backend có đang chạy không
try { $null = Invoke-WebRequest "$BaseUrl/api/test/ping" -UseBasicParsing -TimeoutSec 5 }
catch { Write-Host "Không kết nối được $BaseUrl. Hãy chạy backend trước (.\mvnw.cmd spring-boot:run)." -ForegroundColor Yellow; exit 1 }

Write-Host "1. API test"
$r = Call GET "/api/test/ping"
Check "GET /api/test/ping trả 200, ok = true" ($r.Status -eq 200 -and $r.Body.ok) "HTTP $($r.Status)"
$r = Call POST "/api/test/echo" '{"hello":"SmartCarePC"}'
Check "POST /api/test/echo trả lại đúng body" ($r.Body.received.hello -eq "SmartCarePC") "HTTP $($r.Status)"

Write-Host "`n2. Dữ liệu danh mục"
$r = Call GET "/api/categories"
Check "GET /api/categories có 11 loại linh kiện" ($r.Status -eq 200 -and @($r.Body).Count -eq 11) "có $(@($r.Body).Count)"
$r = Call GET "/api/assembly-steps"
Check "GET /api/assembly-steps có 8 bước, bước 1 là psu" (@($r.Body).Count -eq 8 -and $r.Body[0].key -eq "psu") "có $(@($r.Body).Count)"
$r = Call GET "/api/credits"
Check "GET /api/credits có 10 nguồn model" (@($r.Body).Count -eq 10) "có $(@($r.Body).Count)"

Write-Host "`n3. Linh kiện"
$r = Call GET "/api/components"
Check "GET /api/components có 25 linh kiện" (@($r.Body).Count -eq 25) "có $(@($r.Body).Count)"
$r = Call GET "/api/components?category=gpu"
Check "GET /api/components?category=gpu có 2 card" (@($r.Body).Count -eq 2) "có $(@($r.Body).Count)"
$r = Call GET "/api/components/gpu_280"
Check "GET /api/components/gpu_280 có specs.lengthMm = 280" ($r.Status -eq 200 -and $r.Body.specs.lengthMm -eq 280) "HTTP $($r.Status)"
$r = Call GET "/api/components/khong_co"
Check "GET /api/components/khong_co trả 404" ($r.Status -eq 404) "HTTP $($r.Status)"

Write-Host "`n4. Bộ máy có sẵn"
$r = Call GET "/api/builds"
Check "GET /api/builds có 6 máy" (@($r.Body).Count -eq 6) "có $(@($r.Body).Count)"
$r = Call GET "/api/builds/mini_sff"
Check "GET /api/builds/mini_sff có 9 dòng linh kiện" ($r.Status -eq 200 -and @($r.Body.items).Count -eq 9) "có $(@($r.Body.items).Count)"
foreach ($code in "standard_tower", "compact_tower", "showcase_dual_chamber", "mini_sff", "full_tower", "gaming") {
    $r = Call GET "/api/builds/$code/check"
    Check "GET /api/builds/$code/check: lắp được" ($r.Body.compatible -eq $true) "lỗi: $((Rules $r.Body.errors) -join ', ')"
}

Write-Host "`n5. Kiểm tra tương thích (POST /api/builds/check)"
$ok = '{"items":[{"code":"case_mini_sff"},{"code":"mb_itx"},{"code":"cpu_std"},{"code":"low_profile_47"},{"code":"ram_ddr4","quantity":2},{"code":"gpu_240"},{"code":"ssd_m2_2280"},{"code":"psu_sfx"}]}'
$r = Call POST "/api/builds/check" $ok
Check "Cấu hình Mini đúng: compatible = true" ($r.Status -eq 200 -and $r.Body.compatible -eq $true) "lỗi: $((Rules $r.Body.errors) -join ', ')"

$bad = '{"items":[{"code":"case_mini_sff"},{"code":"mb_atx"},{"code":"cpu_std"},{"code":"tower_air_155"},{"code":"ram_ddr4","quantity":2},{"code":"gpu_280"},{"code":"ssd_m2_2280"},{"code":"psu_atx"}]}'
$r = Call POST "/api/builds/check" $bad
$rules = Rules $r.Body.errors
Check "Vỏ Mini + bo ATX + nguồn ATX + card 280 + tản tháp: báo đủ 4 lỗi" (
    $r.Body.compatible -eq $false -and ($rules -contains "case.form_factor") -and ($rules -contains "case.psu") -and
    ($rules -contains "gpu.length") -and ($rules -contains "cooler.height")) "lỗi: $($rules -join ', ')"

$r = Call POST "/api/builds/check" '{"items":[{"code":"case_compact_tower"},{"code":"mb_matx"},{"code":"cpu_std"},{"code":"aio_360"},{"code":"ram_ddr4","quantity":2},{"code":"gpu_240"},{"code":"ssd_m2_2280"},{"code":"psu_atx"}]}'
Check "Tản nước 360 trong vỏ không có chỗ gắn két: báo cooler.radiator" ((Rules $r.Body.errors) -contains "cooler.radiator") "lỗi: $((Rules $r.Body.errors) -join ', ')"

$r = Call POST "/api/builds/check" '{"items":[{"code":"case_mini_sff"},{"code":"mb_itx"},{"code":"cpu_std"},{"code":"low_profile_47"},{"code":"ram_ddr4","quantity":3},{"code":"gpu_240"},{"code":"ssd_m2_2280"},{"code":"psu_sfx"},{"code":"fan120","quantity":2}]}'
$rules = Rules $r.Body.errors
Check "3 thanh RAM vào bo ITX 2 khe, 2 quạt vào vỏ 1 chỗ: báo ram.slots và fans.slots" (($rules -contains "ram.slots") -and ($rules -contains "fans.slots")) "lỗi: $($rules -join ', ')"

$r = Call POST "/api/builds/check" '{"items":[{"code":"case_mini_sff"},{"code":"cpu_std"}]}'
$rules = Rules $r.Body.errors
Check "Thiếu linh kiện: báo required.motherboard, required.psu, required.ram, required.ssd" (
    ($rules -contains "required.motherboard") -and ($rules -contains "required.psu") -and
    ($rules -contains "required.ram") -and ($rules -contains "required.ssd")) "lỗi: $($rules -join ', ')"

$r = Call POST "/api/builds/check" '{"items":[]}'
Check "Danh sách rỗng: trả 400" ($r.Status -eq 400) "HTTP $($r.Status)"
$r = Call POST "/api/builds/check" '{"items":[{"code":"cpu_std","quantity":0}]}'
Check "Số lượng 0: trả 400" ($r.Status -eq 400) "HTTP $($r.Status)"
$r = Call POST "/api/builds/check" '{"items":[{"code":"khong_co"}]}'
Check "Mã linh kiện không tồn tại: trả 404" ($r.Status -eq 404) "HTTP $($r.Status)"
$r = Call POST "/api/builds/check" 'day khong phai JSON'
Check "Body không phải JSON: trả 400" ($r.Status -eq 400) "HTTP $($r.Status)"

Write-Host ""
$color = if ($script:fail -eq 0) { "Green" } else { "Red" }
Write-Host "Kết quả: $($script:pass) PASS, $($script:fail) FAIL" -ForegroundColor $color
if ($script:fail -gt 0) { exit 1 }
