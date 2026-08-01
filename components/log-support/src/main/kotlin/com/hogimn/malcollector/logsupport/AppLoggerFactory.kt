import com.hogimn.malcollector.logsupport.AppLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object AppLoggerFactory {
    fun getLogger(clazz: Class<*>): Logger {
        val baseLogger = LoggerFactory.getLogger(clazz)
        return AppLogger(baseLogger)
    }
}