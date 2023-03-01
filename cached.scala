package cached

import java.nio.file._

import scala.io.Source

import org.apache.commons.codec.digest.DigestUtils

trait Cached[I](hash: I => String):
  val parent: Path = Paths.get("cache")

  def get(name: String, in: I): String =
    val file = cacheFile(name, in)
    if Files.exists(file) then
      Source.fromFile(file.toFile).mkString
    else
      val bytes = fetchUncached(in)
      Files.write(file, bytes)
      // TODO encoding?
      new String(bytes)

  def has(name: String, in: I): Boolean =
    Files.exists(cacheFile(name, in))

  private def cacheFile(name: String, in: I): Path =
    require(Files.isDirectory(parent), "directory cache/ should exist")
    parent.resolve(name + "-" + DigestUtils.sha256Hex(hash(in)))

  def fetchUncached(in: I): Array[Byte]
