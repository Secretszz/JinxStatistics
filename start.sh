#!/bin/bash

# 定义变量
APP_NAME="JinxStatistics-0.0.1-SNAPSHOT.jar"
LOG_FILE="app.log"
JAVA_OPTS="-Xms256m -Xmx512m"

# 检查 Java 版本
check_java_version() {
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    if [[ "$JAVA_VERSION" < "17" ]]; then
        echo "错误：需要 Java 17 或更高版本，当前版本为 $JAVA_VERSION"
        exit 1
    fi
}

# 检查 JAR 文件是否存在
check_jar_file() {
    if [[ -z "$APP_NAME" ]]; then
        echo "错误：APP_NAME 变量未定义"
        exit 1
    fi
    SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
    JAR_PATH="$SCRIPT_DIR/$APP_NAME"
    if [[ ! -f "$JAR_PATH" ]]; then
        echo "错误：未找到 $APP_NAME 文件，请确保文件存在于以下路径：$JAR_PATH"
        echo "当前目录内容："
        ls -l "$SCRIPT_DIR"
        exit 1
    fi
}

# 停止程序
stop_application() {
    echo "正在停止 $APP_NAME..."
    if pgrep -f "$APP_NAME" > /dev/null; then
        pkill -f "$APP_NAME"
        sleep 2
        echo "程序已停止"
    else
        echo "程序未运行"
    fi
}

# 启动程序
start_application() {
    echo "正在启动 $APP_NAME..."
    SCRIPT_DIR=$(dirname "$0")
    LOG_PATH="$SCRIPT_DIR/$LOG_FILE"
    # 启动应用
    nohup java $JAVA_OPTS -jar "$JAR_PATH" > "$LOG_PATH" 2>&1 &
    echo "启动命令已执行，请稍后运行检查命令确认是否启动成功。"
}

# 检查程序是否启动成功
check_application_status() {
    if pgrep -f "$APP_NAME" > /dev/null; then
        echo "程序已启动，日志输出到 $LOG_PATH"
    else
        echo "程序未启动，请检查日志：$LOG_PATH"
        exit 1
    fi
}

# 重启程序
restart_application() {
    stop_application
    start_application
}

# 主逻辑
case "$1" in
    "start")
        check_java_version
        check_jar_file
        start_application
        ;;
    "stop")
        stop_application
        ;;
    "restart")
        check_java_version
        check_jar_file
        restart_application
        ;;
    "check")
        check_application_status
        ;;
    *)
        echo "用法: $0 {start|stop|restart|check}"
        exit 1
        ;;
esac