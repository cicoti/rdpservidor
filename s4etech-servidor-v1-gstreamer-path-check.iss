#define MyAppName "S4ETech-RDP-Server"
#define MyAppVersion "1.3.0"
#define MyAppPublisher "s4etech"
#define MyAppURL "https://www.s4e.tech/br/"
#define GStreamerMsiName "gstreamer-1.0-msvc-x86_64-1.24.13.msi"

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
OutputBaseFilename=s4etech-rdp-server-instalador-130
SetupIconFile=C:\projetos\ctech\s4etech\remotedesktop\executavel\icon_remote_server.ico
Compression=lzma
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=admin
ArchitecturesInstallIn64BitMode=x64
ChangesEnvironment=yes

[Languages]
Name: "brazilianportuguese"; MessagesFile: "compiler:Languages\BrazilianPortuguese.isl"

[Files]
Source: "C:\projetos\ctech\s4etech\remotedesktop\executavel\rdpservidor.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\projetos\ctech\s4etech\remotedesktop\executavel\icon_remote_server.ico"; DestDir: "{app}"; Flags: ignoreversion onlyifdoesntexist

Source: "C:\projetos\ctech\s4etech\remotedesktop\executavel\{#GStreamerMsiName}"; DestDir: "{tmp}"; Flags: deleteafterinstall; Check: NeedsGStreamerInstall

[Icons]
Name: "{commondesktop}\S4ETech RDP Server"; Filename: "{app}\rdpservidor.exe"; IconFilename: "{app}\icon_remote_server.ico"; WorkingDir: "{app}"
Name: "{group}\S4ETech RDP Server"; Filename: "{app}\rdpservidor.exe"; IconFilename: "{app}\icon_remote_server.ico"; WorkingDir: "{app}"

[Code]
var
  GStreamerCheckDone: Boolean;
  GStreamerAlreadyAvailable: Boolean;

function GetGStreamerBinPath(): string;
begin
  Result := 'C:\gstreamer\1.0\msvc_x86_64\bin';
end;

function GetGStreamerExePath(): string;
begin
  Result := GetGStreamerBinPath() + '\gst-launch-1.0.exe';
end;

function IsGStreamerAvailableInDefaultPath(): Boolean;
begin
  Result := FileExists(GetGStreamerExePath());
end;

function IsGStreamerAvailableInPathNoCache(): Boolean;
var
  ResultCode: Integer;
begin
  Result :=
    Exec(
      ExpandConstant('{cmd}'),
      '/C gst-launch-1.0.exe --version >nul 2>&1',
      '',
      SW_HIDE,
      ewWaitUntilTerminated,
      ResultCode
    ) and (ResultCode = 0);
end;

function IsGStreamerAvailable(): Boolean;
begin
  if not GStreamerCheckDone then
  begin
    GStreamerCheckDone := True;
    GStreamerAlreadyAvailable :=
      IsGStreamerAvailableInDefaultPath() or IsGStreamerAvailableInPathNoCache();
  end;

  Result := GStreamerAlreadyAvailable;
end;

function NeedsGStreamerInstall(): Boolean;
begin
  Result := not IsGStreamerAvailable();
end;

procedure AddToPath(const NewPath: string);
var
  CurrentPath: string;
  NewValue: string;
begin
  if RegQueryStringValue(
    HKLM,
    'SYSTEM\CurrentControlSet\Control\Session Manager\Environment',
    'Path',
    CurrentPath
  ) then
  begin
    if Pos(';' + LowerCase(NewPath) + ';', ';' + LowerCase(CurrentPath) + ';') = 0 then
    begin
      if CurrentPath = '' then
        NewValue := NewPath
      else
        NewValue := CurrentPath + ';' + NewPath;

      RegWriteExpandStringValue(
        HKLM,
        'SYSTEM\CurrentControlSet\Control\Session Manager\Environment',
        'Path',
        NewValue
      );
    end;
  end
  else
  begin
    RegWriteExpandStringValue(
      HKLM,
      'SYSTEM\CurrentControlSet\Control\Session Manager\Environment',
      'Path',
      NewPath
    );
  end;
end;

function InstallGStreamerMsi(): Boolean;
var
  ResultCode: Integer;
  MsiPath: string;
begin
  ResultCode := -1;
  MsiPath := ExpandConstant('{tmp}\{#GStreamerMsiName}');

  Result :=
    Exec(
      ExpandConstant('{sys}\msiexec.exe'),
      '/i ' + AddQuotes(MsiPath) + ' /passive /norestart',
      '',
      SW_SHOW,
      ewWaitUntilTerminated,
      ResultCode
    );

  if Result then
    Result := (ResultCode = 0) or (ResultCode = 3010);

  if not Result then
  begin
    MsgBox(
      'Não foi possível instalar o GStreamer.' + #13#10 +
      'Código retornado pelo instalador: ' + IntToStr(ResultCode),
      mbError,
      MB_OK
    );
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
  begin
    if NeedsGStreamerInstall() then
    begin
      if not InstallGStreamerMsi() then
      begin
        MsgBox(
          'A instalação do S4ETech RDP Server foi concluída, mas o GStreamer não foi instalado corretamente.' + #13#10 +
          'O servidor pode não funcionar até que o GStreamer seja instalado manualmente.',
          mbError,
          MB_OK
        );

        Exit;
      end;
    end;

    if IsGStreamerAvailableInDefaultPath() then
      AddToPath(GetGStreamerBinPath());

    if not IsGStreamerAvailableInDefaultPath() and not IsGStreamerAvailableInPathNoCache() then
    begin
      MsgBox(
        'O GStreamer não foi localizado após a instalação.' + #13#10 +
        'Verifique se o arquivo gst-launch-1.0.exe existe em:' + #13#10 +
        GetGStreamerExePath(),
        mbError,
        MB_OK
      );

      Exit;
    end;

    MsgBox('Instalação do servidor concluída com sucesso!', mbInformation, MB_OK);
  end;
end;