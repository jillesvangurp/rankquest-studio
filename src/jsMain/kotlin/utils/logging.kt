package utils

private external fun setLogLevel(lvl: Int)

enum class JsLogLevel {
    OFF,
    DEBUG,
    INFO,
    WARN,
    ERROR
}

fun setJsLogLevel(level: JsLogLevel) {
    console.log(level, level.ordinal)
    setLogLevel(level.ordinal)
}