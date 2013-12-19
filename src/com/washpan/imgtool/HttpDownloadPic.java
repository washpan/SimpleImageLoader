package com.washpan.imgtool;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.Build;

/**
 * 
 * @author washpan 190848972@qq.com
 * 
 */
public class HttpDownloadPic {
	public static final int IO_BUFFER_SIZE = 32 * 1024;
	public static final int HTTP_CONNECT_TIMEOUT = 20000;
	public static final int HTTP_READ_TIMEOUT = 20000;
	public static final String FILE_SUFFIX = ".tmp";

	public HttpDownloadPic() {
	}

	public long downloadPic(File cacheFile, String urlString)
			throws IOException {
		// 如果文件存在，则认为是已经下载完成的，不需要下载。
		if (cacheFile != null && cacheFile.exists()) {
			cacheFile.setLastModified(System.currentTimeMillis());
			return 0;
		}
		File tmpFile = new File(cacheFile.getAbsolutePath() + FILE_SUFFIX);
		if (!tmpFile.exists()) {
			tmpFile.createNewFile();
		}
		download(tmpFile, urlString);
		tmpFile.renameTo(cacheFile);
		cacheFile.setLastModified(System.currentTimeMillis());
		return cacheFile.length();
	}

	private void download(File tmpFile, String urlString)
			throws IOException {
		FileOutputStream out = null;
		InputStream is = null;
		try {
			HttpURLConnection urlConnection = getURLConnection(urlString);

			is = urlConnection.getInputStream();
			out = new FileOutputStream(tmpFile);

			byte[] buffer = new byte[IO_BUFFER_SIZE];
			int b = -1;
			while ((b = is.read(buffer)) != -1) {
				out.write(buffer, 0, b);
			}
		} catch (Exception e) {
				throw new IOException("download fail");
		} finally {
			try {
				if (is != null) {
					is.close();
				}
				if (out != null) {
					out.flush();
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}


	/**
	 * getURLConnection
	 * 
	 * @param urlString
	 * @param host
	 * @return
	 * @throws IOException
	 */
	private static HttpURLConnection getURLConnection(String urlString) throws IOException {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
		}
		HttpURLConnection conn = null;
		URL url;
		url = new URL(urlString);
		conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(HTTP_CONNECT_TIMEOUT);
			conn.setReadTimeout(HTTP_READ_TIMEOUT);
		return conn;
	}
}
