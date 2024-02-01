package com.asuala.mock.file.monitor.utils;

import com.asuala.mock.file.monitor.entity.*;
import com.asuala.mock.file.monitor.po.FileInfo;
import com.asuala.mock.file.monitor.vo.FileTreeNode;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

/**
 * @description:
 * @create: 2024/02/01
 **/
@Slf4j
public class MonitorFileUtil {

    private static final int FSCTL_CREATE_USN_JOURNAL = WinioctlUtil.CTL_CODE(Winioctl.FILE_DEVICE_FILE_SYSTEM, 57, Winioctl.METHOD_NEITHER, Winioctl.FILE_ANY_ACCESS);
    private static final int FSCTL_QUERY_USN_JOURNAL = WinioctlUtil.CTL_CODE(Winioctl.FILE_DEVICE_FILE_SYSTEM, 61, Winioctl.METHOD_BUFFERED, Winioctl.FILE_ANY_ACCESS);
    private static final int FSCTL_ENUM_USN_DATA = WinioctlUtil.CTL_CODE(Winioctl.FILE_DEVICE_FILE_SYSTEM, 44, Winioctl.METHOD_NEITHER, Winioctl.FILE_ANY_ACCESS);
    private static final int FSCTL_DELETE_USN_JOURNAL = WinioctlUtil.CTL_CODE(Winioctl.FILE_DEVICE_FILE_SYSTEM, 62, Winioctl.METHOD_BUFFERED, Winioctl.FILE_ANY_ACCESS);
    private static final int USN_DELETE_FLAG_DELETE = 0x00000001;
    private static final int outputBufferSize = 1024 * 1024 / 2;

    public static void getFile(TreeMap<Long, FileTreeNode> map){
        for (Map.Entry<Long, FileTreeNode> entry : map.descendingMap().entrySet()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("/").append(entry.getValue().getFileName());
            findPath(map, entry.getValue(), stringBuilder);
            String filePath = stringBuilder.toString();
            File file = new File(filePath);
            FileInfo.builder().name(entry.getValue().getFileName());
        }
    }

    private static void findPath(TreeMap<Long, FileTreeNode> map, FileTreeNode entry, StringBuilder stringBuilder) {
        FileTreeNode parentNode = map.get(entry.getParentFileReferenceNumber());
        if (null != parentNode) {
            stringBuilder.insert(0, parentNode.getFileName()).insert(0, "/");
            findPath(map, parentNode, stringBuilder);
        }
    }

    /**
    * @Description TODO-asuala 索引卷上所有文件并返回
    * @param volumeNo
    * @Return {@link TreeMap< Long, FileTreeNode>}
    * @Date 2024-02-01
    **/
    public static TreeMap<Long, FileTreeNode> buildFileInfo(String volumeNo) throws Exception {
        Kernel32 kernel32 = Kernel32.INSTANCE;
        WinNT.HANDLE hVolume = kernel32.CreateFile("\\\\.\\" + volumeNo + ":", WinNT.GENERIC_READ | WinNT.GENERIC_WRITE, WinNT.FILE_SHARE_READ | WinNT.FILE_SHARE_WRITE, null, WinNT.OPEN_EXISTING, WinNT.FILE_ATTRIBUTE_NORMAL, null);
        if (WinNT.INVALID_HANDLE_VALUE.equals(hVolume)) {
            log.error("Failed to open volume! code {}", kernel32.GetLastError());
            throw new Exception();
        }
        CREATE_USN_JOURNAL_DATA createUsnJournalData = new CREATE_USN_JOURNAL_DATA();
        IntByReference resultInt = new IntByReference();
        if (!kernel32.DeviceIoControl(hVolume, FSCTL_CREATE_USN_JOURNAL, createUsnJournalData.getPointer(), createUsnJournalData.size(), null, 0, resultInt, null)) {
            log.error("打开卷失败! 错误码 {}", kernel32.GetLastError());
            throw new Exception();
        }

        USN_JOURNAL_DATA_V0 usnJournalData = new USN_JOURNAL_DATA_V0();
        if (!kernel32.DeviceIoControl(hVolume, FSCTL_QUERY_USN_JOURNAL, null, 0, usnJournalData.getPointer(), usnJournalData.size(), resultInt, null)) {
            log.error("读取卷失败! 错误码 {}", kernel32.GetLastError());
            throw new Exception();
        }
        usnJournalData.read();

        log.debug("UsnJournalID: {}", usnJournalData.UsnJournalID);
        log.debug("FirstUsn: {}", usnJournalData.FirstUsn);
        log.debug("NextUsn: {}", usnJournalData.NextUsn);
        log.debug("LowestValidUsn: {}", usnJournalData.LowestValidUsn);
        log.debug("MaxUsn: {}", usnJournalData.MaxUsn);
        log.debug("MaximumSize: {}", usnJournalData.MaximumSize);
        log.debug("AllocationDelta: {}", usnJournalData.AllocationDelta);


        // 构建输入缓冲区
        MTF_ENUM_DATA mtf_enum_data = new MTF_ENUM_DATA();
        mtf_enum_data.LowUsn = usnJournalData.FirstUsn;
        mtf_enum_data.HighUsn = usnJournalData.NextUsn;
        mtf_enum_data.write();
        Pointer buffer = new Memory(outputBufferSize);
        // 调用DeviceIoControl函数
        TreeMap<Long, FileTreeNode> map = new TreeMap<>();

        while (kernel32.DeviceIoControl(hVolume, FSCTL_ENUM_USN_DATA, mtf_enum_data.getPointer(),
                mtf_enum_data.size(), buffer, outputBufferSize, resultInt, null)) {

            int dwRetBytes = resultInt.getValue() - WinDef.LONGLONG.SIZE;
            long[] start = new long[1];
            buffer.read(0, start, 0, 1);
            // 找到第一个 USN 记录
            USN_RECORD usnRecord = new USN_RECORD(buffer.share(WinDef.LONGLONG.SIZE));
            while (dwRetBytes > 0) {
                usnRecord.read();
                char[] name = new char[usnRecord.FileNameLength.intValue() / 2];
                usnRecord.getPointer().read(usnRecord.FileNameOffset.intValue(), name, 0, usnRecord.FileNameLength.intValue() / 2);
                map.put(usnRecord.FileReferenceNumber.longValue(), FileTreeNode.builder().FileName(String.valueOf(name)).FileReferenceNumber(usnRecord.FileReferenceNumber.longValue()).ParentFileReferenceNumber(usnRecord.ParentFileReferenceNumber.longValue()).build());
                dwRetBytes -= usnRecord.RecordLength.intValue();
                usnRecord = new USN_RECORD(usnRecord.getPointer().share(usnRecord.RecordLength.intValue()));
            }
            mtf_enum_data.StartFileReferenceNumber = start[0];
            mtf_enum_data.write();
        }
        log.debug("读取USN记录错误码: {}", kernel32.GetLastError());

        DELETE_USN_JOURNAL_DATA deleteUsnJournalData = new DELETE_USN_JOURNAL_DATA();
        deleteUsnJournalData.UsnJournalID = usnJournalData.UsnJournalID;
        deleteUsnJournalData.DeleteFlags = USN_DELETE_FLAG_DELETE;
        deleteUsnJournalData.write();
        if (kernel32.DeviceIoControl(hVolume, FSCTL_DELETE_USN_JOURNAL, deleteUsnJournalData.getPointer(), deleteUsnJournalData.size(), null, 0, resultInt, null)) {
            log.error("删除卷失败! {}", kernel32.GetLastError());
            throw new Exception();
        }
        kernel32.CloseHandle(hVolume);
        return map;
    }


}