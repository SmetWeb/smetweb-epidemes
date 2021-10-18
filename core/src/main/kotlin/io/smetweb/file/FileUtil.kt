package io.smetweb.file

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.util.stream.Collectors

private val LOG: Logger = LoggerFactory.getLogger("FileUtil")

private const val DEFAULT_CHARSET = "UTF-8"

/**
 * [see here](http://www.adam-bien.com/roller/abien/entry/reading_inputstream_into_string_with)
 */
@Throws(IOException::class)
fun InputStream.toString(encoding: String = DEFAULT_CHARSET): String =
    InputStreamReader(this, encoding).use {
        BufferedReader(it).use { buffer ->
            buffer.lines().collect(Collectors.joining("\n"))
        }
    }

@Throws(UnsupportedEncodingException::class)
fun String.urlEncode(encoding: String = DEFAULT_CHARSET): String? =
    URLEncoder.encode(this, encoding)

@Throws(IOException::class)
fun File.toInputStream(): InputStream =
    path.toInputStream()

@Throws(IOException::class)
fun URI.toInputStream(): InputStream =
    try {
        toURL().toInputStream()
    } catch (e: Exception) {
        toASCIIString().toInputStream()
    }

@Throws(IOException::class)
fun URL.toInputStream(): InputStream =
    toExternalForm().toInputStream()

/**
 * @return an [InputStream] for this `path` (absolute, relative to `user.dir`, or in `class path`)
 * @see System.getProperties
 */
@Throws(IOException::class)
fun CharSequence.toInputStream(cl: ClassLoader = Thread.currentThread().contextClassLoader): InputStream {
    val path = toString()
    val file = File(path)
    if (file.exists()) {
        LOG.trace("Found '{}' at location: {}", path, file.absolutePath)

        // if (path.exists() && path.isFile())
        return FileInputStream(file)
    }
    val userFile = File(System.getProperty("user.dir") + path)
    if (userFile.exists()) {
        LOG.trace("Found '{}' at location: {}", path, userFile.absolutePath)

        // if (path.exists() && path.isFile())
        return FileInputStream(userFile)
    }
    cl.getResource(path)?.let {
        LOG.trace("Found '{}' in java.class.path: {}", path, it)
        return cl.getResourceAsStream(path)!!
    }
    try {
        val url = URL(path)
        LOG.trace("Attempting download from $path")
        return url.openStream()
    } catch (e: MalformedURLException) {
        // ignore
    }
    throw FileNotFoundException("File not found: " + path + ", tried "
                + file.absolutePath + " and (context) java.class.path: "
                + System.getProperty("java.class.path", "<unknown>"))
}

@Throws(IOException::class)
fun String.toOutputStream(append: Boolean = true): OutputStream =
    File(this).toOutputStream(append)

@Throws(IOException::class)
fun File.toOutputStream(append: Boolean = true): OutputStream {
    if (createNewFile())
        LOG.trace("Creating '{}' at location: {}", name, absolutePath)
    else if (append)
        LOG.trace("Appending '{}' at location: {}", name, absolutePath)
    else
        LOG.trace("Overwriting '{}' at location: {}", name, absolutePath)

    return FileOutputStream(this, append)
}
