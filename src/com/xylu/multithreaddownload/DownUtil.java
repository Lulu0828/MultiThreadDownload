package com.xylu.multithreaddownload;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownUtil {
	
	/** 
     * 需要下载资源的地址 
     */
    private String urlStr;
    /** 
     * 下载的文件 
     */  
    private File localFile;
    /** 
     * 需要下载文件的存放的本地文件夹路径 
     */  
    private String dirStr;  
    /** 
     * 存储到本地的文件名 
     */  
    private String filename;
  
    /** 
     * 开启的线程数量 
     */  
    private int threadCount;
    /** 
     * 下载文件的大小 
     */  
    private long fileSize;
    /** 
     * 下载线程对象
     */  
    private DownloadThread[] threads = new DownloadThread[20];
    
    public DownUtil(String urlStr, String dirStr, String filename, int threadCount) {  
        this.urlStr = urlStr;  
        this.dirStr = dirStr;
        this.filename = filename;
        this.threadCount = threadCount;  
    }
    
    public void download() throws Exception {
    	
        createFileByUrl();
  
        /** 
         * 计算每个线程需要下载的数据长度 
         */  
        long block = fileSize % threadCount == 0 ? fileSize / threadCount : fileSize / threadCount + 1;
  
        for (int i = 0; i < threadCount; i++) {
        	//每条线程下载的起始位置
            long start = i * block;
            long end = start + block >= fileSize ? (fileSize - 1) : (start + block - 1);
  
            threads[i] = new DownloadThread(new URL(urlStr), localFile, start, end);
            threads[i].start();
            
        }
  
    }

	private void createFileByUrl() throws Exception {
		URL url = new URL(urlStr);  
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();  
        conn.setConnectTimeout(5 * 1000);  
        conn.setRequestMethod("GET");  
        conn.setRequestProperty(  
                "Accept",  
                "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, "
                + "application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, "
                + "application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, "
                + "application/msword, */*");  
        conn.setRequestProperty("Accept-Language", "zh-CN");  
        conn.setRequestProperty("Referer", urlStr);  
        conn.setRequestProperty("Charset", "UTF-8");  
        conn.setRequestProperty(  
                "User-Agent",  
                "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; "
                + ".NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");  
        conn.setRequestProperty("Connection", "Keep-Alive");
        
    	// 获取文件大小
        this.fileSize = conn.getContentLength();
        conn.disconnect();
        if (fileSize <= 0)
            throw new RuntimeException("the file that you download has a wrong size ... ");  
        File dir = new File(dirStr);
        if (!dir.exists())
            dir.mkdirs();
        this.localFile = new File(dir, filename);
        RandomAccessFile raf = new RandomAccessFile(this.localFile, "rw");  
        raf.setLength(fileSize);
        raf.close();
  
        System.out.println("需要下载的文件大小为 :" + this.fileSize + " , 存储位置为： "  + dirStr + "/" + filename);
	}
	
	//获取下载完成百分比
	public double getCompleteRate() {
		int sumSize = 0;
		for (int i = 0; i < threadCount; i++) {
        	sumSize += threads[i].length;
        }
		return sumSize * 1.0 / fileSize;
	}
	
	private class DownloadThread extends Thread {

		 /**  
         * 下载文件的URL 
         */  
        private URL url;  
        /** 
         * 存储到本地的文件名
         */  
        private File localFile;
        /** 
         * 开始的位置 
         */  
        private Long startPos;  
        /** 
         * 结束位置 
         */  
        private Long endPos;
        /** 
         * 目前所处位置 
         */  
        private Long currentPos;
        /** 
         * 已下载字节数
         */  
        public int length = 0;
        
		public DownloadThread(URL url, File localFile, long start, long end) {
			this.url = url;  
            this.localFile = localFile;  
            this.startPos = start;
            this.currentPos = start;
            this.endPos = end;
		}

		@Override
		public void run() {
			System.out.println(Thread.currentThread().getName() + "开始下载...");  
            try {  
            	HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.setConnectTimeout(15 * 1000);  
                conn.setRequestMethod("GET");  
                conn.setRequestProperty(  
                        "Accept",  
                        "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, "
                        + "application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, "
                        + "application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, "
                        + "application/msword, */*");
                conn.setRequestProperty("Accept-Language", "zh-CN");  
                conn.setRequestProperty("Referer", url.toString());  
                conn.setRequestProperty("Charset", "UTF-8");
                // 设置当前线程下载的起点，终点 
                conn.setRequestProperty("Range", "bytes=" + startPos + "-"  + endPos);
                conn.setRequestProperty(
                        "User-Agent",
                        "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; "
                        + ".NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");  
                conn.setRequestProperty("Connection", "Keep-Alive");
                
                if (conn.getResponseCode() == 206) {
                	BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
                    int len = 0;  
                    byte[] buf = new byte[1024];

                    RandomAccessFile raf = new RandomAccessFile(localFile, "rw");
                    raf.seek(startPos);
                    //开始循环以流的形式读写文件
                    while (currentPos < endPos) {
                        len = bis.read(buf, 0, 1024);
                        if (len == -1) {
                            break;
                        }
                        raf.write(buf, 0, len);
                        currentPos = currentPos + len;
                        length = length + len;
                    }
                    raf.close();
                    bis.close();
                    System.out.println(Thread.currentThread().getName() + "完成下载  ： " + startPos + " -- " + endPos);
                } else {
                    throw new RuntimeException("url that you conneted has error ...");  
                }  
            } catch (IOException e) {  
                e.printStackTrace();  
            }
		}
		
	}

}
