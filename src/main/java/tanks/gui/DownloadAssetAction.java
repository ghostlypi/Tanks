package tanks.gui;

import tanks.Game;
import tanks.gui.ScreenElement.Notification;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A notification action that downloads an asset file from a URL in the background.
 * It updates the notification text with live percentage progress and completes smoothly.
 */
public class DownloadAssetAction implements NotificationAction
{
    private final String downloadUrl;
    private final String destPath;
    private final String assetName;
    private final Runnable onComplete;

    /**
     * Constructs a DownloadAssetAction for the specified download URL and destination.
     *
     * @param downloadUrl The HTTP URL of the asset to download.
     * @param destPath    The absolute file system path where the downloaded file should be saved.
     * @param assetName   The user-friendly name of the asset to display in the progress messages.
     */
    public DownloadAssetAction(String downloadUrl, String destPath, String assetName)
    {
        this(downloadUrl, destPath, assetName, null);
    }

    /**
     * Constructs a DownloadAssetAction for the specified download URL, destination, and completion callback.
     *
     * @param downloadUrl The HTTP URL of the asset to download.
     * @param destPath    The absolute file system path where the downloaded file should be saved.
     * @param assetName   The user-friendly name of the asset to display in the progress messages.
     * @param onComplete  An optional callback to run upon successful download completion.
     */
    public DownloadAssetAction(String downloadUrl, String destPath, String assetName, Runnable onComplete)
    {
        this.downloadUrl = downloadUrl;
        this.destPath = destPath;
        this.assetName = assetName;
        this.onComplete = onComplete;
    }

    @Override
    public boolean onClick(Notification notification)
    {
        notification.progressActive = true;
        notification.setText("Downloading " + this.assetName + "... 0%");

        new Thread(() ->
        {
            try
            {
                URL url = new URL(this.downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK)
                {
                    throw new Exception("HTTP response code: " + responseCode);
                }

                int fileLength = conn.getContentLength();
                File destFile = new File(this.destPath);

                File parentFile = destFile.getParentFile();
                if (parentFile != null && !parentFile.exists())
                {
                    parentFile.mkdirs();
                }

                try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
                     FileOutputStream out = new FileOutputStream(destFile))
                {
                    byte[] data = new byte[4096];
                    int count;
                    int total = 0;

                    while ((count = in.read(data, 0, 4096)) != -1)
                    {
                        out.write(data, 0, count);
                        total += count;

                        if (fileLength > 0)
                        {
                            int percent = (int) ((total * 100L) / fileLength);
                            notification.setText("Downloading " + this.assetName + "... " + percent + "%");
                        }
                    }
                }

                notification.setText("\u00A7000200000255" + this.assetName + " downloaded!");

                if (this.onComplete != null)
                {
                    this.onComplete.run();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                notification.setText("\u00A7200000000255Failed to download " + this.assetName + "!");
            }
            finally
            {
                notification.progressActive = false;
                notification.age = 0;
            }
        }).start();

        return false;
    }
}
