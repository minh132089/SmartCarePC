# Gửi một lịch thử sang SmartCarePC Helper, thay cho nút "Send my plan to the desktop app" (web chưa có nút này).
# Chạy trên Windows sau khi đã cài app:
#   powershell -ExecutionPolicy Bypass -File tools\send-test-plan.ps1              # dust tới hạn hôm nay
#   powershell -ExecutionPolicy Bypass -File tools\send-test-plan.ps1 -DustInDays 3
#   powershell -ExecutionPolicy Bypass -File tools\send-test-plan.ps1 -Study P07   # chế độ thử nghiệm (có gửi sự kiện)
#   powershell -ExecutionPolicy Bypass -File tools\send-test-plan.ps1 -Bad         # link hỏng: app phải bỏ qua, không crash
param(
  [int]$DustInDays = 0,
  [string]$Study = "",
  [string]$Origin = "https://smartcarepc.pages.dev",
  [switch]$Bad
)
$today = Get-Date
$plan = [ordered]@{
  v = 1
  pc = [ordered]@{ machine = "standard_tower"; cooler = "air"; hasGpu = $true }
  dates = [ordered]@{
    dust = $today.AddDays($DustInDays).ToString("yyyy-MM-dd")
    paste = $today.AddMonths(30).ToString("yyyy-MM-dd")
    check = $today.AddMonths(1).ToString("yyyy-MM-dd")
  }
  completed = [ordered]@{ dust = $today.AddMonths(-6).ToString("yyyy-MM-dd") }
  origin = $Origin
}
if ($Study) { $plan.study = [ordered]@{ code = $Study; phase = "before" } }
$json = $plan | ConvertTo-Json -Compress -Depth 4
$b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($json)).TrimEnd('=').Replace('+', '-').Replace('/', '_')
$link = if ($Bad) { "smartcarepc://plan?d=" + ("A" * 9000) } else { "smartcarepc://plan?d=$b64" }
Write-Host $json
Start-Process $link
