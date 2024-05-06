package com.asuala.mock.file.monitor.linux;
import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * @description:
 * @create: 2024/05/06
 **/
public class InotifyLibraryUtil {

    public interface InotifyLibrary extends Library {
        InotifyLibrary INSTANCE = (InotifyLibrary) Native.load("c", InotifyLibrary.class);

        int inotify_init();
        int inotify_add_watch(int fd, String path, int mask);
        int inotify_rm_watch(int fd, int wd);
    }
}

