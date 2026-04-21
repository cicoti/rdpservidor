#define MyAppName "S4ETech-RDP-Server"
#define MyAppVersion "1.1.0"
#define MyAppPublisher "s4etech"
#define MyAppURL "https://www.s4e.tech/br/"

[Setup]
AppId={{8D6E4F1A-2B77-4D91-9A4C-7F22D8A11001}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName=C:\{#MyAppName}
DefaultGroupName={#MyAppName}
OutputDir=C:\Users\silvi\OneDrive\Desktop
OutputBaseFilename=s4etech-rdp-server-instalador-110
SetupIconFile=C:\projetos\ctech\s4etech\remotedesktop\executavel\icon_remote_server.ico
Compression=lzma
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=admin
ArchitecturesInstallIn64BitMode=x64

[Languages]
Name: "brazilianportuguese"; MessagesFile: "compiler:Languages\BrazilianPortuguese.isl"

[Files]
Source: "C:\projetos\ctech\s4etech\remotedesktop\executavel\rdpservidor.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\projetos\ctech\s4etech\remotedesktop\executavel\icon_remote_server.ico"; DestDir: "{app}"; Flags: ignoreversion onlyifdoesntexist
Source: "C:\projetos\ctech\s4etech\remotedesktop\executavel\remote-desktop-server.properties"; DestDir: "{app}"; Flags: ignoreversion onlyifdoesntexist

; GStreamer saindo da pasta do empacotamento e indo para dentro da instalação
Source: "C:\projetos\ctech\s4etech\remotedesktop\executavel\gstreamer\1.0\msvc_x86_64\bin\*"; DestDir: "{app}\gstreamer\1.0\msvc_x86_64\bin"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "C:\projetos\ctech\s4etech\remotedesktop\executavel\gstreamer\1.0\msvc_x86_64\lib\gstreamer-1.0\*"; DestDir: "{app}\gstreamer\1.0\msvc_x86_64\lib\gstreamer-1.0"; Flags: ignoreversion recursesubdirs createallsubdirs

[Registry]
Root: HKLM; Subkey: "SYSTEM\CurrentControlSet\Control\Session Manager\Environment"; ValueType: expandsz; ValueName: "Path"; ValueData: "{olddata}"

[Icons]
Name: "{commondesktop}\S4ETech RDP Server"; Filename: "{app}\rdpservidor.exe"; IconFilename: "{app}\icon_remote_server.ico"; WorkingDir: "{app}"
Name: "{group}\S4ETech RDP Server"; Filename: "{app}\rdpservidor.exe"; IconFilename: "{app}\icon_remote_server.ico"; WorkingDir: "{app}"

[Code]
procedure AddToPath(const NewPath: string);
var
  Path: string;
begin
  if RegQueryStringValue(HKLM, 'SYSTEM\CurrentControlSet\Control\Session Manager\Environment', 'Path', Path) then
  begin
    if Pos(';' + LowerCase(NewPath) + ';', ';' + LowerCase(Path) + ';') = 0 then
      RegWriteStringValue(HKLM, 'SYSTEM\CurrentControlSet\Control\Session Manager\Environment', 'Path', Path + ';' + NewPath);
  end
  else
  begin
    RegWriteStringValue(HKLM, 'SYSTEM\CurrentControlSet\Control\Session Manager\Environment', 'Path', NewPath);
  end;
end;

procedure RefreshEnvironment();
var
  ErrorCode: Integer;
begin
  if not ShellExec('open', 'powershell.exe', '-NoProfile -ExecutionPolicy Bypass -Command "' +
    '[System.Environment]::SetEnvironmentVariable(''Path'', [System.Environment]::GetEnvironmentVariable(''Path'', ''Machine''), ''Process'')"',
    '', SW_HIDE, ewWaitUntilTerminated, ErrorCode) then
  begin
    MsgBox('Falha ao atualizar o ambiente. Código de erro: ' + IntToStr(ErrorCode), mbError, MB_OK);
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
  begin
    AddToPath(ExpandConstant('{app}\gstreamer\1.0\msvc_x86_64\bin'));
    RefreshEnvironment();
    MsgBox('Instalação do servidor concluída com sucesso!', mbInformation, MB_OK);
  end;
end;