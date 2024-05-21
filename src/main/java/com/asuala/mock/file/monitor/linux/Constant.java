package com.asuala.mock.file.monitor.linux;

/**
 * @description:
 * @create: 2024/05/20
 **/
public class Constant {

    public static final int IN_CREATE = 0x00000100;
    public static final int IN_DELETE = 0x00000200;
    public static final int IN_MODIFY = 0x00000002;
    public static final int IN_MOVE = 0x00000008;
    public static final int IN_OPEN = 0x00000020;
    public static final int IN_CLOSE = 0x00000040;
    public static final int IN_MOVED_FROM = 0x00000040;
    public static final int IN_MOVED_TO = 0x00000080;
    public static final int IN_ISDIR = 0x40000000;
    public static final int IN_MOVE_SELF = 0x00000800;
    public static final int IN_DELETE_SELF = 0x00000400;

}