package com.asuala.mock.m3u8.download;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSONObject;
import com.asuala.mock.m3u8.Exception.M3u8Exception;
import com.asuala.mock.m3u8.listener.DownloadListener;
import com.asuala.mock.m3u8.utils.Constant;
import com.asuala.mock.m3u8.utils.MediaFormat;
import com.asuala.mock.m3u8.utils.StringUtils;
import com.asuala.mock.service.RecordPageService;
import com.asuala.mock.service.RecordService;
import com.asuala.mock.task.CommonTask;
import com.asuala.mock.utils.CacheUtils;
import com.asuala.mock.utils.ThreadPoolExecutorUtils;
import com.asuala.mock.vo.FixedLengthQueue;
import com.asuala.mock.vo.RecordPage;
import com.asuala.mock.websocket.HttpAuthHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLHandshakeException;
import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.asuala.mock.m3u8.utils.Constant.*;

/**
 * @author liyaling
 * @email ts_liyaling@qq.com
 * @date 2019/12/14 16:05
 */
@Slf4j
public class M3u8DownloadFactory {


    private static final int SUCESS = 1;
    private static final int FAIL = 0;

    private M3u8Download m3u8Download;

//    private


    /**
     *
     * 解决java不支持AES/CBC/PKCS7Padding模式解密
     *
     */
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Data
    public class M3u8Download {

        private RecordService recordService;
        private RecordPageService recordPageService;
//        private AnalysisDownUrlUtils analysisDownUrlUtils;
//        //优化内存占用
//        private static final BlockingQueue<byte[]> BLOCKING_QUEUE = new LinkedBlockingQueue<>();
//        public void setThreadCount(int threadCount) {
//            if (BLOCKING_QUEUE.size() < threadCount) {
//                for (int i = BLOCKING_QUEUE.size(); i < threadCount * Constant.FACTOR; i++) {
//                    try {
//                        BLOCKING_QUEUE.put();
//                    } catch (InterruptedException ignored) {
//                    }
//                }
//            }
//            this.threadCount = threadCount;
//        }

        private Map<Integer, RecordPage> pageMap = new HashMap<>();

        //要下载的m3u8链接
        private final String DOWNLOADURL;

        private Long id;

        //线程数
        private int threadCount = 1;

        //重试次数
        private int retryCount = 30;

        //链接连接超时时间（单位：毫秒）
        private long timeoutMillisecond = 1000L;

        //合并后的文件存储目录
        private String dir;

        //分片文件路径
        private String pageDir;

        //合并后的视频文件名称
        private String fileName;
        private String picUrl;

        //已完成ts片段个数
        private AtomicInteger finishedCount = new AtomicInteger(0);

        //失败ts片段个数
        private AtomicInteger failCount = new AtomicInteger(0);

        //解密算法名称
        private String method;

        //密钥
        private String key = "";

        //密钥字节
        private byte[] keyBytes = new byte[16];

        //key是否为字节
        private boolean isByte = false;

        //IV
        private String iv = "";

        //所有ts片段下载链接
        private Set<String> tsSet = new LinkedHashSet<>();

        //解密后的片段
        private Set<File> finishedFiles = new ConcurrentSkipListSet<>(Comparator.comparingInt(o -> Integer.parseInt(o.getName().replace(fileName + "-", "").replace(".xyz", ""))));

        //已经下载的文件大小
        private BigDecimal downloadBytes = new BigDecimal(0);

//        private AtomicInteger skipPage = new AtomicInteger(0);

        private Lock lock = new ReentrantLock();

        private boolean stopCosumer = false;

        //自定义请求头
        private Map<String, Object> requestHeaderMap = new HashMap<>();
        ;

        //监听事件
        private Set<DownloadListener> listenerSet = new HashSet<>(5);

        //代理设置
        private Proxy proxy;

        public void setProxy(int port) {
            this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", port));
        }

        public void setProxy(String address, int port) {
            this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(address, port));
        }

        public void setProxy(Proxy.Type type, String address, int port) {
            this.proxy = new Proxy(type, new InetSocketAddress(address, port));
        }

        //是否加密
        private boolean encryption = false;

        //总片段数
        private int page = 0;

        private ExecutorService fixedThreadPool;

        private Thread mergeThread;

        //开始时间
        private LocalDateTime startTime;

        private Set<Integer> overPage;


        public void shutdownNow() {
//            lock.lock();
//            try {
//                if (stopCosumer) {
//                    return;
//                }
            stopCosumer = true;

//            fixedThreadPool.shutdownNow();
            while (null != fixedThreadPool) {
                try {
                    if (fixedThreadPool.awaitTermination(1, TimeUnit.SECONDS)) break;
                } catch (InterruptedException e) {
                    log.error("{} 等待线池池结束错误", fileName);
                }
                // 等待直到线程池结束
            }
//            recordPageService.savePages(pageMap.values(), id);
            if (pageMap.size() > 0) {
                recordPageService.saveValues(pageMap.values());
            }
//            } finally {
//                lock.unlock();
//            }

        }

        /**
         * 开始下载视频
         */
        public void start() {
            if (!CacheUtils.downFlag(null)) {
                log.debug("未到可以下载的时间");
                stopDown();
                return;
            }
            try {
                checkField();
            } catch (Exception e) {
                log.error("地址检测失败 {}", fileName);
                recordService.deleteRecord(id, fileName);
                stopDown();
                return;
            }
            String tsUrl = getTsUrl();
            if (StringUtils.isEmpty(tsUrl)) {
                log.info("《{}》不需要解密", fileName);
            } else {
                encryption = true;
                log.warn("《{}》 需要解密", fileName);
            }
            startDownload();
        }


        /**
         * 下载视频
         */
        private void startDownload() {
            //检测文件夹是否创建
            File file = new File(dir);
            if (!file.exists()) {
                file.mkdirs();
            }
            file = new File(pageDir);
            if (!file.exists()) {
                file.mkdirs();
            }
//            FixedLengthQueue session = WsSessionManager.get("1");
            //线程池
            fixedThreadPool = Executors.newFixedThreadPool(threadCount);

            //执行多线程下载
            for (String s : tsSet) {
//                downFuture(s, page, session);
                if (!overPage.contains(page)) {
                    if (stopCosumer) {
                        break;
                    }
                    fixedThreadPool.execute(getThread(s, page));
                } else {
                    log.debug("{} 片段 {} 已下载", fileName, page);
                    finishedCount.incrementAndGet();
                }
                page++;
            }
            fixedThreadPool.shutdown();
            //下载过程监视
            mergeThread = new Thread(() -> {
                int consume = 0;
                //轮询是否下载成功
                while (!fixedThreadPool.isTerminated()) {

                    try {
                        consume++;
                        if (consume > 3600) {
                            log.error("{} 下载失败 !!!", fileName);
                            return;
                        }
                        BigDecimal bigDecimal = new BigDecimal(downloadBytes.toString());
                        Thread.sleep(1000L);
                        log.info("《{}》 已完成 {} % {} 个 共 {} 个 已用时 {} 秒 下载速度：{}/s", fileName, new BigDecimal(finishedCount.get()).divide(new BigDecimal(tsSet.size()),
                                4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100)).setScale(2, BigDecimal.ROUND_HALF_UP), finishedCount.get(), tsSet.size(), consume,
                                StringUtils.convertToDownloadSpeed(new BigDecimal(downloadBytes.toString()).subtract(bigDecimal), 3));
                    } catch (InterruptedException e) {
                        log.error("睡眠失败", e);
                    }
                }

                if (finishedCount.get() == page) {
                    log.info("下载完成，正在合并文件！《{}》有 {} 个 共 {} 个！ {}", fileName, finishedFiles.size(), page, StringUtils.convertToDownloadSpeedNomal(downloadBytes, 3));
                    //开始合并视频
                    mergeTs();
                    log.info("视频合并完成!《{}》", fileName);
                    downPic();
                    //删除多余的ts片段
                    deleteFiles();
                    recordService.success(id);
                    CacheUtils.setLastId(id);
                    CacheUtils.x = 1;
                    CommonTask.downloads.remove(fileName);
                } else if (failCount.get() > 0) {
                    log.error("《{}》 ts片段下载失败", fileName);
                    recordService.deleteRecord(id, fileName);
                    stopDown();
                } else if (stopCosumer) {
                    log.error("暂停任务《{}》", fileName);
                    CacheUtils.downFlag(LocalDateTime.now());
                    stopDown();
                } else {
                    log.error("下载失败 !!!《{}》", fileName);
//                    CommonTask.downloads.remove(fileName);
                }

//                String msg = fileName + " 下载完成";
//                sendMsg(msg,session);

            });
            mergeThread.start();
//            startListener(fixedThreadPool);
//            startListener();
//            return fixedThreadPool;
        }

        private void downPic() {
            StringBuilder builder = new StringBuilder();
            builder.append(dir).append(FILESEPARATOR).append(fileName).append(".jpg");
            File file = new File(builder.toString());
            if (!file.exists()) {
                HttpURLConnection httpURLConnection = null;
                try {
//                    file.createNewFile();
                    URL url = new URL(picUrl);
                    if (proxy == null) {
                        httpURLConnection = (HttpURLConnection) url.openConnection();
                    } else {
                        httpURLConnection = (HttpURLConnection) url.openConnection(proxy);
                    }
                    httpURLConnection.setConnectTimeout((int) timeoutMillisecond);
                    for (Map.Entry<String, Object> entry : requestHeaderMap.entrySet())
                        httpURLConnection.addRequestProperty(entry.getKey(), entry.getValue().toString());
                    httpURLConnection.setUseCaches(false);
                    httpURLConnection.setReadTimeout((int) timeoutMillisecond);
                    httpURLConnection.setDoInput(true);

                    int resCode = httpURLConnection.getResponseCode();
                    if (resCode == 200) {
                        try (OutputStream out = new FileOutputStream(file)) {
                            //创建缓冲区
                            byte[] buff = new byte[4096];
                            int n;
                            // 开始读取
                            while ((n = httpURLConnection.getInputStream().read(buff)) >= 0) {
                                out.write(buff, 0, n);
                            }
                        }
                    } else {
                        log.error("图片下载失败, code :{} : {}", resCode, picUrl);
                    }
                } catch (Exception e) {
                    log.error("图片下载失败: {}", picUrl);
                } finally {
                    if (null != httpURLConnection) {
                        httpURLConnection.disconnect();
                    }
                }
            }


        }


        /**
         * 合并下载好的ts片段
         */
        private void mergeTs() {
            try {
                File file = createFile();

                FileOutputStream fileOutputStream = new FileOutputStream(file);
                byte[] b = new byte[4096];
                for (File f : finishedFiles) {
                    FileInputStream fileInputStream = new FileInputStream(f);
                    int len;
                    while ((len = fileInputStream.read(b)) != -1) {
                        fileOutputStream.write(b, 0, len);
                    }
                    fileInputStream.close();
                    fileOutputStream.flush();
                }
                fileOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private File createFile() throws IOException {
            StringBuilder builder = new StringBuilder();
            builder.append(dir).append(FILESEPARATOR).append(fileName);
            File file = new File(builder.toString() + ".mp4");
//                System.gc();
            if (file.exists()) {
                for (int i = 0; ; i++) {
                    file = new File(builder.append("-").append(i).toString() + ".mp4");
                    if (!file.exists()) {
                        log.warn("视频名称: {}", file.getName());
                        break;
                    }
                }
            }
            file.createNewFile();
            return file;
        }

        /**
         * 删除下载好的片段
         */
        private void deleteFiles() {
            for (int i = 0; i < page; i++) {
                File file2 = new File(pageDir + fileName + "-" + i + ".xyz");
                if (file2.exists())
                    file2.delete();
            }
            if (encryption) {
                for (int i = 0; i < page; i++) {
                    File file2 = new File(pageDir + fileName + "-" + i + ".xy");
                    if (file2.exists())
                        file2.delete();
                }
            }
        }

        /**
         * 开启下载线程
         *
         * @param urls ts片段链接
         * @param i    ts片段序号
         * @return 线程
         */
        private Thread getThread(String urls, int i) {
            return new Thread(() -> {
                int count = 1;
                HttpURLConnection httpURLConnection = null;
                //xy为未解密的ts片段，如果存在，则删除
                String partName = pageDir + fileName + "-" + i;
                File file2 = new File(partName + ".xy");
                if (file2.exists())
                    file2.delete();
                OutputStream outputStream = null;
                InputStream inputStream1 = null;
                FileOutputStream outputStream1 = null;
                log.debug("开始下载: {}", partName);
                byte[] bytes;

                bytes = new byte[Constant.BYTE_COUNT];

                //重试次数判断
                boolean sleep = false;
                int resCode = 0;
                while (count <= retryCount) {
                    try {
                        if (stopCosumer) {
                            log.error("停止线程 {} 文件 {}", i, fileName);
                            return;
                        }
                        sleep = false;
                        //模拟http请求获取ts片段文件
                        URL url = new URL(urls);
                        if (proxy == null) {
                            httpURLConnection = (HttpURLConnection) url.openConnection();
                        } else {
                            httpURLConnection = (HttpURLConnection) url.openConnection(proxy);
                        }
                        httpURLConnection.setConnectTimeout((int) timeoutMillisecond);
                        for (Map.Entry<String, Object> entry : requestHeaderMap.entrySet())
                            httpURLConnection.addRequestProperty(entry.getKey(), entry.getValue().toString());
                        httpURLConnection.setUseCaches(false);
                        httpURLConnection.setReadTimeout((int) timeoutMillisecond);
                        httpURLConnection.setDoInput(true);

                        resCode = httpURLConnection.getResponseCode();
                        if (resCode != 200) {
                            // 获取所有的响应头字段
                            Map<String, java.util.List<String>> headers = httpURLConnection.getHeaderFields();
                            StringBuilder sb = new StringBuilder();
                            // 遍历响应头字段
                            for (Map.Entry<String, java.util.List<String>> entry : headers.entrySet()) {
                                String key = entry.getKey();
                                java.util.List<String> values = entry.getValue();
                                if (key != null) {
                                    sb.append(key).append(": ");
                                }
                                if (values != null) {
                                    for (String value : values) {
                                        sb.append(value).append(" ");
                                    }
                                }
                                sb.append("\n");
                            }
                            log.error("返回头信息 {}\n{}", fileName, sb);

                            throw new SSLHandshakeException("返回码错误");
                        }
                        InputStream inputStream = httpURLConnection.getInputStream();

                        File file = new File(partName + ".xyz");

                        if (!encryption) {
                            file2 = file;
                        }
                        try {
                            outputStream = new FileOutputStream(file2);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            continue;
                        }
                        int len;
                        //将未解密的ts片段写入文件
                        while ((len = inputStream.read(bytes)) != -1) {
                            outputStream.write(bytes, 0, len);
                            synchronized (this) {
                                downloadBytes = downloadBytes.add(new BigDecimal(len));
                            }
                        }
                        outputStream.flush();
                        inputStream.close();
                        if (encryption) {
                            inputStream1 = new FileInputStream(file2);
                            int available = inputStream1.available();
                            if (bytes.length < available)
                                bytes = new byte[available];
                            inputStream1.read(bytes);
                            outputStream1 = new FileOutputStream(file);
                            //开始解密ts片段，这里我们把ts后缀改为了xyz，改不改都一样
                            byte[] decrypt = decrypt(bytes, available, key, iv, method);
                            if (decrypt == null)
                                outputStream1.write(bytes, 0, available);
                            else outputStream1.write(decrypt);
                        }
                        finishedFiles.add(file);
                        break;
                    } catch (Exception e) {
                        if (e instanceof InvalidKeyException || e instanceof InvalidAlgorithmParameterException) {
                            log.error("{} 解密失败！", partName, e);
                            failCount.incrementAndGet();
                            return;
                        }
                        if (e instanceof SSLHandshakeException) {
                            if (resCode == STOP_CDDE || resCode == FORBIDDEN_CDDE) {
                                log.debug("连接下载地址失败, 删除任务！返回码:{} {}", resCode, partName, e);
                                failCount.incrementAndGet();
                                stopCosumer = true;
                                return;
                            } else if (resCode == SKIP_CDDE) {
                                log.debug("连接下载地址被终止, 暂停当前线程任务！ 返回码:{} {}", resCode, partName, e);
                                stopCosumer = true;
//                                skipPage.incrementAndGet();
                                return;
                            }
                        }
                        sleep = true;
                        log.debug("第 {} 获取链接重试！ {} {}", count, partName, urls, e);
                        count++;
//                        e.printStackTrace();
                    } finally {
                        try {
                            if (inputStream1 != null)
                                inputStream1.close();
                            if (outputStream1 != null)
                                outputStream1.close();
                            if (outputStream != null)
                                outputStream.close();
//                            BLOCKING_QUEUE.put(bytes);
                        } catch (IOException e) {
                            log.error("{} 释放流失败", partName);
                        }
                        if (httpURLConnection != null) {
                            httpURLConnection.disconnect();
                        }
                        if (sleep) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                log.error("睡眠失败 {}", partName, e);
                            }
                        }

                    }
                }
                if (count > retryCount) {
                    failCount.incrementAndGet();
//                    pauseDown();
                    log.error("连接超时！{}", partName);
                    return;
                }
                log.debug("下载完成 {}", partName);
                finishedCount.incrementAndGet();
                pageMap.put(i, RecordPage.builder().name(partName).num(i).pId(id).build());
            });
        }

//        public void pauseDown() {
//            shutdownNow();
//            CommonTask.downloads.remove(fileName);
//
//            CacheUtils.put(fileName, LocalDateTime.now());
//        }

        public void stopDown() {
            CacheUtils.setLastId(id);
            shutdownNow();
            CacheUtils.cache(fileName, LocalDateTime.now());
            CommonTask.downloads.remove(fileName);
//            CacheUtils.removeCacheRecord(id);
        }

        private void downFuture(String urls, int i, FixedLengthQueue session) {
            CompletableFuture.runAsync(() -> {
                int count = 1;
                HttpURLConnection httpURLConnection = null;
                //xy为未解密的ts片段，如果存在，则删除
                File file2 = new File(pageDir + fileName + "-" + i + ".xy");
                if (file2.exists())
                    file2.delete();
                OutputStream outputStream = null;
                InputStream inputStream1 = null;
                FileOutputStream outputStream1 = null;
                byte[] bytes;
//                try {
//                    bytes = BLOCKING_QUEUE.take();
//                } catch (InterruptedException e) {
                bytes = new byte[Constant.BYTE_COUNT];
//                }
                //重试次数判断
                while (count <= retryCount) {
                    try {
                        //模拟http请求获取ts片段文件
                        URL url = new URL(urls);
                        if (proxy == null) {
                            httpURLConnection = (HttpURLConnection) url.openConnection();
                        } else {
                            httpURLConnection = (HttpURLConnection) url.openConnection(proxy);
                        }
                        httpURLConnection.setConnectTimeout((int) timeoutMillisecond);
                        for (Map.Entry<String, Object> entry : requestHeaderMap.entrySet())
                            httpURLConnection.addRequestProperty(entry.getKey(), entry.getValue().toString());
                        httpURLConnection.setUseCaches(false);
                        httpURLConnection.setReadTimeout((int) timeoutMillisecond);
                        httpURLConnection.setDoInput(true);
                        InputStream inputStream = httpURLConnection.getInputStream();

                        File file = new File(pageDir + fileName + "-" + i + ".xyz");

                        if (!encryption) {
                            file2 = file;
                        }
                        try {
                            outputStream = new FileOutputStream(file2);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            continue;
                        }
                        int len;
                        //将未解密的ts片段写入文件
                        while ((len = inputStream.read(bytes)) != -1) {
                            outputStream.write(bytes, 0, len);
                            synchronized (this) {
                                downloadBytes = downloadBytes.add(new BigDecimal(len));
                            }
                        }
                        outputStream.flush();
                        inputStream.close();
                        if (encryption) {
                            inputStream1 = new FileInputStream(file2);
                            int available = inputStream1.available();
                            if (bytes.length < available)
                                bytes = new byte[available];
                            inputStream1.read(bytes);
                            outputStream1 = new FileOutputStream(file);
                            //开始解密ts片段，这里我们把ts后缀改为了xyz，改不改都一样
                            byte[] decrypt = decrypt(bytes, available, key, iv, method);
                            if (decrypt == null)
                                outputStream1.write(bytes, 0, available);
                            else outputStream1.write(decrypt);
                        }
                        finishedFiles.add(file);
                        break;
                    } catch (Exception e) {
                        if (e instanceof InvalidKeyException || e instanceof InvalidAlgorithmParameterException) {
                            log.error("解密失败！");
                            break;
                        }
                        log.debug("第" + count + "获取链接重试！\t" + urls);
                        count++;
//                        e.printStackTrace();
                    } finally {
                        try {
                            if (inputStream1 != null)
                                inputStream1.close();
                            if (outputStream1 != null)
                                outputStream1.close();
                            if (outputStream != null)
                                outputStream.close();
//                            BLOCKING_QUEUE.put(bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (httpURLConnection != null) {
                            httpURLConnection.disconnect();
                        }
                    }
                }
                if (count > retryCount)
                    //自定义异常
                    throw new M3u8Exception("连接超时！");
                finishedCount.incrementAndGet();
//                Log.i(urls + "下载完毕！\t已完成" + finishedCount + "个，还剩" + (tsSet.size() - finishedCount) + "个！");
            }, ThreadPoolExecutorUtils.getThreadPoolExecutorInstance()).handle((result, e) -> {
                if (null != e) {
                    log.error("下载失败", e);
//                    String msg = fileName + "-" + i + "下载失败";
//                    sendMsg(msg,session);
                }
//                    return FAIL;
//                } else {
                return result;
            });
        }

        public void sendMsg(String msg, FixedLengthQueue queue) {
            for (int i = 0; i < queue.list().length; i++) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("code", 222);
                jsonObject.put("msg", msg);
                try {
                    HttpAuthHandler.sendMessage(queue.list()[i], jsonObject);
                } catch (Exception e) {
                    log.error("发送信息失败: {} -- {}", msg, e.getMessage());
                    queue.remove(i);
                }
            }
        }

        /**
         * 获取所有的ts片段下载链接
         *
         * @return 链接是否被加密，null为非加密
         */
        private String getTsUrl() {
            StringBuilder content = getUrlContent(DOWNLOADURL, false);
            //判断是否是m3u8链接
            if (!content.toString().contains("#EXTM3U"))
                throw new M3u8Exception(DOWNLOADURL + "不是m3u8链接！");
            String[] split = content.toString().split("\\n");
            String keyUrl = "";
            boolean isKey = false;
            for (String s : split) {
                //如果含有此字段，则说明只有一层m3u8链接
                if (s.contains("#EXT-X-KEY") || s.contains("#EXTINF")) {
                    isKey = true;
                    keyUrl = DOWNLOADURL;
                    break;
                }
                //如果含有此字段，则说明ts片段链接需要从第二个m3u8链接获取
                if (s.contains(".m3u8")) {
                    if (StringUtils.isUrl(s))
                        return s;
                    String relativeUrl = DOWNLOADURL.substring(0, DOWNLOADURL.lastIndexOf("/") + 1);
                    if (s.startsWith("/"))
                        s = s.replaceFirst("/", "");
                    keyUrl = mergeUrl(relativeUrl, s);
                    break;
                }
            }
            if (StringUtils.isEmpty(keyUrl))
                throw new M3u8Exception("未发现key链接！");
            //获取密钥
            String key1 = isKey ? getKey(keyUrl, content) : getKey(keyUrl, null);
            if (StringUtils.isNotEmpty(key1))
                key = key1;
            else key = null;
            return key;
        }

        /**
         * 获取ts解密的密钥，并把ts片段加入set集合
         *
         * @param url     密钥链接，如果无密钥的m3u8，则此字段可为空
         * @param content 内容，如果有密钥，则此字段可以为空
         * @return ts是否需要解密，null为不解密
         */
        private String getKey(String url, StringBuilder content) {
            StringBuilder urlContent;
            if (content == null || StringUtils.isEmpty(content.toString()))
                urlContent = getUrlContent(url, false);
            else urlContent = content;
            if (!urlContent.toString().contains("#EXTM3U"))
                throw new M3u8Exception(DOWNLOADURL + "不是m3u8链接！");
            String[] split = urlContent.toString().split("\\n");
            for (String s : split) {
                //如果含有此字段，则获取加密算法以及获取密钥的链接
                if (s.contains("EXT-X-KEY")) {
                    String[] split1 = s.split(",");
                    for (String s1 : split1) {
                        if (s1.contains("METHOD")) {
                            method = s1.split("=", 2)[1];
                            continue;
                        }
                        if (s1.contains("URI")) {
                            key = s1.split("=", 2)[1];
                            continue;
                        }
                        if (s1.contains("IV"))
                            iv = s1.split("=", 2)[1];
                    }
                }
            }
            String relativeUrl = url.substring(0, url.lastIndexOf("/") + 1);
            //将ts片段链接加入set集合
            for (int i = 0; i < split.length; i++) {
                String s = split[i];
                if (s.contains("#EXTINF")) {
                    String s1 = split[++i];
                    tsSet.add(StringUtils.isUrl(s1) ? s1 : mergeUrl(relativeUrl, s1));
                }
            }
            if (!StringUtils.isEmpty(key)) {
                key = key.replace("\"", "");
                return getUrlContent(StringUtils.isUrl(key) ? key : mergeUrl(relativeUrl, key), true).toString().replaceAll("\\s+", "");
            }
            return null;
        }

        /**
         * 模拟http请求获取内容
         *
         * @param urls  http链接
         * @param isKey 这个url链接是否用于获取key
         * @return 内容
         */
        private StringBuilder getUrlContent(String urls, boolean isKey) {
            int count = 1;
            HttpURLConnection httpURLConnection = null;
            StringBuilder content = new StringBuilder();
            while (count <= retryCount) {
                try {
                    URL url = new URL(urls);
                    if (proxy == null) {
                        httpURLConnection = (HttpURLConnection) url.openConnection();
                    } else {
                        httpURLConnection = (HttpURLConnection) url.openConnection(proxy);
                    }
                    httpURLConnection.setConnectTimeout((int) timeoutMillisecond);
                    httpURLConnection.setReadTimeout((int) timeoutMillisecond);
                    httpURLConnection.setUseCaches(false);
                    httpURLConnection.setDoInput(true);
                    for (Map.Entry<String, Object> entry : requestHeaderMap.entrySet())
                        httpURLConnection.addRequestProperty(entry.getKey(), entry.getValue().toString());
                    String line;
                    InputStream inputStream = httpURLConnection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    if (isKey) {
                        byte[] bytes = new byte[128];
                        int len;
                        len = inputStream.read(bytes);
                        isByte = true;
                        if (len == 1 << 4) {
                            keyBytes = Arrays.copyOf(bytes, 16);
                            content.append("isByte");
                        } else
                            content.append(new String(Arrays.copyOf(bytes, len)));
                        return content;
                    }
                    while ((line = bufferedReader.readLine()) != null)
                        content.append(line).append("\n");
                    bufferedReader.close();
                    inputStream.close();
                    log.debug("{} \n{}", fileName, content);
                    break;
                } catch (Exception e) {
                    log.debug("第 {} 获取链接重试！\t{}", count, urls);
                    count++;
//                    e.printStackTrace();
                } finally {
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                }
            }
            if (count > retryCount)
                throw new M3u8Exception(fileName + " 获取 m3u8文件 连接超时！");
            return content;
        }

        /**
         * 解密ts
         *
         * @param sSrc   ts文件字节数组
         * @param length
         * @param sKey   密钥
         * @return 解密后的字节数组
         */
        private byte[] decrypt(byte[] sSrc, int length, String sKey, String iv, String method) throws Exception {
            if (StringUtils.isNotEmpty(method) && !method.contains("AES"))
                throw new M3u8Exception("未知的算法！");
            // 判断Key是否正确
            if (StringUtils.isEmpty(sKey))
                return null;
            // 判断Key是否为16位
            if (sKey.length() != 16 && !isByte) {
                throw new M3u8Exception("Key长度不是16位！");
            }
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            SecretKeySpec keySpec = new SecretKeySpec(isByte ? keyBytes : sKey.getBytes(StandardCharsets.UTF_8), "AES");
            byte[] ivByte;
            if (iv.startsWith("0x"))
                ivByte = StringUtils.hexStringToByteArray(iv.substring(2));
            else ivByte = iv.getBytes();
            if (ivByte.length != 16)
                ivByte = new byte[16];
            //如果m3u8有IV标签，那么IvParameterSpec构造函数就把IV标签后的内容转成字节数组传进去
            AlgorithmParameterSpec paramSpec = new IvParameterSpec(ivByte);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
            return cipher.doFinal(sSrc, 0, length);
        }

        /**
         * 字段校验
         */
        private void checkField() {
            if (".m3u8".compareTo(MediaFormat.getMediaFormat(DOWNLOADURL)) != 0)
                throw new M3u8Exception(DOWNLOADURL + "不是一个完整m3u8链接！");
            if (threadCount <= 0)
                throw new M3u8Exception("同时下载线程数只能大于0！");
            if (retryCount < 0)
                throw new M3u8Exception("重试次数不能小于0！");
            if (timeoutMillisecond < 0)
                throw new M3u8Exception("超时时间不能小于0！");
            if (StringUtils.isEmpty(dir))
                throw new M3u8Exception("视频存储目录不能为空！");
            if (StringUtils.isEmpty(fileName))
                throw new M3u8Exception("视频名称不能为空！");
            finishedCount.set(0);
            method = "";
            key = "";
            isByte = false;
            iv = "";
            tsSet.clear();
            finishedFiles.clear();
            downloadBytes = new BigDecimal(0);
        }

        private String mergeUrl(String start, String end) {
            if (end.startsWith("/"))
                end = end.replaceFirst("/", "");
            int position = 0;
            String subEnd, tempEnd = end;
            while ((position = end.indexOf("/", position)) != -1) {
                subEnd = end.substring(0, position + 1);
                if (start.endsWith(subEnd)) {
                    tempEnd = end.replaceFirst(subEnd, "");
                    break;
                }
                ++position;
            }
            return start + tempEnd;
        }

    }


    /**
     * 获取实例
     *
     * @param downloadUrl 要下载的链接
     * @return 返回m3u8下载实例
     */
    public M3u8Download getInstance(String downloadUrl) {
        return new M3u8Download(downloadUrl);
    }

    public void destroied() {
        m3u8Download = null;
    }

}
