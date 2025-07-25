package tech.konata.musicintegration.integration.ncm;

import java.io.File;

/**
 * @author IzumiiKonata
 * Date: 2025/7/1 20:05
 */
public class NCMConstants {

    public static final File CLOUDMUSIC_DIR = new File(getLocalAppData(), "NetEase\\CloudMusic");
    public static final File CLOUDMUSIC_ELOG_PATH = new File(CLOUDMUSIC_DIR, "cloudmusic.elog");

    private static File getLocalAppData() {

        String localAppDataPath = System.getenv("LOCALAPPDATA");
        if (localAppDataPath != null) {
            return new File(localAppDataPath);
        } else {
            String userHome = System.getProperty("user.home");
            return new File(userHome + "\\AppData\\Local");
        }

    }

}
