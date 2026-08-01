package com.hogimn.malcollector.logsupport

import org.slf4j.Logger
import org.slf4j.event.Level
import org.slf4j.spi.LoggingEventBuilder

class AppLogger(private val delegate: Logger) : Logger by delegate {
    private val prefixes = listOf("[A]", "[S]", "[R]", "[Q]", "[QP]", "[Z]")

    private fun resolveInfoMessage(msg: String?): String? {
        if (msg == null) return null
        return if (prefixes.none { msg.startsWith(it) }) "[I] $msg" else msg
    }

    override fun info(msg: String?) {
        delegate.info(resolveInfoMessage(msg))
    }

    override fun info(format: String?, arg: Any?) {
        delegate.info(resolveInfoMessage(format), arg)
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        delegate.info(resolveInfoMessage(format), arg1, arg2)
    }

    override fun info(format: String?, vararg arguments: Any?) {
        delegate.info(resolveInfoMessage(format), *arguments)
    }

    override fun info(msg: String?, t: Throwable?) {
        delegate.info(resolveInfoMessage(msg), t)
    }

    override fun warn(msg: String?) {
        delegate.warn("[W] $msg")
    }

    override fun warn(format: String?, arg: Any?) {
        delegate.warn("[W] $format", arg)
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        delegate.warn("[W] $format", arg1, arg2)
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        delegate.warn("[W] $format", *arguments)
    }

    override fun warn(msg: String?, t: Throwable?) {
        delegate.warn("[W] $msg", t)
    }

    override fun error(msg: String?) {
        delegate.error("[E] $msg")
    }

    override fun error(format: String?, arg: Any?) {
        delegate.error("[E] $format", arg)
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        delegate.error("[E] $format", arg1, arg2)
    }

    override fun error(format: String?, vararg arguments: Any?) {
        delegate.error("[E] $format", *arguments)
    }

    override fun error(msg: String?, t: Throwable?) {
        delegate.error("[E] $msg", t)
    }

    override fun makeLoggingEventBuilder(level: Level?): LoggingEventBuilder? {
        return delegate.makeLoggingEventBuilder(level)
    }

    override fun atLevel(level: Level?): LoggingEventBuilder? {
        return delegate.atLevel(level)
    }

    override fun isEnabledForLevel(level: Level?): Boolean {
        return delegate.isEnabledForLevel(level)
    }

    override fun atTrace(): LoggingEventBuilder? {
        return delegate.atTrace()
    }

    override fun atDebug(): LoggingEventBuilder? {
        return delegate.atDebug()
    }

    override fun atInfo(): LoggingEventBuilder? {
        return delegate.atInfo()
    }

    override fun atWarn(): LoggingEventBuilder? {
        return delegate.atWarn()
    }

    override fun atError(): LoggingEventBuilder? {
        return delegate.atError()
    }
}