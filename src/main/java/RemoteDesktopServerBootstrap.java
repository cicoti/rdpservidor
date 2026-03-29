import java.io.File;

import br.com.ctech.remote.server.ApplicationPaths;
import br.com.ctech.remote.server.RemoteDesktopServer;

public class RemoteDesktopServerBootstrap {

    public static void main(String[] args) {
        File baseDir = ApplicationPaths.getApplicationBaseDirectory();
        File logsDir = new File(baseDir, "logs");

        if (!logsDir.exists()) {
            boolean created = logsDir.mkdirs();
            System.out.println("Logs dir criada: " + created + " | caminho=" + logsDir.getAbsolutePath());
        } else {
            System.out.println("Logs dir já existe | caminho=" + logsDir.getAbsolutePath());
        }

        System.setProperty("app.base.dir", baseDir.getAbsolutePath());
        System.out.println("app.base.dir=" + System.getProperty("app.base.dir"));

        RemoteDesktopServer.main(args);
    }
}