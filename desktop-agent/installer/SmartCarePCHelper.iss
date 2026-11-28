; Bộ cài SmartCarePC Helper cho từng người dùng, không cần admin (Inno Setup 6.3+).
; Build: publish trước (xem README), rồi mở file này bằng Inno Setup Compiler, hoặc:
;   "C:\Program Files (x86)\Inno Setup 6\ISCC.exe" installer\SmartCarePCHelper.iss
; Ra file installer\Output\SmartCarePCHelper-Setup-1.0.0.exe. File chưa ký số: Windows SmartScreen sẽ cảnh báo.

#define AppName "SmartCarePC Helper"
#define AppVersion "1.0.0"
#define AppExe "SmartCarePCHelper.exe"

[Setup]
AppId={{6F1C2A0E-4B7D-4E1B-9C3A-5D2E8F7A9B10}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher=SmartCarePC research project
; Cài cho riêng người dùng này: không hỏi quyền admin
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=
DefaultDirName={localappdata}\Programs\{#AppName}
DisableDirPage=yes
DisableProgramGroupPage=yes
DefaultGroupName={#AppName}
UninstallDisplayName={#AppName}
UninstallDisplayIcon={app}\{#AppExe}
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
MinVersion=10.0.17763
; Đóng app đang chạy trước khi cài đè / gỡ (cùng tên mutex với WindowsIntegration.MutexName)
AppMutex=SmartCarePCHelper.Running
CloseApplications=yes
OutputDir=Output
OutputBaseFilename=SmartCarePCHelper-Setup-{#AppVersion}
Compression=lzma2
SolidCompression=yes
WizardStyle=modern

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "startup"; Description: "Start SmartCarePC Helper when Windows starts"; GroupDescription: "Reminders:"

[Files]
Source: "..\publish\win-x64\{#AppExe}"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{userprograms}\{#AppName}"; Filename: "{app}\{#AppExe}"

[Registry]
; URL scheme smartcarepc:// trong HKCU: web mở link này để gửi lịch sang app
Root: HKCU; Subkey: "Software\Classes\smartcarepc"; ValueType: string; ValueName: ""; ValueData: "URL:{#AppName}"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\Classes\smartcarepc"; ValueType: string; ValueName: "URL Protocol"; ValueData: ""
Root: HKCU; Subkey: "Software\Classes\smartcarepc\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: """{app}\{#AppExe}"",0"
Root: HKCU; Subkey: "Software\Classes\smartcarepc\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\{#AppExe}"" ""%1"""
; Khởi động cùng Windows
Root: HKCU; Subkey: "Software\Microsoft\Windows\CurrentVersion\Run"; ValueType: string; ValueName: "{#AppName}"; ValueData: """{app}\{#AppExe}"" --background"; Tasks: startup; Flags: uninsdeletevalue

[Run]
Filename: "{app}\{#AppExe}"; Description: "Start {#AppName} now"; Flags: nowait postinstall skipifsilent

[UninstallRun]
; Xoá đăng ký thông báo và các khoá app tự ghi (kể cả mục khởi động bật từ menu khay)
Filename: "{app}\{#AppExe}"; Parameters: "--uninstall"; Flags: runhidden waituntilterminated; RunOnceId: "AgentCleanup"

[Code]
// Gỡ xong hỏi có giữ dữ liệu không (lịch nhắc, lịch sử số đo trong %LOCALAPPDATA%\SmartCarePC)
procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  DataDir: String;
begin
  if CurUninstallStep = usPostUninstall then
  begin
    DataDir := ExpandConstant('{localappdata}\SmartCarePC');
    if DirExists(DataDir) then
      if (not UninstallSilent) and (MsgBox('Also delete your reminder plan and the health readings saved on this PC?', mbConfirmation, MB_YESNO) = IDYES) then
        DelTree(DataDir, True, True, True);
  end;
end;
