package net.unethicalite.winemaker.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class Log {
    val log: Logger = LoggerFactory.getLogger(this.javaClass)
}