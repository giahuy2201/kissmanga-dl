package com.giahuy2201.manga_dl;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class NetTruyenTest {
	@Test
	public void testImageSetReferer(){
		try
		{
//			URL url = new URL("http://imageinstant.com/data/images/36176/668834/001.jpg");
			URL url = new URL("http://imageinstant.com/data/images/36176/668834/001.jpg");
			URLConnection request = url.openConnection();
//			request.setRequestProperty("user-agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
//			request.setRequestProperty("cookie", "cf_clearance=d555eebfdd40ac44e3397eea80e0bd698102357d-1608523371-0-150; cf_chl_prog=x19; cf_chl_1=31989433693a072; __cfduid=d44de31aa8d2f8a7bcc9567f552b86f8d1608523371; "); // Hard-coded correct cookie value
			request.setRequestProperty("referer","http://www.nettruyen.com");

			InputStream initialStream = request.getInputStream();

			byte[] buffer = initialStream.readAllBytes();

			File targetFile = new File("1234.jpg");
			OutputStream outStream = new FileOutputStream(targetFile);
			outStream.write(buffer);
		} catch (Exception exception)
		{
			exception.printStackTrace();
		}
	}
}

