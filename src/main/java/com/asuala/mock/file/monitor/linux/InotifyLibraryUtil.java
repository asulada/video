package com.asuala.mock.file.monitor.linux;

import cn.hutool.core.io.FileUtil;
import com.asuala.mock.m3u8.utils.Constant;
import com.asuala.mock.service.FileInfoService;
import com.asuala.mock.utils.CacheUtils;
import com.asuala.mock.vo.FileInfo;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;

import static com.asuala.mock.file.monitor.linux.Constant.*;


/**
 * @description:
 * @create: 2024/05/06
 **/
@Slf4j
public class InotifyLibraryUtil {


    public static ExecutorService fixedThreadPool;

    public static ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();


    public interface InotifyLibrary extends Library {
        InotifyLibrary INSTANCE = (InotifyLibrary) Native.load("c", InotifyLibrary.class);

        int inotify_init();

        int inotify_add_watch(int fd, String path, int mask);

        int inotify_rm_watch(int fd, int wd);

        int read(int fd, Pointer buf, int size);

        int close(int fd);

    }

    public static void main(String[] args) {
        test001();
    }
        /*
    IN_ACCESS：文件被访问
IN_MODIFY：文件被修改
IN_ATTRIB，文件属性被修改
IN_CLOSE_WRITE，以可写方式打开的文件被关闭
IN_CLOSE_NOWRITE，以不可写方式打开的文件被关闭
IN_OPEN，文件被打开
IN_MOVED_FROM，文件被移出监控的目录
IN_MOVED_TO，文件被移入监控着的目录
IN_CREATE，在监控的目录中新建文件或子目录
IN_DELETE，文件或目录被删除
IN_DELETE_SELF，自删除，即一个可执行文件在执行时删除自己
IN_MOVE_SELF，自移动，即一个可执行文件在执行时移动自己
     */

    private static Map<Integer, String> eventNameMap = new HashMap<Integer, String>() {{
        put(IN_MOVED_FROM, "文件被移出监控的目录");
        put(IN_MOVED_TO, "文件被移入监控着的目录");
        put(IN_CREATE, "在监控的目录中新建文件或子目录");
        put(IN_DELETE, "文件或目录被删除");
        put(IN_DELETE_SELF, "自删除，即一个可执行文件在执行时删除自己");
        put(IN_MOVE_SELF, "自移动，即一个可执行文件在执行时移动自己");
        put(IN_MODIFY, "文件被修改");
        put(IN_UNMOUNT, "已卸载备份fs");
        put(IN_Q_OVERFLOW, "排队的事件溢出");
        put(IN_IGNORED, "文件被忽略");
    }};

    public static void test001() {
        int fd = InotifyLibrary.INSTANCE.inotify_init();
        String pathname = "/mnt/nfts1/test/test1";
        String pathname1 = "/mnt/nfts1/test/test2";
        int wd = InotifyLibrary.INSTANCE.inotify_add_watch(fd, pathname,
                IN_MOVED_FROM | IN_MOVED_TO | IN_CREATE | IN_DELETE | IN_MODIFY | IN_DELETE_SELF | IN_MOVE_SELF);
        int wd1 = InotifyLibrary.INSTANCE.inotify_add_watch(fd, pathname1,
                IN_MOVED_FROM | IN_MOVED_TO | IN_CREATE | IN_DELETE | IN_MODIFY | IN_DELETE_SELF | IN_MOVE_SELF);

        int size = 4096;
        Pointer pointer = new Memory(size);
        try {
//            while (true) {
            for (int k = 0; k < 10; k++) {

                int bytesRead = InotifyLibrary.INSTANCE.read(fd, pointer, size);
                for (int i = 0; i < bytesRead; ) {
                    int wd2 = pointer.getInt(i);
                    i += 4;
                    int mask = pointer.getInt(i);
                    i += 4;
                    int cookie = pointer.getInt(i);
                    i += 4;
                    int nameLen = pointer.getInt(i);
                    i += 4;
                    byte[] nameBytes = new byte[nameLen];
                    pointer.read(i, nameBytes, 0, nameBytes.length);
                    i += nameLen;
                    String name = byteToStr(nameBytes);

                    boolean isDir = false;
                    if ((mask & IN_ISDIR) != 0) {
                        isDir = true;
                        mask -= IN_ISDIR;
                    }

                    String action = "";
                    if (eventNameMap.containsKey(mask)) {
                        action = eventNameMap.get(mask);
                    }


                    System.out.println("wd=" + wd2 + " mask=" + mask + " cookie=" + cookie + (isDir ? "文件夹 " : "文件") + " name=" + name.toString() + " " + action);
                }
            }
        } finally {
            InotifyLibrary.INSTANCE.inotify_rm_watch(fd, wd);
            InotifyLibrary.INSTANCE.inotify_rm_watch(fd, wd1);
            InotifyLibrary.INSTANCE.close(fd);

        }
    }

    /**
     * 去掉byte[]中填充的0 转为String
     *
     * @param buffer
     * @return
     */
    public static String byteToStr(byte[] buffer) {
        try {
            int length = 0;
            for (int i = 0; i < buffer.length; ++i) {
                if (buffer[i] == 0) {
                    length = i;
                    break;
                }
            }
            return new String(buffer, 0, length, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    public static CopyOnWriteArrayList<FileInfo>[][] rebuild(Set<String> dirs) throws IOException {
        CopyOnWriteArrayList<FileInfo>[][] dirFileArray = new CopyOnWriteArrayList[dirs.size()][2];
        int i = 0;
        for (String dir : dirs) {
            dirFileArray[i++] = findDirFile(dir);
        }
        return dirFileArray;
    }

    public static void init(Set<String> dirs) throws IOException {
        fixedThreadPool = Executors.newFixedThreadPool(dirs.size());
        for (String dir : dirs) {
            List<String> dirPaths = findDir(dir);
            fixedThreadPool.execute(new Watch(dirPaths));
        }
    }

    public static List<String> findDir(String path) throws IOException {
        Path startPath = Paths.get("起始目录路径");

        List<String> dirs
                = new ArrayList<>();
        Files.walk(startPath)
                .filter(Files::isDirectory)
                .forEach(item -> dirs.add(item.toString()));
        return dirs;
    }


    public static CopyOnWriteArrayList<FileInfo>[] findDirFile(String path) throws IOException {
        // 指定要遍历的文件夹路径
        Path folderPath = Paths.get(path);

        CopyOnWriteArrayList<FileInfo> dirs = new CopyOnWriteArrayList();
        CopyOnWriteArrayList<FileInfo> files = new CopyOnWriteArrayList();

        // 使用 Files.walk() 方法遍历文件夹
        Files.walkFileTree(folderPath, new SimpleFileVisitor<Path>() {

            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                // 获取文件夹信息

                FileInfo fileInfo = FileInfo.builder().index(Constant.index).name(dir.getFileName().toString()).path(dir.toString()).createTime(new Date())
                        .changeTime(new Date(Files.getLastModifiedTime(dir).toMillis())).dir(1).build();
                dirs.add(fileInfo);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // 获取文件修改时间
                Date modifyTime = new Date(Files.getLastModifiedTime(file).toMillis());

                // 获取文件大小
                long fileSize = Files.size(file);

                String fileName = file.getFileName().toString();

                String suffix = FileUtil.getSuffix(fileName);

                FileInfo fileInfo = FileInfo.builder().index(Constant.index).name(fileName).path(file.toString()).createTime(new Date())
                        .changeTime(modifyTime).dir(0).size(fileSize).suffix(suffix).build();
                files.add(fileInfo);

                return FileVisitResult.CONTINUE;
            }
        });
        return new CopyOnWriteArrayList[]{dirs, files};
    }

    static class Watch implements Runnable {

        private int fd;
        private Map<Integer, String> wdMap;
        private Map<String, Integer> wdStrMap;
        private List<String> paths;
        private int size = 4096;

        public Watch(List<String> paths) {
            this.paths = paths;
        }

        @Override
        public void run() {
            fd = InotifyLibrary.INSTANCE.inotify_init();

            for (String path : paths) {
                addWatchDir(path);
            }

            Pointer pointer = new Memory(size);
            try {
                while (CacheUtils.watchFlag) {
                    int bytesRead = InotifyLibrary.INSTANCE.read(fd, pointer, size);

                    for (int i = 0; i < bytesRead; ) {
                        String event = "";
                        int wd2 = pointer.getInt(i);
                        i += 4;
                        int mask = pointer.getInt(i);
                        i += 4;
                        int cookie = pointer.getInt(i);
                        i += 4;
                        int nameLen = pointer.getInt(i);
                        i += 4;
                        byte[] nameBytes = new byte[nameLen];
                        pointer.read(i, nameBytes, 0, nameBytes.length);
                        i += nameLen;
                        String name = byteToStr(nameBytes);

                        event = eventNameMap.get(mask);

                        String path = wdMap.get(wd2);

                        String filePath = path + Constant.FILESEPARATOR + name;
                        boolean isDir = false;
                        if ((mask & IN_ISDIR) != 0) {
                            mask -= IN_ISDIR;
                            isDir = true;

                            log.debug("目录: {} 事件: {} 关联码: {} 目录名: {} ", path, event, cookie, name);
                        } else {
                            log.debug("目录: {} 事件: {} 关联码: {} 文件名: {} ", path, event, cookie, name);
                        }

                        if (mask == IN_DELETE_SELF) {
                            filePath = filePath.substring(0, filePath.length() - 2);
                            isDir = true;
                            removeWatchDir(path);
                        } else if (mask == IN_CREATE) {
                            filePath = filePath.substring(0, filePath.length() - 2);
                            addWatchDir(filePath);
                        }
                        FileVo fileVo = new FileVo();
                        fileVo.setFullPath(filePath);
                        fileVo.setPath(path);
                        fileVo.setName(name);
                        fileVo.setCode(mask);
                        fileVo.setDir(isDir);
                        CacheUtils.queue.offer(fileVo);

                    }
                }
            } finally {
                for (Map.Entry<Integer, String> entry : wdMap.entrySet()) {
                    try {
                        InotifyLibrary.INSTANCE.inotify_rm_watch(fd, entry.getKey());
                        log.info("释放释放inotify watch成功! 路径 {}", entry.getValue());
                    } catch (Exception e) {
                        log.error("释放inotify watch失败!!! 路径 {}", entry.getValue(), e);
                    }
                }
                InotifyLibrary.INSTANCE.close(fd);
                log.info("释放释放inotify fd {} 结束", fd);
            }
        }

        private void addWatchDir(String path) {
            int wd = InotifyLibrary.INSTANCE.inotify_add_watch(fd, path,
                    IN_MOVED_FROM | IN_MOVED_TO | IN_CREATE | IN_DELETE | IN_DELETE_SELF);
            wdMap.put(wd, path);
            wdStrMap.put(path, wd);
        }

        private void removeWatchDir(String path) {
            Integer code = wdStrMap.get(path);

            InotifyLibrary.INSTANCE.inotify_rm_watch(fd, code);

            wdStrMap.remove(path);
            wdMap.remove(code);

        }
    }


}

