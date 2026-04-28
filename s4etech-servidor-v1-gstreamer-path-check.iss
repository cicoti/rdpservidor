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
function GetGStreamerInstallBasePath(): string;
begin
  Result := 'C:\gstreamer';
end;

function GetGStreamerRootPath(): string;
begin
  Result := GetGStreamerInstallBasePath() + '\1.0\msvc_x86_64';
end;

function GetGStreamerBinPath(): string;
begin
  Result := GetGStreamerRootPath() + '\bin';
end;

function GetGStreamerExePath(): string;
begin
  Result := GetGStreamerBinPath() + '\gst-launch-1.0.exe';
end;

function GetGStreamerInspectPath(): string;
begin
  Result := GetGStreamerBinPath() + '\gst-inspect-1.0.exe';
end;

procedure ClearGStreamerRegistryCache();
begin
  DeleteFile(ExpandConstant('{localappdata}\gstreamer-1.0\registry.x86_64.bin'));
  DeleteFile(ExpandConstant('{localappdata}\gstreamer-1.0\registry.i686.bin'));
end;

function IsGStreamerAvailableInDefaultPath(): Boolean;
begin
  Result :=
    FileExists(GetGStreamerExePath()) and
    FileExists(GetGStreamerInspectPath());
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

function IsPluginAvailableInDefaultPath(const PluginName: string): Boolean;
var
  ResultCode: Integer;
begin
  Result := False;

  if not FileExists(GetGStreamerInspectPath()) then
    Exit;

  Result :=
    Exec(
      GetGStreamerInspectPath(),
      PluginName,
      '',
      SW_HIDE,
      ewWaitUntilTerminated,
      ResultCode
    ) and (ResultCode = 0);
end;

function IsPluginAvailableInPathNoCache(const PluginName: string): Boolean;
var
  ResultCode: Integer;
begin
  Result :=
    Exec(
      ExpandConstant('{cmd}'),
      '/C gst-inspect-1.0.exe ' + PluginName + ' >nul 2>&1',
      '',
      SW_HIDE,
      ewWaitUntilTerminated,
      ResultCode
    ) and (ResultCode = 0);
end;

function AreRequiredGStreamerPluginsAvailableInDefaultPath(): Boolean;
begin
  Result :=
    IsPluginAvailableInDefaultPath('videotestsrc') and
    IsPluginAvailableInDefaultPath('videoconvert') and
    IsPluginAvailableInDefaultPath('videoscale') and
    IsPluginAvailableInDefaultPath('queue') and
    IsPluginAvailableInDefaultPath('filesink') and
    IsPluginAvailableInDefaultPath('x264enc') and
    IsPluginAvailableInDefaultPath('h264parse') and
    IsPluginAvailableInDefaultPath('mp4mux') and
    IsPluginAvailableInDefaultPath('dx9screencapsrc') and
    IsPluginAvailableInDefaultPath('gdiscreencapsrc') and
    IsPluginAvailableInDefaultPath('d3d11screencapturesrc');
end;

function AreRequiredGStreamerPluginsAvailableInPathNoCache(): Boolean;
begin
  Result :=
    IsPluginAvailableInPathNoCache('videotestsrc') and
    IsPluginAvailableInPathNoCache('videoconvert') and
    IsPluginAvailableInPathNoCache('videoscale') and
    IsPluginAvailableInPathNoCache('queue') and
    IsPluginAvailableInPathNoCache('filesink') and
    IsPluginAvailableInPathNoCache('x264enc') and
    IsPluginAvailableInPathNoCache('h264parse') and
    IsPluginAvailableInPathNoCache('mp4mux') and
    IsPluginAvailableInPathNoCache('dx9screencapsrc') and
    IsPluginAvailableInPathNoCache('gdiscreencapsrc') and
    IsPluginAvailableInPathNoCache('d3d11screencapturesrc');
end;

function AreRequiredGStreamerPluginsAvailable(): Boolean;
begin
  Result :=
    IsGStreamerAvailableInDefaultPath() and
    AreRequiredGStreamerPluginsAvailableInDefaultPath();
end;

function IsGStreamerAvailable(): Boolean;
begin
  ClearGStreamerRegistryCache();

  Result :=
    IsGStreamerAvailableInDefaultPath() and
    AreRequiredGStreamerPluginsAvailableInDefaultPath();
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

function RunGStreamerMsi(const ExtraParams: string): Boolean;
var
  ResultCode: Integer;
  MsiPath: string;
  LogPath: string;
  Params: string;
begin
  ResultCode := -1;
  MsiPath := ExpandConstant('{tmp}\{#GStreamerMsiName}');
  LogPath := ExpandConstant('{tmp}\gstreamer-rdp-server-install.log');

  Params :=
    '/i ' + AddQuotes(MsiPath) +
    ' INSTALLDIR=' + AddQuotes(GetGStreamerInstallBasePath()) +
    ' ADDLOCAL=ALL' +
    ExtraParams +
    ' /passive' +
    ' /norestart' +
    ' /L*v ' + AddQuotes(LogPath);

  Result :=
    Exec(
      ExpandConstant('{sys}\msiexec.exe'),
      Params,
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
      'Não foi possível instalar ou reparar o GStreamer.' + #13#10 +
      'Código retornado pelo instalador: ' + IntToStr(ResultCode) + #13#10 +
      'Log do MSI: ' + LogPath,
      mbError,
      MB_OK
    );
  end;
end;

function InstallGStreamerMsi(): Boolean;
begin
  ClearGStreamerRegistryCache();

  Result := RunGStreamerMsi('');

  ClearGStreamerRegistryCache();

  if Result and not AreRequiredGStreamerPluginsAvailable() then
  begin
    Result := RunGStreamerMsi(' REINSTALL=ALL REINSTALLMODE=vomus');
    ClearGStreamerRegistryCache();
  end;
end;

procedure ShowMissingPluginsWarning();
var
  MessageText: string;
begin
  MessageText :=
    'O GStreamer foi localizado, mas um ou mais plugins necessários não estão disponíveis no caminho padrão:' + #13#10 +
    GetGStreamerRootPath() + #13#10 + #13#10 +
    'A instalação precisa conter todos estes componentes/plugins:' + #13#10 + #13#10 +
    '  videotestsrc' + #13#10 +
    '  videoconvert' + #13#10 +
    '  videoscale' + #13#10 +
    '  queue' + #13#10 +
    '  filesink' + #13#10 +
    '  x264enc' + #13#10 +
    '  h264parse' + #13#10 +
    '  mp4mux' + #13#10 +
    '  dx9screencapsrc' + #13#10 +
    '  gdiscreencapsrc' + #13#10 +
    '  d3d11screencapturesrc' + #13#10 + #13#10 +
    'Se já existir uma instalação do GStreamer em outro drive, como D:, remova essa instalação pelo Windows e execute este instalador novamente.' + #13#10 + #13#10 +
    'Comando de teste:' + #13#10 +
    'C:\gstreamer\1.0\msvc_x86_64\bin\gst-inspect-1.0.exe x264enc';

  MsgBox(MessageText, mbError, MB_OK);
end;

procedure ShowGStreamerFoundOutsideDefaultPathWarning();
begin
  if (not IsGStreamerAvailableInDefaultPath()) and IsGStreamerAvailableInPathNoCache() then
  begin
    MsgBox(
      'Foi localizado um GStreamer no PATH do Windows, mas não no caminho padrão esperado:' + #13#10 +
      GetGStreamerRootPath() + #13#10 + #13#10 +
      'Este instalador tentará instalar o GStreamer em:' + #13#10 +
      GetGStreamerInstallBasePath() + #13#10 + #13#10 +
      'Se o MSI mantiver uma instalação antiga em outro drive, remova o GStreamer pelo Windows e execute o instalador novamente.',
      mbInformation,
      MB_OK
    );
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
  begin
    ClearGStreamerRegistryCache();

    ShowGStreamerFoundOutsideDefaultPathWarning();

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

    ClearGStreamerRegistryCache();

    if IsGStreamerAvailableInDefaultPath() then
      AddToPath(GetGStreamerBinPath());

    if not IsGStreamerAvailableInDefaultPath() then
    begin
      MsgBox(
        'O GStreamer não foi localizado no caminho padrão após a instalação.' + #13#10 +
        'Verifique se o arquivo gst-launch-1.0.exe existe em:' + #13#10 +
        GetGStreamerExePath() + #13#10 + #13#10 +
        'Se ele foi instalado em outro drive, remova a instalação antiga pelo Windows e execute este instalador novamente.',
        mbError,
        MB_OK
      );

      Exit;
    end;

    if not AreRequiredGStreamerPluginsAvailable() then
    begin
      ShowMissingPluginsWarning();
      Exit;
    end;

    MsgBox('Instalação do servidor concluída com sucesso!', mbInformation, MB_OK);
  end;
end;
