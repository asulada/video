import subprocess
import os
import logging.config
import time
import pyopencl as cl
import ffmpeg
import signal
import sys
import traceback

'''
pip install pyopencl
'''

logging.config.fileConfig('python\\logging.conf')
logger = logging.getLogger('main')

suffix = "-h264"

amd_gpu_flag = True
exit_flag = False
process_flag = True


# share_value = None


def seconds_to_24h_format(seconds):
    # 计算总小时数
    total_hours = seconds // 3600

    # 获取小时部分
    hours = total_hours % 24

    # 计算剩余的分钟数
    remaining_minutes = (seconds % 3600) // 60

    # 获取剩余的秒数部分
    remaining_seconds = seconds % 60

    return f"{hours:02d}:{remaining_minutes:02d}:{remaining_seconds:02d}"


def ignore_signal(signum, frame):
    # 这个函数会在收到信号时被调用，但是不会做任何处理
    global exit_flag
    exit_flag = True
    # global share_value
    # share_value.value = True
    logger.info(f"{os.getpid()} 等待退出")


def get_video_bitrate(filename):
    try:
        probe = ffmpeg.probe(filename)
        # video_stream = next((stream for stream in probe['streams'] if stream['codec_type'] == 'video'), None)
        video_info = probe['format']
        if video_info:
            duration = str(video_info['duration'])
            index = duration.find('.')
            if index != -1:
                duration = duration[0: index]
            return int(video_info['bit_rate']), duration
        else:
            print("视频文件中没有视频流。")
            return None
    except ffmpeg.Error as e:
        print("FFmpeg 出错:", e.stderr)
        return None


def is_gpu():
    global process_flag
    platforms = cl.get_platforms()
    devices = []
    for platform in platforms:
        for device in platform.get_devices():
            devices.append(device)

    if devices:
        for device in devices:
            if str(device).find('NVIDIA') != -1:
                auto_flag = False
                process_flag = True
                # pass
            else:
                auto_flag = True
                process_flag = False
            logger.info(f"设备类型 {str(device)} 启动auto转换 {auto_flag}")
    return auto_flag


def ffmpeg_change(in_path, out_path):
    start = time.perf_counter()  # 记录开始时间

    bitrate, duration = get_video_bitrate(in_path)

    duration_24 = seconds_to_24h_format(int(duration))
    bitrate_k = round(bitrate / 1000, 3)
    logger.info(f"{os.getpid()} 开始转码 {in_path} 时长：{duration_24} 码率为：{bitrate_k}kbps")

    bitrate_str = str(bitrate)
    if amd_gpu_flag:
        # full_command = "ffmpeg -hwaccel auto -i \"" + in_path + "\" -b:v " + bit_rate + " -bufsize " + bit_rate + " -c:v h264_amf -c:a aac -strict -2 \"" + out_path + "\""
        full_command = "ffmpeg -i \"" + in_path + "\" -b:v " + bitrate_str + " -bufsize " + bitrate_str + " -c:v h264_amf -c:a aac -strict -2 \"" + out_path + "\""
        # full_command = "ffmpeg  -i \"" + in_path + "\" -b:v " + bit_rate + " -bufsize " + bit_rate + " -c:v libx264 -c:a aac -strict -2 \"" + out_path + "\""
    else:
        full_command = "ffmpeg -hwaccel cuda -c:v h264_cuvid -i \"" + in_path + "\" -b:v " + bitrate_str + " -bufsize " + bitrate_str + " -c:v h264_nvenc -c:a aac -strict -2 \"" + out_path + "\""
    if sys.platform.startswith('win'):
        process = subprocess.Popen(full_command, shell=True, creationflags=0x00000200)
    else:
        process = subprocess.Popen(full_command, shell=True, preexec_fn=ignore_signal)

    output, error = process.communicate()
    # for i in iter(process.stdout.readline, ''):
    #     if len(i) < 1:
    #         break
    #     print(i.decode('gbk').strip())
    end = time.perf_counter()  # 记录结束时间
    elapsed = end - start  # 计算经过的时间（单位为秒）
    if process.returncode == 0:
        os.remove(in_path)
    # if output:
    #     logger.error(f"{in_path}: {error.decode('utf-8')}")
    logger.info(
        f"{os.getpid()} {in_path} \n视频码率为：{bitrate_k}kbps 时长：{duration_24} 耗时：{seconds_to_24h_format(int(elapsed))}")

    if amd_gpu_flag:
        time.sleep(20)


def build_path(path) -> dict:
    # if share_value.value:
    root, file = os.path.split(path)
    if file.lower().endswith(('.mp4', '.ts', '.mkv')):
        file_name = os.path.splitext(os.path.basename(file))[0]
        if not file_name.lower().endswith(suffix):
            file_path = root + "\\" + file_name
            out_path = file_path + suffix + ".mp4"
            if not os.path.exists(out_path):
                return {'root': root, 'file': file, 'out_path': out_path}
            else:
                if os.path.getsize(out_path) > 0:
                    logger.info(f"{out_path} 已存在")
                else:
                    os.remove(out_path)
                    return {'root': root, 'file': file, 'out_path': out_path}
        else:
            logger.info(f"{file} 已经转换")
    return None


def ffmped_pre(root, file, out_path):
    if not root:
        return
    if exit_flag:
        return
    if os.path.exists(out_path):
        logger.info(f"{os.getpid()} {out_path} 已存在")
        return
    in_path = root + "\\" + file
    ffmpeg_change(in_path, out_path)


def init_process_params(auto):
    logger.info(f"{os.getpid()} 初始化参数")
    signal.signal(signal.SIGINT, ignore_signal)

    global amd_gpu_flag
    amd_gpu_flag = auto


def list_file(path, amd_gpu_flag, process_flag):
    # global share_value
    # pool = None
    # share_value = multiprocessing.Manager().Value('b', False)
    try:
        # file_list = []
        # for root, dirs, files in os.walk(path):
        #     for file in files:
        #         file_dict =
        #         if file_dict:
        #             file_list.append(file_dict)
        # logger.info(f'转码视频数量 {len(file_list)}')
        # if not process_flag:
        info = build_path(path)
        if info:
            ffmped_pre(info['root'], info['file'], info['out_path'])
        # else:
        #     pool = Pool(processes=2, initializer=init_process_params, initargs=(amd_gpu_flag,))
        #     pool.starmap(ffmped_pre, [(info['root'], info['file'], info['out_path']) for info in file_list])
    except Exception as e:
        # 获取当前调用堆栈
        tb = traceback.format_exc()
        # 记录异常信息，包括异常类型、信息和堆栈
        logger.error(f"Exception occurred. Type: {type(e).__name__}, Message: {e}, Stack Trace: {tb}")
    # finally:
    # if pool:
    #     pool.close()
    logger.info("转换结束")


if __name__ == '__main__':
    path = (sys.argv[1])

    # 注册信号处理函数
    signal.signal(signal.SIGINT, ignore_signal)
    list_file(path, is_gpu(), process_flag)
